package tech.veterinaria_api.resumenes;

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
import tech.veterinaria_api.resumenes.dto.AprobarResumenRequest;
import tech.veterinaria_api.resumenes.dto.ResumenResponse;
import tech.veterinaria_api.security.UsuarioActual;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Resúmenes IA", description = "Resumen de la consulta para el dueño: la IA redacta, el veterinario aprueba")
public class ResumenController {

    private final ResumenService resumenService;

    @PostMapping("/consultas/{consultaId}/resumen")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Genera el borrador si no se generó al cerrar la consulta")
    public ResumenResponse generar(@PathVariable UUID consultaId, @AuthenticationPrincipal Jwt jwt) {
        return ResumenResponse.de(resumenService.generarBorradorPara(consultaId, UsuarioActual.de(jwt)));
    }

    @GetMapping("/consultas/{consultaId}/resumen")
    @Operation(summary = "Resumen de una consulta (el dueño solo lo ve una vez enviado)")
    public ResumenResponse porConsulta(@PathVariable UUID consultaId, @AuthenticationPrincipal Jwt jwt) {
        UsuarioActual usuario = UsuarioActual.de(jwt);
        return vista(resumenService.porConsulta(consultaId, usuario), usuario);
    }

    @GetMapping("/resumenes")
    @Operation(summary = "Bandeja de revisión por estado (BORRADOR por defecto)")
    public List<ResumenResponse> porEstado(@RequestParam(defaultValue = "BORRADOR") EstadoResumen estado,
            @AuthenticationPrincipal Jwt jwt) {
        return resumenService.porEstado(estado, UsuarioActual.de(jwt)).stream().map(ResumenResponse::de).toList();
    }

    @GetMapping("/resumenes/{id}")
    @Operation(summary = "Un resumen (el dueño solo lo ve una vez enviado)")
    public ResumenResponse obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UsuarioActual usuario = UsuarioActual.de(jwt);
        return vista(resumenService.obtener(id, usuario), usuario);
    }

    @PutMapping("/resumenes/{id}")
    @Operation(summary = "El veterinario edita el borrador")
    public ResumenResponse editar(@PathVariable UUID id, @Valid @RequestBody ContenidoResumen contenido,
            @AuthenticationPrincipal Jwt jwt) {
        return ResumenResponse.de(resumenService.editar(id, contenido, UsuarioActual.de(jwt)));
    }

    @PostMapping("/resumenes/{id}/aprobar")
    @Operation(summary = "El veterinario aprueba el resumen (opcionalmente con su versión final)")
    public ResumenResponse aprobar(@PathVariable UUID id,
            @Valid @RequestBody(required = false) AprobarResumenRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResumenResponse.de(resumenService.aprobar(id, request == null ? null : request.contenidoFinal(),
                UsuarioActual.de(jwt)));
    }

    @PostMapping("/resumenes/{id}/enviar")
    @Operation(summary = "Envía al dueño por correo un resumen ya aprobado")
    public ResumenResponse enviar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResumenResponse.de(resumenService.enviar(id, UsuarioActual.de(jwt)));
    }

    private static ResumenResponse vista(ResumenConsulta resumen, UsuarioActual usuario) {
        return usuario.esPersonalClinico() ? ResumenResponse.de(resumen) : ResumenResponse.paraPropietario(resumen);
    }
}
