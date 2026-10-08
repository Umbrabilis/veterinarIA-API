package tech.veterinaria_api.consultas;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.consultas.dto.ConsultaResponse;
import tech.veterinaria_api.consultas.dto.CrearConsultaRequest;
import tech.veterinaria_api.consultas.dto.DatosClinicosRequest;
import tech.veterinaria_api.consultas.dto.EnmiendaRequest;
import tech.veterinaria_api.consultas.dto.EnmiendaResponse;
import tech.veterinaria_api.security.UsuarioActual;

@RestController
@RequestMapping("/api/v1/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas", description = "Historia clínica: consultas, cierre y enmiendas")
public class ConsultaController {

    private final ConsultaService consultaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una consulta (solo veterinarios)")
    public ConsultaResponse crear(@Valid @RequestBody CrearConsultaRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ConsultaResponse.de(consultaService.crear(request.mascotaId(), request.citaId(), request.datos(),
                UsuarioActual.de(jwt)));
    }

    @GetMapping
    @Operation(summary = "Historia clínica de una mascota (el propietario ve solo consultas cerradas)")
    public List<ConsultaResponse> historia(@RequestParam UUID mascotaId, @AuthenticationPrincipal Jwt jwt) {
        return consultaService.historiaDeMascota(mascotaId, UsuarioActual.de(jwt)).stream()
                .map(ConsultaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una consulta con sus enmiendas")
    public ConsultaResponse obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Consulta consulta = consultaService.obtener(id, UsuarioActual.de(jwt));
        return ConsultaResponse.de(consulta, consultaService.enmiendas(consulta.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una consulta abierta (solo el veterinario que la atiende)")
    public ConsultaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody DatosClinicosRequest datos,
            @AuthenticationPrincipal Jwt jwt) {
        return ConsultaResponse.de(consultaService.actualizar(id, datos, UsuarioActual.de(jwt)));
    }

    @PostMapping("/{id}/cerrar")
    @Operation(summary = "Cierra la consulta: queda inmutable y se genera el borrador del resumen para el dueño")
    public ConsultaResponse cerrar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ConsultaResponse.de(consultaService.cerrar(id, UsuarioActual.de(jwt)));
    }

    @PostMapping("/{id}/enmiendas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega una enmienda a una consulta cerrada (queda registro de quién y cuándo)")
    public EnmiendaResponse enmendar(@PathVariable UUID id, @Valid @RequestBody EnmiendaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return EnmiendaResponse.de(consultaService.enmendar(id, request, UsuarioActual.de(jwt)));
    }
}
