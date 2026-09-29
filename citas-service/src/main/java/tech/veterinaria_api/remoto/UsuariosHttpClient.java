package tech.veterinaria_api.remoto;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tech.veterinaria_api.common.RecursoNoEncontradoException;

@Component
public class UsuariosHttpClient implements UsuariosClient {

    private final RestClient restClient;

    public UsuariosHttpClient(ClienteHttpFactory factory, @Value("${app.servicios.auth-url}") String baseUrl) {
        this.restClient = factory.crear(baseUrl, "auth-service");
    }

    @Override
    public VeterinarioRemoto obtenerVeterinario(UUID veterinarioId) {
        try {
            return restClient.get().uri("/api/v1/usuarios/veterinarios/{id}", veterinarioId).retrieve()
                    .body(VeterinarioRemoto.class);
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Veterinario no encontrado");
        }
    }
}
