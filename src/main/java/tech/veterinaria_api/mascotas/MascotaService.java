package tech.veterinaria_api.mascotas;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.duenos.DuenoService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MascotaService {
    private final MascotaRepository repository;
    private final DuenoService duenoService;

    public Page<MascotaResponse> listar(UUID duenoId, Pageable pageable) {
        if (duenoId == null) {
            return repository.findAll(pageable).map(MascotaResponse::de);
        }
        duenoService.exigirExistente(duenoId);
        return repository.findByDuenoId(duenoId, pageable).map(MascotaResponse::de);
    }

    public MascotaResponse consultar(UUID id) {
        return MascotaResponse.de(buscar(id));
    }

    @Transactional
    public MascotaResponse crear(MascotaRequest request) {
        duenoService.exigirExistente(request.duenoId());
        Mascota mascota = new Mascota();
        mascota.setId(UUID.randomUUID());
        aplicar(mascota, request);
        return MascotaResponse.de(repository.save(mascota));
    }

    @Transactional
    public MascotaResponse actualizar(UUID id, MascotaRequest request) {
        Mascota mascota = buscar(id);
        duenoService.exigirExistente(request.duenoId());
        aplicar(mascota, request);
        return MascotaResponse.de(repository.save(mascota));
    }

    private Mascota buscar(UUID id) {
        return repository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Mascota no encontrada: " + id));
    }

    private void aplicar(Mascota m, MascotaRequest r) {
        m.setDuenoId(r.duenoId());
        m.setNombre(r.nombre().strip());
        m.setEspecie(r.especie().strip());
        m.setRaza(r.raza() == null ? null : r.raza().strip());
        m.setSexo(r.sexo());
        m.setFechaNacimiento(r.fechaNacimiento());
    }
}
