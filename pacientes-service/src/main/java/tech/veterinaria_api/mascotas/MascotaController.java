package tech.veterinaria_api.mascotas;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.PaginaResponse;
import tech.veterinaria_api.common.Patrones;
import tech.veterinaria_api.mascotas.dto.MascotaRequest;
import tech.veterinaria_api.mascotas.dto.MascotaResponse;
import tech.veterinaria_api.security.UsuarioActual;

@RestController
@RequestMapping("/api/v1/mascotas")
@RequiredArgsConstructor
@Tag(name = "Mascotas", description = "Pacientes de la clínica")
public class MascotaController {

    private static final String PERSONAL_CLINICO = "hasAnyRole('ADMINISTRADOR','VETERINARIO')";

    private final MascotaService mascotaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PERSONAL_CLINICO)
    @Operation(summary = "Registra una mascota")
    public MascotaResponse crear(@Valid @RequestBody MascotaRequest request, @AuthenticationPrincipal Jwt jwt) {
        return MascotaResponse.de(mascotaService.crear(request, UsuarioActual.de(jwt)));
    }

    @GetMapping
    @PreAuthorize(PERSONAL_CLINICO)
    @Operation(summary = "Lista mascotas; filtra por propietario y busca por nombre de la mascota, nombre del propietario o documento exacto")
    public PaginaResponse<MascotaResponse> listar(@RequestParam(required = false) UUID propietarioId,
            @RequestParam(required = false)
            @Size(max = 100, message = "La búsqueda no puede superar 100 caracteres")
            @Pattern(regexp = Patrones.BUSQUEDA, message = "La búsqueda contiene caracteres no permitidos") String busqueda,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return PaginaResponse.de(
                mascotaService.listar(propietarioId, busqueda, PageRequest.of(pagina, tamano, Sort.by("nombre"))),
                MascotaResponse::de);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @Operation(summary = "Mascotas del propietario autenticado")
    public List<MascotaResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return mascotaService.listarMias(UsuarioActual.de(jwt)).stream().map(MascotaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Una mascota: personal de la clínica o su propietario")
    public MascotaResponse obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return MascotaResponse.de(mascotaService.obtener(id, UsuarioActual.de(jwt)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(PERSONAL_CLINICO)
    @Operation(summary = "Actualiza una mascota (incluye marcarla como inactiva)")
    public MascotaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MascotaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return MascotaResponse.de(mascotaService.actualizar(id, request, UsuarioActual.de(jwt)));
    }
}
