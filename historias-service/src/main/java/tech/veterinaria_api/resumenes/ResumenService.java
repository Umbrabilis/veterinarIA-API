package tech.veterinaria_api.resumenes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.ConflictoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.common.ReglaNegocioException;
import tech.veterinaria_api.consultas.Consulta;
import tech.veterinaria_api.consultas.ConsultaService;
import tech.veterinaria_api.remoto.PacientesClient;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;
import tech.veterinaria_api.remoto.PacientesClient.PropietarioRemoto;
import tech.veterinaria_api.resumenes.ia.DatosParaResumen;
import tech.veterinaria_api.resumenes.ia.GeneradorResumen;
import tech.veterinaria_api.resumenes.ia.ResumenGenerado;
import tech.veterinaria_api.security.UsuarioActual;

/**
 * Flujo del resumen para el dueño: la IA redacta un BORRADOR, el veterinario de la consulta lo edita y lo
 * APRUEBA, y solo entonces se puede ENVIAR. La base de datos vuelve a validar cada transición.
 */
@Service
public class ResumenService {

    private final ResumenConsultaRepository resumenRepository;
    private final ConsultaService consultaService;
    private final PacientesClient pacientesClient;
    private final GeneradorResumen generador;
    private final NotificadorResumen notificador;
    private final TransactionTemplate transaccion;
    private final ZoneId zona;

    private static final String MENSAJE_SIN_APROBACION =
            "El resumen debe estar aprobado por el veterinario antes de enviarse";

    public ResumenService(ResumenConsultaRepository resumenRepository, ConsultaService consultaService,
            PacientesClient pacientesClient, GeneradorResumen generador, NotificadorResumen notificador,
            PlatformTransactionManager transactionManager, @Value("${app.zona-horaria:America/Bogota}") String zona) {
        this.resumenRepository = resumenRepository;
        this.consultaService = consultaService;
        this.pacientesClient = pacientesClient;
        this.generador = generador;
        this.notificador = notificador;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.zona = ZoneId.of(zona);
    }

    /**
     * Genera el borrador de una consulta cerrada. Las llamadas a pacientes-service y al modelo ocurren fuera de
     * transacción; si dos peticiones compiten, la restricción UNIQUE deja solo un resumen por consulta.
     */
    public ResumenConsulta generarBorrador(UUID consultaId) {
        Consulta consulta = consultaService.buscar(consultaId);
        if (!consulta.estaCerrada()) {
            throw new ReglaNegocioException("El resumen se genera cuando la consulta está cerrada");
        }
        if (resumenRepository.existsByConsultaId(consultaId)) {
            throw new ConflictoException("La consulta ya tiene un resumen");
        }
        MascotaRemota mascota = pacientesClient.obtenerMascota(consulta.getMascotaId());
        ResumenGenerado generado = generador.generar(DatosParaResumen.de(consulta, mascota, LocalDate.now(zona)));
        try {
            return resumenRepository.saveAndFlush(ResumenConsulta.borrador(consulta.getId(),
                    consulta.getVeterinarioId(), generado.modelo(), generado.versionPrompt(), generado.contenido()));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("La consulta ya tiene un resumen");
        }
    }

    public ResumenConsulta generarBorradorPara(UUID consultaId, UsuarioActual usuario) {
        Consulta consulta = consultaService.buscar(consultaId);
        exigirVeterinarioDe(consulta.getVeterinarioId(), usuario);
        return generarBorrador(consultaId);
    }

    @Transactional
    public ResumenConsulta editar(UUID id, ContenidoResumen contenido, UsuarioActual usuario) {
        ResumenConsulta resumen = buscar(id);
        exigirVeterinarioDe(resumen.getVeterinarioId(), usuario);
        exigirEstado(resumen, EstadoResumen.BORRADOR, "Solo se edita un resumen en borrador");
        resumen.editar(contenido);
        return resumen;
    }

    @Transactional
    public ResumenConsulta aprobar(UUID id, ContenidoResumen contenidoFinal, UsuarioActual usuario) {
        ResumenConsulta resumen = buscar(id);
        exigirVeterinarioDe(resumen.getVeterinarioId(), usuario);
        exigirEstado(resumen, EstadoResumen.BORRADOR, "El resumen ya fue aprobado");
        if (contenidoFinal != null) {
            resumen.editar(contenidoFinal);
        }
        resumen.aprobar(usuario.id(), Instant.now());
        return resumen;
    }

    /**
     * Único camino hacia el dueño. Exige APROBADO antes de tocar el notificador. El destinatario es el dueño
     * actual de la mascota. El envío ocurre con la fila bloqueada: dos peticiones simultáneas no envían dos
     * correos (la segunda espera y encuentra ENVIADO). Si el correo falla, la transacción se revierte y el
     * resumen sigue APROBADO para reintentar.
     */
    public ResumenConsulta enviar(UUID id, UsuarioActual usuario) {
        ResumenConsulta previo = buscar(id);
        exigirVeterinarioDe(previo.getVeterinarioId(), usuario);
        exigirEstado(previo, EstadoResumen.APROBADO, MENSAJE_SIN_APROBACION);

        Consulta consulta = consultaService.buscar(previo.getConsultaId());
        MascotaRemota mascota = pacientesClient.obtenerMascota(consulta.getMascotaId());
        PropietarioRemoto propietario = pacientesClient.obtenerPropietario(mascota.propietarioId());
        if (propietario.email() == null || propietario.email().isBlank()) {
            throw new ReglaNegocioException("El propietario no tiene email registrado");
        }

        return transaccion.execute(estado -> {
            ResumenConsulta resumen = resumenRepository.bloquear(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Resumen no encontrado"));
            exigirEstado(resumen, EstadoResumen.APROBADO, MENSAJE_SIN_APROBACION);
            notificador.enviar(propietario.email(), propietario.nombre(), mascota.nombre(), resumen.contenidoFinal());
            resumen.marcarEnviado(Instant.now());
            return resumen;
        });
    }

    /** Personal de la clínica: siempre. Propietario: solo si ya se le envió y la consulta es de su mascota. */
    public ResumenConsulta obtener(UUID id, UsuarioActual usuario) {
        ResumenConsulta resumen = buscar(id);
        verificarLectura(resumen, usuario);
        return resumen;
    }

    public ResumenConsulta porConsulta(UUID consultaId, UsuarioActual usuario) {
        ResumenConsulta resumen = resumenRepository.findByConsultaId(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La consulta aún no tiene resumen"));
        verificarLectura(resumen, usuario);
        return resumen;
    }

    /** Bandeja de revisión: el veterinario ve los de sus consultas; el administrador, todos. */
    @Transactional(readOnly = true)
    public List<ResumenConsulta> porEstado(EstadoResumen estado, UsuarioActual usuario) {
        if (usuario.esVeterinario()) {
            return resumenRepository.findByVeterinarioIdAndEstadoOrderByCreatedAtDesc(usuario.id(), estado);
        }
        if (usuario.esAdministrador()) {
            return resumenRepository.findByEstadoOrderByCreatedAtDesc(estado);
        }
        throw new AccesoDenegadoException();
    }

    private void verificarLectura(ResumenConsulta resumen, UsuarioActual usuario) {
        if (usuario.esPersonalClinico()) {
            return;
        }
        if (!usuario.esPropietario() || resumen.getEstado() != EstadoResumen.ENVIADO) {
            throw new AccesoDenegadoException();
        }
        // Pertenencia actual de la mascota (403 si ya no es suya).
        pacientesClient.obtenerMascota(consultaService.buscar(resumen.getConsultaId()).getMascotaId());
    }

    private ResumenConsulta buscar(UUID id) {
        return resumenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Resumen no encontrado"));
    }

    private static void exigirVeterinarioDe(UUID veterinarioConsulta, UsuarioActual usuario) {
        if (!usuario.esVeterinario() || !veterinarioConsulta.equals(usuario.id())) {
            throw new AccesoDenegadoException("Solo el veterinario que atendió la consulta revisa su resumen");
        }
    }

    private static void exigirEstado(ResumenConsulta resumen, EstadoResumen esperado, String mensaje) {
        if (resumen.getEstado() != esperado) {
            throw new ReglaNegocioException(mensaje);
        }
    }
}
