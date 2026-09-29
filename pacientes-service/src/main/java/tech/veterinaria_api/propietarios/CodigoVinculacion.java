package tech.veterinaria_api.propietarios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Código de un solo uso para vincular una cuenta PROPIETARIO con su registro en la clínica. Solo se guarda su
 * hash; el código en claro se muestra una única vez al personal que lo genera.
 */
public record CodigoVinculacion(String codigo, Instant expiraEn) {

    // Sin caracteres ambiguos (0/O, 1/I/L): se dicta o se copia a mano. 10 caracteres ≈ 50 bits.
    private static final char[] ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LONGITUD = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    static String nuevoCodigo() {
        StringBuilder codigo = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            codigo.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
        }
        return codigo.toString();
    }

    static String hash(String codigo) {
        String normalizado = codigo == null ? "" : codigo.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
