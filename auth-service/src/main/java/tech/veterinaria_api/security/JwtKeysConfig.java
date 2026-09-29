package tech.veterinaria_api.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Llaves RSA con las que auth-service firma los JWT. Para escalar horizontalmente, todas las réplicas deben
 * compartir el mismo par: se cargan de archivos PEM ({@code app.jwt.private-key-location} en PKCS#8 y
 * {@code app.jwt.public-key-location} en X.509). Sin esas propiedades se genera un par en memoria, válido solo
 * con una réplica (desarrollo y tests). La parte pública se publica como JWKS para el resto de servicios.
 */
@Configuration
public class JwtKeysConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeysConfig.class);

    @Bean
    public KeyPair jwtKeyPair(ResourceLoader resourceLoader,
            @Value("${app.jwt.private-key-location:}") String privateKeyLocation,
            @Value("${app.jwt.public-key-location:}") String publicKeyLocation) {
        if (!privateKeyLocation.isBlank() && !publicKeyLocation.isBlank()) {
            RSAPrivateKey privada = leer(resourceLoader.getResource(privateKeyLocation), true);
            RSAPublicKey publica = leer(resourceLoader.getResource(publicKeyLocation), false);
            return new KeyPair(publica, privada);
        }
        log.warn("app.jwt.*-key-location no configurado: se genera un par RSA en memoria. "
                + "Solo sirve con una réplica de auth-service.");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo generar el par de llaves RSA para JWT", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T leer(Resource recurso, boolean privada) {
        try (InputStream in = recurso.getInputStream()) {
            return (T) (privada ? RsaKeyConverters.pkcs8().convert(in) : RsaKeyConverters.x509().convert(in));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la llave JWT: " + recurso.getDescription(), e);
        }
    }

    @Bean
    public RSAKey jwtRsaKey(KeyPair jwtKeyPair) {
        try {
            // kid derivado de la llave: idéntico en todas las réplicas que comparten el par.
            return new RSAKey.Builder((RSAPublicKey) jwtKeyPair.getPublic())
                    .privateKey((RSAPrivateKey) jwtKeyPair.getPrivate())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo calcular el kid de la llave JWT", e);
        }
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey jwtRsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(jwtRsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair jwtKeyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic()).build();
    }
}
