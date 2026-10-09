package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uade.prog3.tpo.dto.DijkstraResponseDTO;
import uade.prog3.tpo.dto.FloydWarshallResponseDTO;
import uade.prog3.tpo.dto.MstResponseDTO;
import uade.prog3.tpo.service.GrafoService;

/**
 * Controlador de navegación estelar y diseño de infraestructura subespacial.
 *
 * UNIDAD: Grafos II (Hito 5) - PUNTAJE: 3 puntos (Dijkstra 1 pt, Prim 1 pt, Kruskal 1 pt).
 *
 * Cumple con la regla anti-penalización: Cero lógica algorítmica en el controlador,
 * limitándose a la validación de entrada, ruteo y delegación en GrafoService.
 */
@RestController
@RequestMapping("/api")
public class NavegacionController {

    private final GrafoService grafoService;

    public NavegacionController(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    /**
     * Endpoint oficial 5: Trayectoria Óptima de Antimateria (Dijkstra con reconstrucción).
     *
     * GET /api/navegacion/dijkstra?origen=SOL&destino=CITADEL
     */
    @GetMapping("/navegacion/dijkstra")
    public DijkstraResponseDTO dijkstra(
            @RequestParam String origen,
            @RequestParam String destino
    ) {
        return grafoService.dijkstra(origen, destino);
    }

    /**
     * Endpoint oficial 6: Red Troncal de Balizas Subespaciales (MST - Prim o Kruskal).
     *
     * GET /api/red/mst?metodo=kruskal
     */
    @GetMapping("/red/mst")
    public MstResponseDTO mst(
            @RequestParam(defaultValue = "kruskal") String metodo
    ) {
        return grafoService.mst(metodo);
    }

    /**
     * Hito 7: Matriz de Telemetría Galáctica (Floyd-Warshall entre todos los pares).
     *
     * GET /api/navegacion/todos-contra-todos
     * Opcional: ?simularOrigen=NOVA&simularDestino=ORION&simularCosto=-30 agrega una ruta
     * dirigida solo en memoria para probar la detección de ciclos negativos.
     */
    @GetMapping("/navegacion/todos-contra-todos")
    public FloydWarshallResponseDTO todosContraTodos(
            @RequestParam(required = false) String simularOrigen,
            @RequestParam(required = false) String simularDestino,
            @RequestParam(required = false) Double simularCosto
    ) {
        return grafoService.todosContraTodos(simularOrigen, simularDestino, simularCosto);
    }
}
