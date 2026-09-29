package tech.veterinaria_api.resumenes.ia;

import tech.veterinaria_api.resumenes.ContenidoResumen;

/** Texto generado y su trazabilidad (qué modelo y qué versión del prompt lo produjeron). */
public record ResumenGenerado(ContenidoResumen contenido, String modelo, String versionPrompt) {
}
