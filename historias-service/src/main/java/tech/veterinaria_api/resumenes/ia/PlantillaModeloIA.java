package tech.veterinaria_api.resumenes.ia;

import org.springframework.stereotype.Component;

import tech.veterinaria_api.resumenes.ContenidoResumen;

/**
 * Respaldo sin IA: arma el borrador con los mismos campos de la consulta. Se usa cuando no hay API key
 * configurada o el modelo falla, para que el veterinario siempre tenga un borrador que revisar.
 */
@Component
public class PlantillaModeloIA implements ModeloIA {

    public static final String MODELO = "plantilla-local";
    public static final String VERSION = "plantilla-v1";

    @Override
    public ResumenGenerado generar(DatosParaResumen d) {
        String nombre = d.nombreMascota() == null ? "tu mascota" : d.nombreMascota();
        String hallazgos = "En la consulta de " + nombre + " (" + minuscula(d.motivo()) + "), el veterinario encontró: "
                + valorO(d.diagnostico(), "sin hallazgos registrados") + ".";
        String tratamiento = valorO(d.tratamiento(), "El veterinario no indicó tratamiento con medicamentos.");
        String cuidados = valorO(d.indicaciones(),
                "Sigue las indicaciones que te dio el veterinario en la consulta.")
                + " Si notas que empeora o aparece algo nuevo, comunícate con la clínica.";
        String proxima = d.proximoControl() == null
                ? "Tu veterinario te indicará cuándo volver."
                : "Vuelve a control el " + d.proximoControlTexto() + ".";
        return new ResumenGenerado(new ContenidoResumen(hallazgos, tratamiento, cuidados, proxima), MODELO, VERSION);
    }

    private static String valorO(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }

    private static String minuscula(String valor) {
        if (valor == null || valor.isBlank()) {
            return "consulta general";
        }
        String v = valor.trim();
        return Character.toLowerCase(v.charAt(0)) + v.substring(1);
    }
}
