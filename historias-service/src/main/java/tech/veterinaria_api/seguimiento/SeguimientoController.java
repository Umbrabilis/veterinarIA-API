package tech.veterinaria_api.seguimiento;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/seguimiento")
@RequiredArgsConstructor
@Tag(name = "Seguimiento", description = "Controles y refuerzos de vacuna próximos")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    @Operation(summary = "Controles y refuerzos de vacuna de los próximos días que aún no se atienden")
    public List<SeguimientoItem> pendientes(@RequestParam(defaultValue = "30") @Min(0) @Max(365) int dias) {
        return seguimientoService.pendientes(dias);
    }
}
