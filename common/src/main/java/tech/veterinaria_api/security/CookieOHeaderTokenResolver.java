package tech.veterinaria_api.security;

import java.util.List;

import org.springframework.http.server.PathContainer;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Obtiene el JWT de la petición. El navegador lo envía en la cookie HttpOnly de sesión (el JavaScript del frontend
 * nunca lo ve); las llamadas entre microservicios y las pruebas lo envían en {@code Authorization: Bearer}, que
 * tiene prioridad.
 *
 * <p>En las rutas públicas la cookie se ignora: si llega una cookie vencida a {@code /auth/login}, el login debe
 * funcionar igual en lugar de responder 401.
 */
public class CookieOHeaderTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver header = new DefaultBearerTokenResolver();
    private final String nombreCookie;
    private final List<PathPattern> rutasPublicas;

    public CookieOHeaderTokenResolver(String nombreCookie, List<String> rutasPublicas) {
        this.nombreCookie = nombreCookie;
        this.rutasPublicas = rutasPublicas.stream().map(PathPatternParser.defaultInstance::parse).toList();
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String token = header.resolve(request);
        if (token != null || esRutaPublica(request)) {
            return token;
        }
        return valorCookie(request, nombreCookie);
    }

    static String valorCookie(HttpServletRequest request, String nombre) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean esRutaPublica(HttpServletRequest request) {
        PathContainer ruta = PathContainer.parsePath(request.getRequestURI().substring(request.getContextPath().length()));
        return rutasPublicas.stream().anyMatch(patron -> patron.matches(ruta));
    }
}
