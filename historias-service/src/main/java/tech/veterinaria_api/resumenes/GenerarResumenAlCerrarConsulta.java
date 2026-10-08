package tech.veterinaria_api.resumenes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tech.veterinaria_api.consultas.ConsultaCerradaEvent;

/**
 * Al confirmarse el cierre de una consulta se genera el borrador del resumen. Si falla (p. ej. pacientes-service
 * no responde) el cierre ya quedó guardado y el veterinario puede pedirlo con POST /consultas/{id}/resumen.
 */
@Component
public class GenerarResumenAlCerrarConsulta {

    private static final Logger log = LoggerFactory.getLogger(GenerarResumenAlCerrarConsulta.class);

    private final ResumenService resumenService;

    public GenerarResumenAlCerrarConsulta(ResumenService resumenService) {
        this.resumenService = resumenService;
    }

    // NOT_SUPPORTED: se desvincula de la transacción ya confirmada del cierre; cada escritura abre la suya y la
    // llamada al modelo no retiene una conexión a la base de datos.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void alCerrarConsulta(ConsultaCerradaEvent evento) {
        try {
            resumenService.generarBorrador(evento.consultaId());
        } catch (RuntimeException e) {
            log.warn("No se generó el borrador del resumen de la consulta {} ({})", evento.consultaId(),
                    e.getClass().getSimpleName());
        }
    }
}
