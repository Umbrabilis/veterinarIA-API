package tech.veterinaria_api.testing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import tech.veterinaria_api.common.RolUsuario;

/** Firma JWT con los mismos claims que emite auth-service. */
public class TokensDePrueba {

    private final JwtEncoder encoder;

    public TokensDePrueba(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String token(UUID usuarioId, String email, RolUsuario rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("veterinaria-api")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(30, ChronoUnit.MINUTES))
                .subject(email)
                .claim("userId", usuarioId.toString())
                .claim("rol", rol.name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String bearer(UUID usuarioId, String email, RolUsuario rol) {
        return "Bearer " + token(usuarioId, email, rol);
    }
}
