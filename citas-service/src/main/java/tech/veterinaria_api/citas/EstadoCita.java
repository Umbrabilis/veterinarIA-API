package tech.veterinaria_api.citas;

import java.util.Map;
import java.util.Set;

public enum EstadoCita {
    PROGRAMADA,
    CONFIRMADA,
    CANCELADA,
    ATENDIDA,
    NO_ASISTIO;

    private static final Map<EstadoCita, Set<EstadoCita>> TRANSICIONES = Map.of(
            PROGRAMADA, Set.of(CONFIRMADA, CANCELADA, ATENDIDA, NO_ASISTIO),
            CONFIRMADA, Set.of(CANCELADA, ATENDIDA, NO_ASISTIO));

    /** Ocupa horario en la agenda (debe coincidir con el WHERE de las restricciones EXCLUDE). */
    public boolean esActiva() {
        return this == PROGRAMADA || this == CONFIRMADA;
    }

    public boolean puedePasarA(EstadoCita destino) {
        return TRANSICIONES.getOrDefault(this, Set.of()).contains(destino);
    }
}
