package tech.veterinaria_api.remoto;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tech.veterinaria_api.common.RecursoNoEncontradoException;

@Component
public class PacientesHttpClient implements PacientesClient {

    private final RestClient restClient;

    public PacientesHttpClient(ClienteHttpFactory factory, @Value("${app.servicios.pacientes-url}") String baseUrl) {
        this.restClient = factory.crear(baseUrl, "pacientes-service");
    }

    @Override
    public MascotaRemota obtenerMascota(UUID mascotaId) {
        try {
            return restClient.get().uri("/api/v1/mascotas/{id}", mascotaId).retrieve().body(MascotaRemota.class);
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Mascota no encontrada");
        }
    }

    @Override
    public PropietarioRemoto obtenerPropietario(UUID propietarioId) {
        try {
            return restClient.get().uri("/api/v1/propietarios/{id}", propietarioId).retrieve()
                    .body(PropietarioRemoto.class);
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Propietario no encontrado");
        }
    }
}
