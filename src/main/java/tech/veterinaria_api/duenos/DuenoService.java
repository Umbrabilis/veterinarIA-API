package tech.veterinaria_api.duenos;

import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.veterinaria_api.common.RecursoNoEncontradoException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuenoService {
    private final DuenoRepository repository;

    public Page<DuenoResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(DuenoResponse::de);
    }

    public DuenoResponse consultar(UUID id) {
        return DuenoResponse.de(buscar(id));
    }

    public void exigirExistente(UUID id) {
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException("Dueño no encontrado: " + id);
        }
    }

    @Transactional
    public DuenoResponse crear(DuenoRequest request) {
        Dueno dueno = new Dueno();
        dueno.setId(UUID.randomUUID());
        aplicar(dueno, request);
        return DuenoResponse.de(repository.save(dueno));
    }

    @Transactional
    public DuenoResponse actualizar(UUID id, DuenoRequest request) {
        Dueno dueno = buscar(id);
        aplicar(dueno, request);
        return DuenoResponse.de(repository.save(dueno));
    }

    private Dueno buscar(UUID id) {
        return repository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Dueño no encontrado: " + id));
    }

    private void aplicar(Dueno d, DuenoRequest r) {
        d.setNombre(r.nombre().strip());
        d.setEmail(r.email().strip().toLowerCase(Locale.ROOT));
        d.setTelefono(r.telefono().strip());
        d.setDireccion(r.direccion() == null ? null : r.direccion().strip());
    }
}
