package tech.veterinaria_api.propietarios;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.ConflictoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.propietarios.dto.ActualizarMisDatosRequest;
import tech.veterinaria_api.propietarios.dto.PropietarioRequest;
import tech.veterinaria_api.security.UsuarioActual;

@Service
@RequiredArgsConstructor
public class PropietarioService {

    private final PropietarioRepository propietarioRepository;

    @Transactional
    public Propietario crear(PropietarioRequest request) {
        String email = normalizarEmail(request.email());
        if (propietarioRepository.existsByDocumento(request.documento().trim())) {
            throw new ConflictoException("Ya existe un propietario con ese documento");
        }
        if (email != null && propietarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe un propietario con ese email");
        }
        Propietario propietario = Propietario.builder()
                .id(UUID.randomUUID())
                .consentimientoDatosEn(Instant.now())
                .build();
        aplicar(propietario, request, email);
        return propietarioRepository.save(propietario);
    }

    @Transactional
    public Propietario actualizar(UUID id, PropietarioRequest request) {
        Propietario propietario = buscar(id);
        String email = normalizarEmail(request.email());
        if (propietarioRepository.existsByDocumentoAndIdNot(request.documento().trim(), id)) {
            throw new ConflictoException("Ya existe un propietario con ese documento");
        }
        if (email != null && propietarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictoException("Ya existe un propietario con ese email");
        }
        aplicar(propietario, request, email);
        return propietario;
    }

    @Transactional(readOnly = true)
    public Page<Propietario> listar(String busqueda, Pageable pageable) {
        if (busqueda == null || busqueda.isBlank()) {
            return propietarioRepository.findAll(pageable);
        }
        return propietarioRepository.buscar(busqueda.trim(), pageable);
    }

    /** Personal de la clínica: cualquiera. Propietario: solo su propio registro. */
    @Transactional
    public Propietario obtener(UUID id, UsuarioActual usuario) {
        Propietario propietario = buscar(id);
        if (usuario.esPersonalClinico()) {
            return propietario;
        }
        if (usuario.esPropietario() && propietario.getId().equals(obtenerMio(usuario).getId())) {
            return propietario;
        }
        throw new AccesoDenegadoException();
    }

    /**
     * Registro de propietario de la cuenta autenticada. La primera vez se vincula por email con el registro
     * que creó la clínica; desde entonces se resuelve por {@code usuario_id}.
     */
    @Transactional
    public Propietario obtenerMio(UsuarioActual usuario) {
        if (!usuario.esPropietario()) {
            throw new AccesoDenegadoException("Solo las cuentas de propietario tienen este recurso");
        }
        return propietarioRepository.findByUsuarioId(usuario.id())
                .or(() -> propietarioRepository.findByEmailIgnoreCaseAndUsuarioIdIsNull(usuario.email())
                        .map(p -> {
                            p.setUsuarioId(usuario.id());
                            return p;
                        }))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Aún no tienes un registro de propietario en la clínica"));
    }

    @Transactional
    public Propietario actualizarMio(UsuarioActual usuario, ActualizarMisDatosRequest request) {
        Propietario propietario = obtenerMio(usuario);
        propietario.setTelefono(request.telefono().trim());
        propietario.setDireccion(vacioANulo(request.direccion()));
        return propietario;
    }

    Propietario buscar(UUID id) {
        return propietarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propietario no encontrado"));
    }

    private void aplicar(Propietario propietario, PropietarioRequest request, String email) {
        propietario.setNombre(request.nombre().trim());
        propietario.setDocumento(request.documento().trim());
        propietario.setTelefono(request.telefono().trim());
        propietario.setEmail(email);
        propietario.setDireccion(vacioANulo(request.direccion()));
    }

    private static String normalizarEmail(String email) {
        String valor = vacioANulo(email);
        return valor == null ? null : valor.toLowerCase();
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
