package tech.veterinaria_api.resumenes;

/**
 * Entrega al dueño un resumen YA APROBADO. Solo {@link ResumenService#enviar} lo invoca, después de verificar
 * el estado APROBADO; no hay otro camino hacia el dueño.
 */
public interface NotificadorResumen {

    void enviar(String emailDestino, String nombreDestinatario, String nombreMascota, ContenidoResumen contenido);
}
