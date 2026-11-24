package tech.veterinaria_api.graphql;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import tech.veterinaria_api.remoto.ClienteHttpFactory;

/**
 * Llamadas REST a los microservicios con el JWT del usuario (token relay). Devuelve el JSON como mapas y listas: el
 * gateway no duplica los DTO de cada servicio; GraphQL lee los campos del mapa por nombre.
 */
@Component
public class ApiInterna {

    public enum Servicio { AUTH, PACIENTES, CITAS, HISTORIAS }

    private final Map<Servicio, RestClient> clientes;

    public ApiInterna(ClienteHttpFactory factory,
            @Value("${app.servicios.auth-url}") String auth,
            @Value("${app.servicios.pacientes-url}") String pacientes,
            @Value("${app.servicios.citas-url}") String citas,
            @Value("${app.servicios.historias-url}") String historias) {
        this.clientes = Map.of(
                Servicio.AUTH, factory.crear(auth, "auth-service"),
                Servicio.PACIENTES, factory.crear(pacientes, "pacientes-service"),
                Servicio.CITAS, factory.crear(citas, "citas-service"),
                Servicio.HISTORIAS, factory.crear(historias, "historias-service"));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> objeto(Servicio servicio, String ruta, Object... variables) {
        return clientes.get(servicio).get().uri(ruta, variables).retrieve().body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> lista(Servicio servicio, java.util.function.Function<UriBuilder, java.net.URI> uri) {
        List<Map<String, Object>> resultado = clientes.get(servicio).get().uri(uri).retrieve().body(List.class);
        return resultado == null ? List.of() : resultado;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> objeto(Servicio servicio, java.util.function.Function<UriBuilder, java.net.URI> uri) {
        return clientes.get(servicio).get().uri(uri).retrieve().body(Map.class);
    }
}
