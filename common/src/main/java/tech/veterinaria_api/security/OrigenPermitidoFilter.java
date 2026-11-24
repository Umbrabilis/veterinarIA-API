package tech.veterinaria_api.security;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protección CSRF para la sesión por cookie. El navegador adjunta la cookie a cualquier petición hacia la API, incluso
 * si la origina otra página; por eso una petición que cambia datos (POST, PUT, PATCH, DELETE) y se autentica con la
 * cookie solo se acepta si su {@code Origin} es uno de los orígenes permitidos (el frontend) o el propio host (Swagger
 * a través del gateway). Las peticiones con {@code Authorization: Bearer} no se revisan: un navegador nunca agrega ese
 * header por su cuenta.
 */
public class OrigenPermitidoFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final String nombreCookie;
    private final Set<String> origenesPermitidos;

    public OrigenPermitidoFilter(String nombreCookie, Set<String> origenesPermitidos) {
        this.nombreCookie = nombreCookie;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean usaCookie = request.getHeader(HttpHeaders.AUTHORIZATION) == null
                && CookieOHeaderTokenResolver.valorCookie(request, nombreCookie) != null;
        if (usaCookie && !METODOS_SEGUROS.contains(request.getMethod()) && !origenValido(request)) {
            rechazar(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean origenValido(HttpServletRequest request) {
        String origen = request.getHeader(HttpHeaders.ORIGIN);
        if (origen == null) {
            // Algunos navegadores antiguos no envían Origin; Referer trae la misma información.
            origen = origenDe(request.getHeader(HttpHeaders.REFERER));
        }
        if (origen == null) {
            return false;
        }
        if (origenesPermitidos.contains(origen)) {
            return true;
        }
        String host = request.getHeader(HttpHeaders.HOST);
        String hostDelOrigen = origenDe(origen) == null ? null : URI.create(origen).getRawAuthority();
        return host != null && host.equalsIgnoreCase(hostDelOrigen);
    }

    private static String origenDe(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getRawAuthority() == null) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void rechazar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"timestamp":"%s","status":403,"error":"Forbidden",\
                "message":"Origen de la petición no permitido","path":"%s","detalles":[]}"""
                .formatted(Instant.now(), request.getRequestURI().replace("\"", "")));
    }
}
