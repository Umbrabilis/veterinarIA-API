package tech.veterinaria_api.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones de negocio a {@link ApiError}. Los mensajes nunca incluyen datos personales
 * de propietarios (Ley 1581 de 2012): las excepciones se construyen con textos genéricos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        List<String> globales = ex.getBindingResult().getGlobalErrors().stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        List<String> todos = new java.util.ArrayList<>(detalles);
        todos.addAll(globales);
        return construir(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", request, todos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleValidacionParametros(HandlerMethodValidationException ex,
            WebRequest request) {
        // Solo los mensajes: nunca el valor rechazado.
        List<String> detalles = ex.getAllErrors().stream().map(MessageSourceResolvable::getDefaultMessage).toList();
        return construir(HttpStatus.BAD_REQUEST, "Los parámetros enviados no son válidos", request, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleCuerpoIlegible(HttpMessageNotReadableException ex, WebRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no tiene un formato válido", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleParametroInvalido(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' no tiene un formato válido",
                request);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<ApiError> handleNoAutenticado(NoAutenticadoException ex, WebRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ApiError> handleAccesoDenegado(AccesoDenegadoException ex, WebRequest request) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNoEncontrado(RecursoNoEncontradoException ex, WebRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> handleConflicto(ConflictoException ex, WebRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegridad(DataIntegrityViolationException ex, WebRequest request) {
        // El detalle de PostgreSQL puede contener valores de columnas (correo, documento): no se expone.
        log.warn("Violación de integridad en {}: {}", path(request), ex.getClass().getSimpleName());
        return construir(HttpStatus.CONFLICT, "La operación entra en conflicto con el estado actual de los datos",
                request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> handleReglaNegocio(ReglaNegocioException ex, WebRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request);
    }

    @ExceptionHandler(ServicioRemotoException.class)
    public ResponseEntity<ApiError> handleServicioRemoto(ServicioRemotoException ex, WebRequest request) {
        log.error("Fallo al llamar a otro microservicio desde {}: {}", path(request), ex.getMessage());
        return construir(HttpStatus.BAD_GATEWAY, "Un servicio interno no está disponible. Intenta de nuevo.",
                request);
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, WebRequest request) {
        return construir(status, mensaje, request, List.of());
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, WebRequest request,
            List<String> detalles) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), mensaje, path(request), detalles);
        return ResponseEntity.status(status).body(body);
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
