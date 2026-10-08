package tech.veterinaria_api.propietarios.dto;

import java.time.Instant;

import tech.veterinaria_api.propietarios.CodigoVinculacion;

/** Se muestra una sola vez: la clínica se lo entrega al dueño para que vincule su cuenta. */
public record CodigoVinculacionResponse(String codigo, Instant expiraEn) {

    public static CodigoVinculacionResponse de(CodigoVinculacion c) {
        return new CodigoVinculacionResponse(c.codigo(), c.expiraEn());
    }
}
