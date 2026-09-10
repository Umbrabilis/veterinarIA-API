package tech.veterinaria_api.mascotas;

import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import tech.veterinaria_api.common.PaginaResponse;

@RestController
@RequestMapping("/api/v1/mascotas")
@RequiredArgsConstructor
@Tag(name = "Mascotas")
public class MascotaController {
    private final MascotaService service;

    @PostMapping
    public ResponseEntity<MascotaResponse> crear(@Valid @RequestBody MascotaRequest request) {
        MascotaResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/mascotas/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public MascotaResponse consultar(@PathVariable UUID id) {
        return service.consultar(id);
    }

    @GetMapping
    public PaginaResponse<MascotaResponse> listar(
            @RequestParam(required = false) UUID duenoId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return PaginaResponse.de(service.listar(duenoId, PageRequest.of(pagina, tamano, Sort.by("nombre", "id"))));
    }

    @PutMapping("/{id}")
    public MascotaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody MascotaRequest request) {
        return service.actualizar(id, request);
    }
}
