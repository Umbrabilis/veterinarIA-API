package tech.veterinaria_api.remoto;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.common.ServicioRemotoException;

/**
 * Crea {@link RestClient} para llamar a otros microservicios. Reenvía el JWT de la petición en curso
 * (token relay): el servicio destino aplica sus propias reglas de pertenencia con la identidad real del
 * usuario, en lugar de confiar en el servicio que llama. Los 404/403 remotos se traducen a las
 * excepciones de negocio equivalentes; cualquier otro error, a {@link ServicioRemotoException}.
 */
@Component
public class ClienteHttpFactory {

    private final ObjectProvider<RestClient.Builder> builders;

    public ClienteHttpFactory(ObjectProvider<RestClient.Builder> builders) {
        this.builders = builders;
    }

    public RestClient crear(String baseUrl, String nombreServicio) {
        return builders.getObject()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    tokenActual().ifPresent(token -> request.getHeaders().setBearerAuth(token));
                    try {
                        return execution.execute(request, body);
                    } catch (java.io.IOException e) {
                        throw new ServicioRemotoException(nombreServicio + " no responde", e);
                    }
                })
                .defaultStatusHandler(status -> status.isError(), (request, response) -> {
                    HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
                    if (status == HttpStatus.NOT_FOUND) {
                        throw new RecursoNoEncontradoException("El recurso solicitado no existe");
                    }
                    if (status == HttpStatus.FORBIDDEN) {
                        throw new AccesoDenegadoException();
                    }
                    throw new ServicioRemotoException(
                            nombreServicio + " respondió " + response.getStatusCode().value());
                })
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    private static java.util.Optional<String> tokenActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return java.util.Optional.of(jwtAuth.getToken().getTokenValue());
        }
        return java.util.Optional.empty();
    }
}
