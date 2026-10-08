package tech.veterinaria_api.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie de sesión con el JWT. HttpOnly: el JavaScript del navegador no puede leerla, así que un script inyectado
 * (XSS) no puede robar el token como sí podía hacerlo desde localStorage. Path {@code /api}: el navegador solo la
 * envía a la API.
 */
@Component
public class CookieSesion {

    private static final String RUTA = "/api";

    private final String nombre;
    private final boolean secure;
    private final String sameSite;
    private final Duration duracion;

    public CookieSesion(@Value("${app.sesion.cookie.nombre}") String nombre,
            @Value("${app.sesion.cookie.secure}") boolean secure,
            @Value("${app.sesion.cookie.same-site}") String sameSite,
            @Value("${app.jwt.expiration-minutes}") long expiracionMinutos) {
        this.nombre = nombre;
        this.secure = secure;
        this.sameSite = sameSite;
        this.duracion = Duration.ofMinutes(expiracionMinutos);
    }

    public ResponseCookie con(String token) {
        return base(token).maxAge(duracion).build();
    }

    /** Misma cookie vacía y vencida: el navegador la borra. */
    public ResponseCookie vencida() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(RUTA);
    }
}
