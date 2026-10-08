package tech.veterinaria_api.auth;

import tech.veterinaria_api.common.ConflictoException;

public class EmailYaRegistradoException extends ConflictoException {

    public EmailYaRegistradoException() {
        // Sin el email en el mensaje: puede ser el de un propietario (dato personal, Ley 1581).
        super("Ya existe un usuario registrado con ese email");
    }
}
