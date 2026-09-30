package uade.prog3.tpo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uade.prog3.tpo.dto.GreedyResponseDTO;
import uade.prog3.tpo.dto.MineralDTO;
import uade.prog3.tpo.service.MineralService;

/**
 * Carga de la bodega de la nave (Hito 3: Greedy).
 * Sin lógica algorítmica: solo ruteo y delegación en MineralService.
 */
@RestController
@RequestMapping("/api/bodega")
public class CargaController {

    private final MineralService mineralService;

    public CargaController(MineralService mineralService) {
        this.mineralService = mineralService;
    }

    @PostMapping("/cargar-greedy")
    public GreedyResponseDTO cargarGreedy(
            @RequestBody CargaGreedyRequest request
    ) {
        return mineralService.cargarGreedy(
                request.itemsDisponibles(),
                request.capacidadBodega()
        );
    }

    /** capacidadBodega es Double para detectar si falta en el JSON (con double valdría 0). */
    public record CargaGreedyRequest(
            Double capacidadBodega,
            List<MineralDTO> itemsDisponibles
    ) {
    }
}
