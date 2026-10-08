package tech.veterinaria_api.seguimiento;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.veterinaria_api.consultas.ConsultaRepository;
import tech.veterinaria_api.seguimiento.SeguimientoItem.TipoSeguimiento;
import tech.veterinaria_api.vacunas.VacunaRepository;

@Service
public class SeguimientoService {

    private final ConsultaRepository consultaRepository;
    private final VacunaRepository vacunaRepository;
    private final ZoneId zona;

    public SeguimientoService(ConsultaRepository consultaRepository, VacunaRepository vacunaRepository,
            @Value("${app.zona-horaria:America/Bogota}") String zona) {
        this.consultaRepository = consultaRepository;
        this.vacunaRepository = vacunaRepository;
        this.zona = ZoneId.of(zona);
    }

    /** Controles y refuerzos entre hoy y los próximos {@code dias} días, sin atender aún. */
    @Transactional(readOnly = true)
    public List<SeguimientoItem> pendientes(int dias) {
        LocalDate hoy = LocalDate.now(zona);
        LocalDate hasta = hoy.plusDays(dias);
        List<SeguimientoItem> items = new ArrayList<>();
        consultaRepository.controlesPendientes(hoy, hasta).forEach(c -> items.add(new SeguimientoItem(
                TipoSeguimiento.CONTROL, c.getProximoControl(), c.getMascotaId(), c.getPropietarioId(), c.getId(),
                "Control: " + c.getMotivo())));
        vacunaRepository.refuerzosPendientes(hoy, hasta).forEach(v -> items.add(new SeguimientoItem(
                TipoSeguimiento.REFUERZO_VACUNA, v.getProximaDosis(), v.getMascotaId(), v.getPropietarioId(),
                v.getId(), "Refuerzo de vacuna: " + v.getNombre())));
        items.sort(Comparator.comparing(SeguimientoItem::fecha));
        return items;
    }
}
