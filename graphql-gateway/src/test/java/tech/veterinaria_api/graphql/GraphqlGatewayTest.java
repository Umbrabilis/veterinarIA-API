package tech.veterinaria_api.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.graphql.ApiInterna.Servicio;
import tech.veterinaria_api.testing.JwtDePruebaConfiguration;
import tech.veterinaria_api.testing.TokensDePrueba;

/** Los microservicios se simulan; se prueba la composición GraphQL, el reenvío de errores y la seguridad. */
@Import(JwtDePruebaConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class GraphqlGatewayTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private TokensDePrueba tokens;

    @MockitoBean
    private ApiInterna api;

    private String vet() {
        return tokens.bearer(UUID.randomUUID(), "vet@clinica.test", RolUsuario.VETERINARIO);
    }

    private String consultar(String bearer, String query) {
        return restTestClient.post().uri("/api/v1/graphql")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void unaConsultaReuneMascotaPropietarioYCitasDeVariosServicios() {
        given(api.objeto(Servicio.PACIENTES, "/api/v1/mascotas/{id}", "m1"))
                .willReturn(Map.of("id", "m1", "nombre", "Luna", "especie", "GATO", "propietarioId", "p1"));
        given(api.objeto(Servicio.PACIENTES, "/api/v1/propietarios/{id}", "p1"))
                .willReturn(Map.of("id", "p1", "nombre", "Carlos Pérez"));
        given(api.lista(eq(Servicio.CITAS), any()))
                .willReturn(List.of(Map.of("id", "c1", "motivo", "Control", "estado", "PROGRAMADA")));

        String respuesta = consultar(vet(), """
                { mascota(id: "m1") { nombre especie propietario { nombre } citas { motivo estado } } }""");

        assertThat(respuesta).contains("\"nombre\":\"Luna\"", "\"nombre\":\"Carlos Pérez\"", "\"motivo\":\"Control\"")
                .doesNotContain("errors");
    }

    @Test
    void siUnServicioNiegaElAccesoGraphQlDevuelveElError() {
        given(api.objeto(Servicio.PACIENTES, "/api/v1/mascotas/{id}", "ajena"))
                .willThrow(new AccesoDenegadoException());

        String respuesta = consultar(vet(), "{ mascota(id: \"ajena\") { nombre } }");

        assertThat(respuesta).contains("\"errors\"", "FORBIDDEN", "\"mascota\":null");
    }

    @Test
    void sinSesionNoHayAcceso() {
        restTestClient.post().uri("/api/v1/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", "{ yo { nombre } }"))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
