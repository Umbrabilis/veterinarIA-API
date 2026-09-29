package tech.veterinaria_api.propietarios;

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
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.PaginaResponse;
import tech.veterinaria_api.propietarios.dto.ActualizarMisDatosRequest;
import tech.veterinaria_api.propietarios.dto.CodigoVinculacionResponse;
import tech.veterinaria_api.propietarios.dto.PropietarioRequest;
import tech.veterinaria_api.propietarios.dto.PropietarioResponse;
import tech.veterinaria_api.propietarios.dto.VincularCuentaRequest;
import tech.veterinaria_api.security.UsuarioActual;

@RestController
@RequestMapping("/api/v1/propietarios")
@RequiredArgsConstructor
@Tag(name = "Propietarios", description = "Dueños de mascotas y sus datos de contacto")
public class PropietarioController {

    private static final String PERSONAL_CLINICO = "hasAnyRole('ADMINISTRADOR','VETERINARIO')";

    private final PropietarioService propietarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PERSONAL_CLINICO)
    @Operation(summary = "Registra un propietario (requiere autorización de tratamiento de datos)")
    public PropietarioResponse crear(@Valid @RequestBody PropietarioRequest request) {
        return PropietarioResponse.de(propietarioService.crear(request));
    }

    @GetMapping
    @PreAuthorize(PERSONAL_CLINICO)
    @Operation(summary = "Lista propietarios; busca por nombre o documento exacto")
    public PaginaResponse<PropietarioResponse> listar(@RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return PaginaResponse.de(
                propietarioService.listar(busqueda, PageRequest.of(pagina, tamano, Sort.by("nombre"))),
                PropietarioResponse::de);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @Operation(summary = "Datos del propietario autenticado")
    public PropietarioResponse miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return PropietarioResponse.de(propietarioService.obtenerMio(UsuarioActual.de(jwt)));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @Operation(summary = "El propietario autenticado actualiza su teléfono y dirección")
    public PropietarioResponse actualizarMiPerfil(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarMisDatosRequest request) {
        return PropietarioResponse.de(propietarioService.actualizarMio(UsuarioActual.de(jwt), request));
    }

    @PostMapping("/me/vincular")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @Operation(summary = "Vincula la cuenta autenticada con su registro usando el código que entregó la clínica")
    public PropietarioResponse vincular(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VincularCuentaRequest request) {
        return PropietarioResponse.de(propietarioService.vincularCuenta(UsuarioActual.de(jwt), request.codigo()));
    }

    @PostMapping("/{id}/codigo-vinculacion")
    @PreAuthorize(PERSONAL_CLINICO)
    @Operation(summary = "Genera un código de un solo uso (7 días) para que el dueño vincule su cuenta")
    public CodigoVinculacionResponse codigoVinculacion(@PathVariable UUID id) {
        return CodigoVinculacionResponse.de(propietarioService.generarCodigoVinculacion(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Un propietario: personal de la clínica o el propio propietario")
    public PropietarioResponse obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return PropietarioResponse.de(propietarioService.obtener(id, UsuarioActual.de(jwt)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(PERSONAL_CLINICO)
    @Operation(summary = "Actualiza un propietario")
    public PropietarioResponse actualizar(@PathVariable UUID id, @Valid @RequestBody PropietarioRequest request) {
        return PropietarioResponse.de(propietarioService.actualizar(id, request));
    }
}
