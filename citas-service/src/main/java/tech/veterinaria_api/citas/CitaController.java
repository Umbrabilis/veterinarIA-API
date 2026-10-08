package tech.veterinaria_api.citas;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.citas.dto.CambiarEstadoCitaRequest;
import tech.veterinaria_api.citas.dto.CitaResponse;
import tech.veterinaria_api.citas.dto.CrearCitaRequest;
import tech.veterinaria_api.citas.dto.ReprogramarCitaRequest;
import tech.veterinaria_api.security.UsuarioActual;

@RestController
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "Agenda de la clínica")
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agenda una cita (personal de la clínica, o el propietario para su mascota)")
    public CitaResponse crear(@Valid @RequestBody CrearCitaRequest request, @AuthenticationPrincipal Jwt jwt) {
        return CitaResponse.de(citaService.crear(request, UsuarioActual.de(jwt)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    @Operation(summary = "Agenda filtrada por veterinario, mascota y rango de fechas (ISO-8601)")
    public List<CitaResponse> listar(@RequestParam(required = false) UUID veterinarioId,
            @RequestParam(required = false) UUID mascotaId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta) {
        return citaService.listar(veterinarioId, mascotaId, desde, hasta).stream().map(CitaResponse::de).toList();
    }

    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('PROPIETARIO','VETERINARIO')")
    @Operation(summary = "Citas del propietario autenticado, o agenda del veterinario autenticado")
    public List<CitaResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return citaService.listarMias(UsuarioActual.de(jwt)).stream().map(CitaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Una cita: personal de la clínica o el propietario de la mascota")
    public CitaResponse obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return CitaResponse.de(citaService.obtener(id, UsuarioActual.de(jwt)));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela una cita programada o confirmada")
    public CitaResponse cancelar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return CitaResponse.de(citaService.cancelar(id, UsuarioActual.de(jwt)));
    }

    @PatchMapping("/{id}/reprogramar")
    @Operation(summary = "Cambia el horario de una cita activa")
    public CitaResponse reprogramar(@PathVariable UUID id, @Valid @RequestBody ReprogramarCitaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return CitaResponse.de(citaService.reprogramar(id, request.inicio(), request.fin(), UsuarioActual.de(jwt)));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    @Operation(summary = "Confirma, marca como atendida / no asistió, o cancela una cita")
    public CitaResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoCitaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return CitaResponse.de(citaService.cambiarEstado(id, request.estado(), UsuarioActual.de(jwt)));
    }
}
