package tech.veterinaria_api.mascotas;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.mascotas.dto.MascotaRequest;
import tech.veterinaria_api.propietarios.PropietarioService;
import tech.veterinaria_api.security.UsuarioActual;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final PropietarioService propietarioService;

    @Transactional
    public Mascota crear(MascotaRequest request, UsuarioActual usuario) {
        propietarioService.obtener(request.propietarioId(), usuario);
        Mascota mascota = Mascota.builder()
                .id(UUID.randomUUID())
                .activo(true)
                .build();
        aplicar(mascota, request);
        return mascotaRepository.save(mascota);
    }

    @Transactional
    public Mascota actualizar(UUID id, MascotaRequest request, UsuarioActual usuario) {
        Mascota mascota = buscar(id);
        propietarioService.obtener(request.propietarioId(), usuario);
        aplicar(mascota, request);
        return mascota;
    }

    @Transactional(readOnly = true)
    public Page<Mascota> listar(UUID propietarioId, String busqueda, Pageable pageable) {
        boolean sinBusqueda = busqueda == null || busqueda.isBlank();
        if (propietarioId == null) {
            return sinBusqueda ? mascotaRepository.findAll(pageable)
                    : mascotaRepository.buscar(busqueda.trim(), pageable);
        }
        return sinBusqueda ? mascotaRepository.findByPropietarioId(propietarioId, pageable)
                : mascotaRepository.buscarDePropietario(propietarioId, busqueda.trim(), pageable);
    }

    @Transactional
    public List<Mascota> listarMias(UsuarioActual usuario) {
        UUID propietarioId = propietarioService.obtenerMio(usuario).getId();
        return mascotaRepository.findByPropietarioIdOrderByNombreAsc(propietarioId);
    }

    /**
     * Personal de la clínica: cualquier mascota. Propietario: solo las suyas. Los demás servicios llaman a este
     * método (con el JWT reenviado) para verificar la pertenencia antes de exponer citas o historia clínica.
     */
    @Transactional
    public Mascota obtener(UUID id, UsuarioActual usuario) {
        Mascota mascota = buscar(id);
        if (usuario.esPersonalClinico()) {
            return mascota;
        }
        if (usuario.esPropietario() && mascota.getPropietarioId().equals(propietarioIdDe(usuario))) {
            return mascota;
        }
        throw new AccesoDenegadoException();
    }

    private UUID propietarioIdDe(UsuarioActual usuario) {
        try {
            return propietarioService.obtenerMio(usuario).getId();
        } catch (RecursoNoEncontradoException e) {
            throw new AccesoDenegadoException();
        }
    }

    private Mascota buscar(UUID id) {
        return mascotaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
    }

    private void aplicar(Mascota mascota, MascotaRequest request) {
        mascota.setPropietarioId(request.propietarioId());
        mascota.setNombre(request.nombre().trim());
        mascota.setEspecie(request.especie());
        mascota.setRaza(request.raza() == null || request.raza().isBlank() ? null : request.raza().trim());
        mascota.setSexo(request.sexo());
        mascota.setFechaNacimiento(request.fechaNacimiento());
        mascota.setPesoKg(request.pesoKg());
        mascota.setColor(request.color() == null || request.color().isBlank() ? null : request.color().trim());
        if (request.activo() != null) {
            mascota.setActivo(request.activo());
        }
    }
}
