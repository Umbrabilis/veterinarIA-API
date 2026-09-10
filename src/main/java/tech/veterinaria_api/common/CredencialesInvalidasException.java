package tech.veterinaria_api.common;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña inválidos");
    }
}
