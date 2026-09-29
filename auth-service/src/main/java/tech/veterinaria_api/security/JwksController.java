package tech.veterinaria_api.security;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Llave pública de firma en formato JWKS. Los demás microservicios la consultan
 * ({@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}) para validar los JWT sin compartir secretos.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class JwksController {

    private final RSAKey jwtRsaKey;

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Llave pública (JWKS) para validar los JWT emitidos")
    public Map<String, Object> jwks() {
        return new JWKSet(jwtRsaKey.toPublicJWK()).toJSONObject();
    }
}
