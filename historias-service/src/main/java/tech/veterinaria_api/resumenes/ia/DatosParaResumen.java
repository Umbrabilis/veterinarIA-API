package tech.veterinaria_api.resumenes.ia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import tech.veterinaria_api.consultas.Consulta;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;

/**
 * Único insumo del modelo de IA: datos clínicos y de la mascota. Por diseño no tiene ningún campo del
 * propietario (nombre, documento, teléfono, correo, dirección), así que no hay forma de enviárselos al modelo.
 */
public record DatosParaResumen(
        String nombreMascota,
        String especie,
        String raza,
        String sexo,
        String edadAproximada,
        BigDecimal pesoKg,
        String motivo,
        String sintomas,
        String examenFisico,
        BigDecimal temperaturaC,
        String diagnostico,
        String tratamiento,
        String indicaciones,
        LocalDate proximoControl
) {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static DatosParaResumen de(Consulta consulta, MascotaRemota mascota, LocalDate hoy) {
        return new DatosParaResumen(mascota.nombre(), mascota.especie(), mascota.raza(), mascota.sexo(),
                edad(mascota.fechaNacimiento(), hoy),
                consulta.getPesoKg() != null ? consulta.getPesoKg() : mascota.pesoKg(),
                consulta.getMotivo(), consulta.getSintomas(), consulta.getExamenFisico(), consulta.getTemperaturaC(),
                consulta.getDiagnostico(), consulta.getTratamiento(), consulta.getIndicaciones(),
                consulta.getProximoControl());
    }

    public String proximoControlTexto() {
        return proximoControl == null ? null : proximoControl.format(FECHA);
    }

    /** Datos en texto, delimitados, para el mensaje del usuario. */
    public String comoTexto() {
        return """
                <datos_consulta>
                Mascota: %s
                Especie: %s
                Raza: %s
                Sexo: %s
                Edad aproximada: %s
                Peso: %s
                Motivo de consulta: %s
                Síntomas: %s
                Examen físico: %s
                Temperatura: %s
                Diagnóstico: %s
                Tratamiento: %s
                Indicaciones para casa: %s
                Próximo control: %s
                </datos_consulta>
                """.formatted(
                limpio(nombreMascota), limpio(especie), limpio(raza), limpio(sexo), limpio(edadAproximada),
                pesoKg == null ? "no registrado" : pesoKg + " kg", limpio(motivo), limpio(sintomas),
                limpio(examenFisico), temperaturaC == null ? "no registrada" : temperaturaC + " °C",
                limpio(diagnostico), limpio(tratamiento), limpio(indicaciones),
                proximoControl == null ? "no indicado" : proximoControlTexto());
    }

    /** Evita que un texto clínico cierre el delimitador y se salga del bloque de datos. */
    private static String limpio(String valor) {
        if (valor == null || valor.isBlank()) {
            return "no registrado";
        }
        return valor.replace("<", "‹").replace(">", "›").trim();
    }

    private static String edad(LocalDate nacimiento, LocalDate hoy) {
        if (nacimiento == null) {
            return null;
        }
        Period p = Period.between(nacimiento, hoy);
        if (p.getYears() >= 1) {
            return p.getYears() + (p.getYears() == 1 ? " año" : " años");
        }
        int meses = Math.max(p.getMonths(), 0);
        return meses + (meses == 1 ? " mes" : " meses");
    }
}
