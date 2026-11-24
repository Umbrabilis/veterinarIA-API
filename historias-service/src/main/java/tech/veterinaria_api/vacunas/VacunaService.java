package tech.veterinaria_api.vacunas;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.ReglaNegocioException;
import tech.veterinaria_api.notificaciones.EventoNotificacion;
import tech.veterinaria_api.notificaciones.PublicadorNotificaciones;
import tech.veterinaria_api.notificaciones.TipoNotificacion;
import tech.veterinaria_api.remoto.PacientesClient;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;
import tech.veterinaria_api.security.UsuarioActual;
import tech.veterinaria_api.vacunas.dto.VacunaRequest;

@Service
@RequiredArgsConstructor
public class VacunaService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VacunaRepository vacunaRepository;
    private final PacientesClient pacientesClient;
    private final PublicadorNotificaciones avisos;

    public Vacuna registrar(VacunaRequest request, UsuarioActual usuario) {
        if (!usuario.esVeterinario()) {
            throw new AccesoDenegadoException("Solo un veterinario registra vacunas aplicadas");
        }
        if (request.proximaDosis() != null && !request.proximaDosis().isAfter(request.fechaAplicacion())) {
            throw new ReglaNegocioException("La próxima dosis debe ser posterior a la fecha de aplicación");
        }
        MascotaRemota mascota = pacientesClient.obtenerMascota(request.mascotaId());
        Vacuna vacuna = vacunaRepository.save(Vacuna.builder()
                .id(UUID.randomUUID())
                .mascotaId(mascota.id())
                .propietarioId(mascota.propietarioId())
                .veterinarioId(usuario.id())
                .nombre(request.nombre().trim())
                .lote(request.lote() == null || request.lote().isBlank() ? null : request.lote().trim())
                .fechaAplicacion(request.fechaAplicacion())
                .proximaDosis(request.proximaDosis())
                .notas(request.notas() == null || request.notas().isBlank() ? null : request.notas().trim())
                .build());
        String proximaDosis = vacuna.getProximaDosis() == null ? ""
                : "; próxima dosis el " + FORMATO_FECHA.format(vacuna.getProximaDosis());
        avisos.publicar(EventoNotificacion.paraTodos(TipoNotificacion.VACUNA_REGISTRADA, "Vacuna aplicada",
                vacuna.getNombre() + " a " + mascota.nombre() + proximaDosis, vacuna.getId()));
        return vacuna;
    }

    /** Carné de vacunación: personal de la clínica, o el propietario de la mascota. */
    public List<Vacuna> carne(UUID mascotaId, UsuarioActual usuario) {
        if (!usuario.esPersonalClinico()) {
            if (!usuario.esPropietario()) {
                throw new AccesoDenegadoException();
            }
            pacientesClient.obtenerMascota(mascotaId); // 403 si la mascota no es suya
        }
        return vacunaRepository.findByMascotaIdOrderByFechaAplicacionDesc(mascotaId);
    }
}
