package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uade.prog3.tpo.dto.GrafoDTO;
import uade.prog3.tpo.dto.RecorridoResponseDTO;
import uade.prog3.tpo.dto.ResumenGrafoDTO;
import uade.prog3.tpo.service.GrafoService;

@RestController
@RequestMapping("/api/grafo")
public class GrafoController {

    private final GrafoService grafoService;

    public GrafoController(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    /** Estaciones y rutas del grafo, para dibujar el mapa en la vista. */
    @GetMapping
    public GrafoDTO grafo() {
        return grafoService.grafo();
    }

    /** Hito 1: cantidad de estaciones y rutas cargadas en Neo4j. */
    @GetMapping("/resumen")
    public ResumenGrafoDTO resumen() {
        return grafoService.resumen();
    }

    /** Hito 4: GET /api/grafo/recorrer?origen=SOL&tipo=BFS (tipo: BFS o DFS). */
    @GetMapping("/recorrer")
    public RecorridoResponseDTO recorrer(
            @RequestParam String origen,
            @RequestParam(defaultValue = "BFS") String tipo
    ) {
        return grafoService.recorrer(origen, tipo);
    }

    /** Compatibilidad scaffold: GET /api/grafo/dijkstra?origen=SOL&destino=CITADEL */
    @GetMapping("/dijkstra")
    public uade.prog3.tpo.dto.DijkstraResponseDTO dijkstra(
            @RequestParam String origen,
            @RequestParam String destino
    ) {
        return grafoService.dijkstra(origen, destino);
    }

    /** Compatibilidad scaffold: GET /api/grafo/mst?metodo=prim|kruskal */
    @GetMapping("/mst")
    public uade.prog3.tpo.dto.MstResponseDTO mst(
            @RequestParam(defaultValue = "kruskal") String metodo
    ) {
        return grafoService.mst(metodo);
    }

    /** Compatibilidad scaffold: GET /api/grafo/prim?origen=SOL */
    @GetMapping("/prim")
    public uade.prog3.tpo.dto.MstResponseDTO prim(
            @RequestParam(required = false) String origen
    ) {
        return grafoService.mst("prim");
    }

    /** Compatibilidad scaffold: GET /api/grafo/kruskal */
    @GetMapping("/kruskal")
    public uade.prog3.tpo.dto.MstResponseDTO kruskal() {
        return grafoService.mst("kruskal");
    }
}
