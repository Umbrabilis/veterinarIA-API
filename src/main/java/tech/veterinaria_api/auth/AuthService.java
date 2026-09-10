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
import tech.veterinaria_api.common.CredencialesInvalidasException;
import tech.veterinaria_api.common.EmailYaRegistradoException;
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

    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioService.existePorEmail(request.email())) {
            throw new EmailYaRegistradoException(request.email());
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Usuario usuario = usuarioService.crear(request.nombre(), request.email(), passwordHash, request.rol());
        return emitirToken(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioService.buscarPorEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        return emitirToken(usuario);
    }

    private AuthResponse emitirToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(expirationMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("veterinaria-api")
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId().toString())
                .claim("rol", usuario.getRol().name())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new AuthResponse(token, "Bearer", expirationMinutes * 60, UsuarioResponse.de(usuario));
    }
}
