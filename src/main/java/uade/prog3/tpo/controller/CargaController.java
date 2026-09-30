package uade.prog3.tpo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uade.prog3.tpo.dto.GreedyResponseDTO;
import uade.prog3.tpo.dto.MineralDTO;
import uade.prog3.tpo.service.MineralService;

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

    public record CargaGreedyRequest(
            double capacidadBodega,
            List<MineralDTO> itemsDisponibles
    ) {
    }
}
