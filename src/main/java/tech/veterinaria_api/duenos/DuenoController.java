package tech.veterinaria_api.duenos;

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
@RequestMapping("/api/v1/duenos")
@RequiredArgsConstructor
@Tag(name = "Dueños")
public class DuenoController {
    private final DuenoService service;

    @PostMapping
    public ResponseEntity<DuenoResponse> crear(@Valid @RequestBody DuenoRequest request) {
        DuenoResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/duenos/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public DuenoResponse consultar(@PathVariable UUID id) {
        return service.consultar(id);
    }

    @GetMapping
    public PaginaResponse<DuenoResponse> listar(
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return PaginaResponse.de(service.listar(PageRequest.of(pagina, tamano, Sort.by("nombre", "id"))));
    }

    @PutMapping("/{id}")
    public DuenoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody DuenoRequest request) {
        return service.actualizar(id, request);
    }
}
