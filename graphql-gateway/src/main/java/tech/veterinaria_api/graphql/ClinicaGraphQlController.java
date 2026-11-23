package tech.veterinaria_api.graphql;

import static tech.veterinaria_api.graphql.ApiInterna.Servicio.AUTH;
import static tech.veterinaria_api.graphql.ApiInterna.Servicio.CITAS;
import static tech.veterinaria_api.graphql.ApiInterna.Servicio.HISTORIAS;
import static tech.veterinaria_api.graphql.ApiInterna.Servicio.PACIENTES;

import java.util.List;
import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.RecursoNoEncontradoException;

/**
 * Resolvers GraphQL. Las consultas raíz y las relaciones (mascota → propietario, citas, consultas, vacunas) se
 * resuelven pidiendo cada dato al microservicio que lo posee; si el usuario no tiene acceso, ese servicio responde
 * 403 y GraphQL devuelve el error en {@code errors}.
 */
@Controller
@RequiredArgsConstructor
public class ClinicaGraphQlController {

    private final ApiInterna api;

    // --- Consultas raíz ---

    @QueryMapping
    public Map<String, Object> yo() {
        return api.objeto(AUTH, "/api/v1/auth/me");
    }

    @QueryMapping
    public List<Map<String, Object>> veterinarios() {
        return api.lista(AUTH, uri -> uri.path("/api/v1/usuarios/veterinarios").build());
    }

    @QueryMapping
    public Map<String, Object> mascota(@Argument String id) {
        return api.objeto(PACIENTES, "/api/v1/mascotas/{id}", id);
    }

    @QueryMapping
    public Map<String, Object> mascotas(@Argument String busqueda, @Argument String propietarioId,
            @Argument int pagina, @Argument int tamano) {
        return api.objeto(PACIENTES, uri -> uri.path("/api/v1/mascotas")
                .queryParamIfPresent("busqueda", java.util.Optional.ofNullable(busqueda))
                .queryParamIfPresent("propietarioId", java.util.Optional.ofNullable(propietarioId))
                .queryParam("pagina", pagina).queryParam("tamano", tamano).build());
    }

    @QueryMapping
    public Map<String, Object> propietario(@Argument String id) {
        return api.objeto(PACIENTES, "/api/v1/propietarios/{id}", id);
    }

    @QueryMapping
    public Map<String, Object> propietarios(@Argument String busqueda, @Argument int pagina, @Argument int tamano) {
        return api.objeto(PACIENTES, uri -> uri.path("/api/v1/propietarios")
                .queryParamIfPresent("busqueda", java.util.Optional.ofNullable(busqueda))
                .queryParam("pagina", pagina).queryParam("tamano", tamano).build());
    }

    @QueryMapping
    public List<Map<String, Object>> agenda(@Argument String desde, @Argument String hasta,
            @Argument String veterinarioId) {
        return api.lista(CITAS, uri -> uri.path("/api/v1/citas")
                .queryParamIfPresent("desde", java.util.Optional.ofNullable(desde))
                .queryParamIfPresent("hasta", java.util.Optional.ofNullable(hasta))
                .queryParamIfPresent("veterinarioId", java.util.Optional.ofNullable(veterinarioId)).build());
    }

    @QueryMapping
    public List<Map<String, Object>> resumenes(@Argument String estado) {
        return api.lista(HISTORIAS, uri -> uri.path("/api/v1/resumenes").queryParam("estado", estado).build());
    }

    @QueryMapping
    public List<Map<String, Object>> seguimientoPendiente(@Argument int dias) {
        return api.lista(HISTORIAS, uri -> uri.path("/api/v1/seguimiento/pendientes").queryParam("dias", dias).build());
    }

    // --- Relaciones ---

    @SchemaMapping(typeName = "Mascota")
    public Map<String, Object> propietario(Map<String, Object> mascota) {
        return api.objeto(PACIENTES, "/api/v1/propietarios/{id}", mascota.get("propietarioId"));
    }

    @SchemaMapping(typeName = "Mascota")
    public List<Map<String, Object>> citas(Map<String, Object> mascota) {
        return api.lista(CITAS, uri -> uri.path("/api/v1/citas").queryParam("mascotaId", mascota.get("id")).build());
    }

    @SchemaMapping(typeName = "Mascota")
    public List<Map<String, Object>> consultas(Map<String, Object> mascota) {
        return api.lista(HISTORIAS, uri -> uri.path("/api/v1/consultas").queryParam("mascotaId", mascota.get("id")).build());
    }

    @SchemaMapping(typeName = "Mascota")
    public List<Map<String, Object>> vacunas(Map<String, Object> mascota) {
        return api.lista(HISTORIAS, uri -> uri.path("/api/v1/vacunas").queryParam("mascotaId", mascota.get("id")).build());
    }

    @SuppressWarnings("unchecked")
    @SchemaMapping(typeName = "Propietario")
    public List<Map<String, Object>> mascotas(Map<String, Object> propietario) {
        Map<String, Object> pagina = api.objeto(PACIENTES, uri -> uri.path("/api/v1/mascotas")
                .queryParam("propietarioId", propietario.get("id")).queryParam("tamano", 100).build());
        return pagina == null ? List.of() : (List<Map<String, Object>>) pagina.getOrDefault("contenido", List.of());
    }

    @SchemaMapping(typeName = "Cita", field = "mascota")
    public Map<String, Object> mascotaDeCita(Map<String, Object> cita) {
        return api.objeto(PACIENTES, "/api/v1/mascotas/{id}", cita.get("mascotaId"));
    }

    @SchemaMapping(typeName = "Seguimiento", field = "mascota")
    public Map<String, Object> mascotaDeSeguimiento(Map<String, Object> seguimiento) {
        return api.objeto(PACIENTES, "/api/v1/mascotas/{id}", seguimiento.get("mascotaId"));
    }

    @SchemaMapping(typeName = "Consulta")
    public Map<String, Object> resumen(Map<String, Object> consulta) {
        try {
            return api.objeto(HISTORIAS, "/api/v1/consultas/{id}/resumen", consulta.get("id"));
        } catch (RecursoNoEncontradoException e) {
            return null; // la consulta aún no tiene resumen
        }
    }

    @SchemaMapping(typeName = "Resumen")
    public Map<String, Object> consulta(Map<String, Object> resumen) {
        return api.objeto(HISTORIAS, "/api/v1/consultas/{id}", resumen.get("consultaId"));
    }
}
