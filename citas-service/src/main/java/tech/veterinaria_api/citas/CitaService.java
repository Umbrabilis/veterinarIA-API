package tech.veterinaria_api.citas;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.citas.dto.CrearCitaRequest;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.ConflictoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.common.ReglaNegocioException;
import tech.veterinaria_api.remoto.PacientesClient;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;
import tech.veterinaria_api.remoto.UsuariosClient;
import tech.veterinaria_api.remoto.UsuariosClient.VeterinarioRemoto;
import tech.veterinaria_api.security.UsuarioActual;

/**
 * Las llamadas a otros servicios se hacen fuera de transacciones de base de datos, para no retener
 * conexiones mientras se espera la red. El no solapamiento lo garantiza PostgreSQL (EXCLUDE USING gist).
 */
@Service
@RequiredArgsConstructor
public class CitaService {

    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Bogota");
    private static final Duration DURACION_MINIMA = Duration.ofMinutes(10);
    private static final Duration DURACION_MAXIMA = Duration.ofHours(4);

    private final CitaRepository citaRepository;
    private final PacientesClient pacientesClient;
    private final UsuariosClient usuariosClient;

    public Cita crear(CrearCitaRequest request, UsuarioActual usuario) {
        if (!usuario.esPersonalClinico() && !usuario.esPropietario()) {
            throw new AccesoDenegadoException();
        }
        validarHorario(request.inicio(), request.fin());
        // pacientes-service responde 403 si un propietario pide una mascota que no es suya.
        MascotaRemota mascota = pacientesClient.obtenerMascota(request.mascotaId());
        if (!mascota.activo()) {
            throw new ReglaNegocioException("La mascota está inactiva");
        }
        VeterinarioRemoto veterinario = usuariosClient.obtenerVeterinario(request.veterinarioId());

        Cita cita = Cita.builder()
                .id(UUID.randomUUID())
                .mascotaId(mascota.id())
                .propietarioId(mascota.propietarioId())
                .mascotaNombre(mascota.nombre())
                .veterinarioId(veterinario.id())
                .veterinarioNombre(veterinario.nombre())
                .inicio(request.inicio())
                .fin(request.fin())
                .motivo(request.motivo().trim())
                .estado(EstadoCita.PROGRAMADA)
                .creadaPor(usuario.id())
                .build();
        return guardar(cita);
    }

    public Cita obtener(UUID id, UsuarioActual usuario) {
        Cita cita = buscar(id);
        verificarAcceso(cita, usuario);
        return cita;
    }

    @Transactional(readOnly = true)
    public List<Cita> listar(UUID veterinarioId, UUID mascotaId, Instant desde, Instant hasta) {
        return citaRepository.findAll(CitaRepository.filtros(veterinarioId, mascotaId, desde, hasta),
                Sort.by("inicio"));
    }

    /** Propietario: sus citas. Veterinario: su agenda desde hoy. */
    public List<Cita> listarMias(UsuarioActual usuario) {
        if (usuario.esPropietario()) {
            return pacientesClient.miPropietarioId()
                    .map(citaRepository::findByPropietarioIdOrderByInicioDesc)
                    .orElse(List.of());
        }
        if (usuario.esVeterinario()) {
            Instant inicioDelDia = LocalDate.now(ZONA_CLINICA).atStartOfDay(ZONA_CLINICA).toInstant();
            return citaRepository.findByVeterinarioIdAndInicioGreaterThanEqualOrderByInicioAsc(usuario.id(),
                    inicioDelDia);
        }
        throw new AccesoDenegadoException("Usa el listado general de citas");
    }

    public Cita cancelar(UUID id, UsuarioActual usuario) {
        Cita cita = buscar(id);
        verificarAcceso(cita, usuario);
        if (!cita.getEstado().puedePasarA(EstadoCita.CANCELADA)) {
            throw new ReglaNegocioException("Solo se pueden cancelar citas programadas o confirmadas");
        }
        if (usuario.esPropietario() && cita.getInicio().isBefore(Instant.now())) {
            throw new ReglaNegocioException("La cita ya pasó; comunícate con la clínica");
        }
        cita.setEstado(EstadoCita.CANCELADA);
        return guardar(cita);
    }

    public Cita reprogramar(UUID id, Instant inicio, Instant fin, UsuarioActual usuario) {
        Cita cita = buscar(id);
        verificarAcceso(cita, usuario);
        if (!cita.getEstado().esActiva()) {
            throw new ReglaNegocioException("Solo se pueden reprogramar citas programadas o confirmadas");
        }
        validarHorario(inicio, fin);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setEstado(EstadoCita.PROGRAMADA);
        return guardar(cita);
    }

    public Cita cambiarEstado(UUID id, EstadoCita nuevo, UsuarioActual usuario) {
        if (!usuario.esPersonalClinico()) {
            throw new AccesoDenegadoException();
        }
        Cita cita = buscar(id);
        if (!cita.getEstado().puedePasarA(nuevo)) {
            throw new ReglaNegocioException(
                    "No se puede pasar una cita de " + cita.getEstado() + " a " + nuevo);
        }
        cita.setEstado(nuevo);
        return guardar(cita);
    }

    /**
     * El propietario accede si la mascota es suya HOY: se pregunta a pacientes-service (403 si no), en lugar de
     * confiar en el propietario copiado al agendar, que queda desactualizado si la mascota cambia de dueño.
     */
    private void verificarAcceso(Cita cita, UsuarioActual usuario) {
        if (usuario.esPersonalClinico()) {
            return;
        }
        if (!usuario.esPropietario()) {
            throw new AccesoDenegadoException();
        }
        pacientesClient.obtenerMascota(cita.getMascotaId());
    }

    private Cita guardar(Cita cita) {
        try {
            return citaRepository.saveAndFlush(cita);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("El horario se cruza con otra cita del veterinario o de la mascota");
        }
    }

    private Cita buscar(UUID id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));
    }

    private static void validarHorario(Instant inicio, Instant fin) {
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio");
        }
        Duration duracion = Duration.between(inicio, fin);
        if (duracion.compareTo(DURACION_MINIMA) < 0 || duracion.compareTo(DURACION_MAXIMA) > 0) {
            throw new ReglaNegocioException("La cita debe durar entre 10 minutos y 4 horas");
        }
    }
}
