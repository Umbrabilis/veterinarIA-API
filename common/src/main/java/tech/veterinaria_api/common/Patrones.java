package tech.veterinaria_api.common;

/**
 * Expresiones regulares compartidas por los DTO de todos los microservicios (se usan en {@code @Pattern}).
 * El frontend replica las mismas reglas en {@code src/shared/validaciones.ts}: si cambias una, cambia la otra.
 *
 * <p>Los textos clínicos libres (síntomas, diagnóstico, notas...) no se restringen por caracteres, solo por
 * longitud: el veterinario necesita escribir símbolos como "°", "%" o "mg/kg". El frontend los muestra
 * escapados.
 */
public final class Patrones {

    /** Nombre de una persona: letras (con tildes y ñ), espacios, punto, apóstrofo y guion. */
    public static final String NOMBRE_PERSONA = "^\\p{L}[\\p{L} .'-]*$";

    /** Nombre de una mascota o de una vacuna: además admite números ("Firulais 2", "Triple felina"). */
    public static final String NOMBRE_COSA = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .'()/-]*$";

    /** Raza o color: letras, espacios y separadores simples. Vacío se permite (campo opcional). */
    public static final String TEXTO_DESCRIPTIVO = "^[\\p{L} .,'/-]*$";

    /**
     * Correo con dominio y extensión ({@code usuario@dominio.co}). Complementa a {@code @Email}, que por sí
     * solo acepta {@code usuario@dominio} sin extensión.
     */
    public static final String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
            + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$";

    /** Contraseña: al menos una letra y un número (la longitud se valida aparte con {@code @Size}). */
    public static final String PASSWORD = "^(?=.*\\p{L})(?=.*\\p{N}).*$";

    /** Teléfono: dígitos y separadores comunes, con entre 7 y 15 dígitos reales. */
    public static final String TELEFONO = "^(?=(?:\\D*\\d){7,15}\\D*$)[0-9+()\\s-]{7,30}$";

    /** Documento de identidad (CC, CE, pasaporte, NIT): letras, números, punto y guion. */
    public static final String DOCUMENTO = "^[A-Za-z0-9.-]{3,30}$";

    /** Dirección colombiana: "Calle 10 # 5-20, Apto 301". */
    public static final String DIRECCION = "^[\\p{L}\\p{N} #.,'°º/()-]*$";

    /** Lote de una vacuna: letras, números y guion. */
    public static final String LOTE = "^[A-Za-z0-9-]*$";

    /** Código de vinculación: letras y números, se aceptan espacios o guiones al dictarlo. */
    public static final String CODIGO_VINCULACION = "^[A-Za-z0-9\\s-]+$";

    /** Texto de búsqueda: sin caracteres de control ni símbolos que no aparecen en nombres o documentos. */
    public static final String BUSQUEDA = "^[\\p{L}\\p{N} .@'-]*$";

    private Patrones() {
    }
}
