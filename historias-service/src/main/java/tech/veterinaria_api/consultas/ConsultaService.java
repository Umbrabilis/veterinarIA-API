package tech.veterinaria_api.consultas;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.common.ReglaNegocioException;
import tech.veterinaria_api.consultas.dto.DatosClinicosRequest;
import tech.veterinaria_api.consultas.dto.EnmiendaRequest;
import tech.veterinaria_api.notificaciones.EventoNotificacion;
import tech.veterinaria_api.notificaciones.PublicadorNotificaciones;
import tech.veterinaria_api.notificaciones.TipoNotificacion;
import tech.veterinaria_api.remoto.PacientesClient;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;
import tech.veterinaria_api.security.UsuarioActual;

/**
 * Historia clínica. Una consulta abierta la edita solo el veterinario que la creó; al cerrarla queda inmutable
 * (lo refuerza un trigger en PostgreSQL) y cualquier corrección posterior es una enmienda.
 */
@Service
@RequiredArgsConstructor
public class ConsultaService {

    private static final Logger log = LoggerFactory.getLogger(ConsultaService.class);

    private final ConsultaRepository consultaRepository;
    private final EnmiendaRepository enmiendaRepository;
    private final PacientesClient pacientesClient;
    private final ApplicationEventPublisher eventos;
    private final PublicadorNotificaciones avisos;

    public Consulta crear(UUID mascotaId, UUID citaId, DatosClinicosRequest datos, UsuarioActual usuario) {
        exigirVeterinario(usuario);
        MascotaRemota mascota = pacientesClient.obtenerMascota(mascotaId);
        Consulta consulta = Consulta.builder()
                .id(UUID.randomUUID())
                .mascotaId(mascota.id())
                .propietarioId(mascota.propietarioId())
                .veterinarioId(usuario.id())
                .citaId(citaId)
                .estado(EstadoConsulta.ABIERTA)
                .build();
        aplicar(consulta, datos);
        Consulta guardada = consultaRepository.save(consulta);
        avisos.publicar(EventoNotificacion.paraTodos(TipoNotificacion.CONSULTA_ABIERTA, "Consulta en curso",
                mascota.nombre() + " está en consulta", guardada.getId()));
        return guardada;
    }

    @Transactional
    public Consulta actualizar(UUID id, DatosClinicosRequest datos, UsuarioActual usuario) {
        Consulta consulta = editablePor(id, usuario);
        aplicar(consulta, datos);
        return consulta;
    }

    /** Cierra la consulta y publica {@link ConsultaCerradaEvent} (dispara el borrador del resumen IA). */
    @Transactional
    public Consulta cerrar(UUID id, UsuarioActual usuario) {
        Consulta consulta = editablePor(id, usuario);
        if (consulta.getDiagnostico() == null || consulta.getDiagnostico().isBlank()) {
            throw new ReglaNegocioException("Registra el diagnóstico antes de cerrar la consulta");
        }
        consulta.setEstado(EstadoConsulta.CERRADA);
        consulta.setCerradaEn(Instant.now());
        consultaRepository.flush();
        eventos.publishEvent(new ConsultaCerradaEvent(consulta.getId()));
        return consulta;
    }

    /**
     * Sin transacción: es una sola inserción (una consulta cerrada no vuelve a abrirse) y el aviso necesita el
     * nombre de la mascota, que se pide a pacientes-service sin retener una conexión de la base de datos.
     */
    public Enmienda enmendar(UUID id, EnmiendaRequest request, UsuarioActual usuario) {
        exigirVeterinario(usuario);
        Consulta consulta = buscar(id);
        if (!consulta.estaCerrada()) {
            throw new ReglaNegocioException("La consulta está abierta: edítala directamente");
        }
        Enmienda enmienda = enmiendaRepository.save(Enmienda.builder()
                .id(UUID.randomUUID())
                .consultaId(consulta.getId())
                .autorId(usuario.id())
                .motivo(request.motivo().trim())
                .contenido(request.contenido().trim())
                .build());
        avisos.publicar(EventoNotificacion.paraTodos(TipoNotificacion.CONSULTA_ENMENDADA,
                "Historia clínica enmendada",
                "Se agregó una enmienda a una consulta de " + nombreMascota(consulta.getMascotaId()),
                consulta.getId()));
        return enmienda;
    }

    /** Para avisos: si pacientes-service no responde, la enmienda ya quedó guardada y no debe fallar por esto. */
    private String nombreMascota(UUID mascotaId) {
        try {
            return pacientesClient.obtenerMascota(mascotaId).nombre();
        } catch (RuntimeException e) {
            log.warn("No se obtuvo el nombre de la mascota para el aviso ({})", e.getClass().getSimpleName());
            return "una mascota";
        }
    }

    /** Personal de la clínica: cualquier consulta. Propietario: solo consultas cerradas de sus mascotas. */
    public Consulta obtener(UUID id, UsuarioActual usuario) {
        Consulta consulta = buscar(id);
        if (usuario.esPersonalClinico()) {
            return consulta;
        }
        if (!usuario.esPropietario() || !consulta.estaCerrada()) {
            throw new AccesoDenegadoException();
        }
        // Pertenencia actual de la mascota (403 si ya no es suya), no el propietario copiado en la consulta.
        pacientesClient.obtenerMascota(consulta.getMascotaId());
        return consulta;
    }

    public List<Enmienda> enmiendas(UUID consultaId) {
        return enmiendaRepository.findByConsultaIdOrderByCreatedAtAsc(consultaId);
    }

    /** Historia clínica de una mascota; el propietario ve solo las consultas cerradas. */
    public List<Consulta> historiaDeMascota(UUID mascotaId, UsuarioActual usuario) {
        if (usuario.esPersonalClinico()) {
            return consultaRepository.findByMascotaIdOrderByCreatedAtDesc(mascotaId);
        }
        if (!usuario.esPropietario()) {
            throw new AccesoDenegadoException();
        }
        pacientesClient.obtenerMascota(mascotaId); // 403 si la mascota no es suya
        return consultaRepository.findByMascotaIdAndEstadoOrderByCreatedAtDesc(mascotaId, EstadoConsulta.CERRADA);
    }

    public Consulta buscar(UUID id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta no encontrada"));
    }

    private Consulta editablePor(UUID id, UsuarioActual usuario) {
        exigirVeterinario(usuario);
        Consulta consulta = buscar(id);
        if (!consulta.getVeterinarioId().equals(usuario.id())) {
            throw new AccesoDenegadoException("Solo el veterinario que atiende la consulta puede modificarla");
        }
        if (consulta.estaCerrada()) {
            throw new ReglaNegocioException("La consulta está cerrada: no se edita, se enmienda");
        }
        return consulta;
    }

    private static void exigirVeterinario(UsuarioActual usuario) {
        if (!usuario.esVeterinario()) {
            throw new AccesoDenegadoException("Solo un veterinario puede registrar historia clínica");
        }
    }

    private static void aplicar(Consulta consulta, DatosClinicosRequest datos) {
        consulta.setMotivo(datos.motivo().trim());
        consulta.setSintomas(limpiar(datos.sintomas()));
        consulta.setExamenFisico(limpiar(datos.examenFisico()));
        consulta.setPesoKg(datos.pesoKg());
        consulta.setTemperaturaC(datos.temperaturaC());
        consulta.setDiagnostico(limpiar(datos.diagnostico()));
        consulta.setTratamiento(limpiar(datos.tratamiento()));
        consulta.setIndicaciones(limpiar(datos.indicaciones()));
        consulta.setProximoControl(datos.proximoControl());
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
