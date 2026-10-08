package tech.veterinaria_api.auth;

import tech.veterinaria_api.common.NoAutenticadoException;

public class CredencialesInvalidasException extends NoAutenticadoException {

    public CredencialesInvalidasException() {
        super("Email o contraseña inválidos");
    }
}
