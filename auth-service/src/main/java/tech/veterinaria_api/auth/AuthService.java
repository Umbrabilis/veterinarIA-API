package tech.veterinaria_api.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.auth.dto.AuthResponse;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.ReglaNegocioException;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.security.JwtKeysConfig;
import tech.veterinaria_api.usuarios.Usuario;
import tech.veterinaria_api.usuarios.UsuarioService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    /**
     * Crea la cuenta pero no inicia sesión: el registro lo puede hacer un administrador para otra persona, y
     * emitir la cookie le cambiaría la sesión al administrador.
     */
    public UsuarioResponse registrar(RegisterRequest request) {
        if (request.rol() == RolUsuario.PROPIETARIO) {
            // Los dueños no usan el sistema: la clínica les escribe al correo registrado en su ficha.
            throw new ReglaNegocioException("El sistema solo admite cuentas de administrador o veterinario");
        }
        String email = normalizarEmail(request.email());
        if (usuarioService.existePorEmail(email)) {
            throw new EmailYaRegistradoException();
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Usuario usuario = usuarioService.crear(request.nombre().trim(), email, passwordHash, request.rol());
        return UsuarioResponse.de(usuario);
    }

    public SesionEmitida login(LoginRequest request) {
        Usuario usuario = usuarioService.buscarPorEmail(normalizarEmail(request.email()))
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        if (usuario.getRol() == RolUsuario.PROPIETARIO) {
            // Cuentas creadas antes de que el panel quedara solo para el personal de la clínica.
            throw new AccesoDenegadoException("Esta cuenta no tiene acceso al panel de la clínica");
        }
        return emitirToken(usuario);
    }

    /** Un mismo correo con distintas mayúsculas no puede crear dos cuentas. */
    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** JWT firmado y lo que se le devuelve al frontend. El token solo viaja en la cookie HttpOnly. */
    public record SesionEmitida(String token, AuthResponse respuesta) {
    }

    private SesionEmitida emitirToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(expirationMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtKeysConfig.ISSUER)
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId().toString())
                .claim("rol", usuario.getRol().name())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new SesionEmitida(token, new AuthResponse(expirationMinutes * 60, UsuarioResponse.de(usuario)));
    }
}
