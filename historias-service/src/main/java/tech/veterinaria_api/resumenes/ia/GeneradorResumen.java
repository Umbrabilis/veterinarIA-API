package tech.veterinaria_api.resumenes.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Elige el modelo: Claude si está configurado; si no, o si falla, la plantilla local. En ambos casos el
 * resultado es solo un borrador que el veterinario debe aprobar.
 */
@Component
public class GeneradorResumen {

    private static final Logger log = LoggerFactory.getLogger(GeneradorResumen.class);

    private final ObjectProvider<AnthropicModeloIA> modeloIA;
    private final PlantillaModeloIA plantilla;

    public GeneradorResumen(ObjectProvider<AnthropicModeloIA> modeloIA, PlantillaModeloIA plantilla) {
        this.modeloIA = modeloIA;
        this.plantilla = plantilla;
    }

    public ResumenGenerado generar(DatosParaResumen datos) {
        AnthropicModeloIA ia = modeloIA.getIfAvailable();
        if (ia != null) {
            try {
                return ia.generar(datos);
            } catch (RuntimeException e) {
                // Solo el tipo de error: el mensaje podría arrastrar contenido clínico.
                log.warn("Falló la generación con IA ({}); se usa la plantilla local", e.getClass().getSimpleName());
            }
        }
        return plantilla.generar(datos);
    }
}
