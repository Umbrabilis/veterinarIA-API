package tech.veterinaria_api.testing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * Para los servicios que solo validan JWT (no los emiten): reemplaza el JwtDecoder remoto (JWKS de
 * auth-service) por uno local y expone {@link TokensDePrueba} para firmar tokens en los tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class JwtDePruebaConfiguration {

    @Bean
    KeyPair llavesDePrueba() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    JwtDecoder jwtDecoder(KeyPair llavesDePrueba) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) llavesDePrueba.getPublic()).build();
    }

    @Bean
    TokensDePrueba tokensDePrueba(KeyPair llavesDePrueba) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) llavesDePrueba.getPublic())
                .privateKey((RSAPrivateKey) llavesDePrueba.getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        return new TokensDePrueba(encoder);
    }
}
