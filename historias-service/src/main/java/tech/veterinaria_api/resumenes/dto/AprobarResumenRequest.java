package tech.veterinaria_api.resumenes.dto;

import jakarta.validation.Valid;
import tech.veterinaria_api.resumenes.ContenidoResumen;

/**
 * {@code contenidoFinal} opcional: si viene, es la versión que aprueba el veterinario; si no, se aprueba la
 * última edición guardada o, sin ediciones, el texto generado tal cual.
 */
public record AprobarResumenRequest(@Valid ContenidoResumen contenidoFinal) {
}
