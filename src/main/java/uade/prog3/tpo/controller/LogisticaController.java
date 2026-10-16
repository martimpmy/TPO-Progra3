package uade.prog3.tpo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uade.prog3.tpo.dto.RepartoResponseDTO;
import uade.prog3.tpo.service.LogisticaService;

/**
 * Logística de despacho (Hito 11: Branch & Bound).
 * Sin lógica algorítmica: solo ruteo y delegación en LogisticaService.
 */
@RestController
@RequestMapping("/api/logistica")
public class LogisticaController {

    private final LogisticaService logisticaService;

    public LogisticaController(LogisticaService logisticaService) {
        this.logisticaService = logisticaService;
    }

    /**
     * POST /api/logistica/reparto-optimo?origen=SOL
     * Cuerpo: ids de las estaciones a aprovisionar, por ejemplo ["ALPHA", "VEGA", "KEPLER"].
     */
    @PostMapping("/reparto-optimo")
    public RepartoResponseDTO repartoOptimo(
            @RequestParam(defaultValue = "SOL") String origen,
            @RequestBody List<String> estacionesRequeridas
    ) {
        return logisticaService.repartoOptimo(origen, estacionesRequeridas);
    }
}
