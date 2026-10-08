package tech.veterinaria_api.resumenes;

/** BORRADOR -> APROBADO (por el veterinario) -> ENVIADO (al dueño). Sin saltos: lo exige la base de datos. */
public enum EstadoResumen {
    BORRADOR,
    APROBADO,
    ENVIADO
}
