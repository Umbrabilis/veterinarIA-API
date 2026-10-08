package tech.veterinaria_api.vacunas;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
import tech.veterinaria_api.security.UsuarioActual;
import tech.veterinaria_api.vacunas.dto.VacunaRequest;
import tech.veterinaria_api.vacunas.dto.VacunaResponse;

@RestController
@RequestMapping("/api/v1/vacunas")
@RequiredArgsConstructor
@Tag(name = "Vacunas", description = "Plan preventivo: vacunas aplicadas y próximas dosis")
public class VacunaController {

    private final VacunaService vacunaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una vacuna aplicada (solo veterinarios)")
    public VacunaResponse registrar(@Valid @RequestBody VacunaRequest request, @AuthenticationPrincipal Jwt jwt) {
        return VacunaResponse.de(vacunaService.registrar(request, UsuarioActual.de(jwt)));
    }

    @GetMapping
    @Operation(summary = "Carné de vacunación de una mascota")
    public List<VacunaResponse> carne(@RequestParam UUID mascotaId, @AuthenticationPrincipal Jwt jwt) {
        return vacunaService.carne(mascotaId, UsuarioActual.de(jwt)).stream().map(VacunaResponse::de).toList();
    }
}
