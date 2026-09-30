package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uade.prog3.tpo.dto.ResumenGrafoDTO;
import uade.prog3.tpo.service.GrafoService;

@RestController
@RequestMapping("/api/grafo")
public class GrafoController {

    private final GrafoService grafoService;

    public GrafoController(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    /** Hito 1: cantidad de estaciones y rutas cargadas en Neo4j. */
    @GetMapping("/resumen")
    public ResumenGrafoDTO resumen() {
        return grafoService.resumen();
    }
}
