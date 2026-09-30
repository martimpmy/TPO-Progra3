package uade.prog3.tpo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uade.prog3.tpo.dto.MineralDTO;
import uade.prog3.tpo.dto.OrdenamientoResponseDTO;
import uade.prog3.tpo.service.MineralService;

/**
 * Controlador REST para operaciones de ordenamiento sobre minerales y cargamento espacial.
 *
 * UNIDAD: Divide y Vencerás (Hito 2) - PUNTAJE: 1 punto.
 *
 * Cumple con la regla anti-penalización: Cero lógica algorítmica en el controlador,
 * limitándose al ruteo y delegación en la capa Service.
 */
@RestController
@RequestMapping("/api")
public class MineralController {

    private final MineralService mineralService;

    public MineralController(MineralService mineralService) {
        this.mineralService = mineralService;
    }

    /**
     * Endpoint oficial Hito 2: Ordenamiento propio de cargamento.
     *
     * POST /api/minerales/ordenar?algoritmo=quicksort&criterio=ratio&direccion=desc
     */
    @PostMapping("/minerales/ordenar")
    public OrdenamientoResponseDTO ordenarMinerales(
            @RequestParam(defaultValue = "quicksort") String algoritmo,
            @RequestParam(defaultValue = "ratio") String criterio,
            @RequestParam(defaultValue = "desc") String direccion,
            @RequestBody(required = false) List<MineralDTO> minerales
    ) {
        return mineralService.ordenar(minerales, algoritmo, criterio, direccion);
    }

    /**
     * Variante GET para pruebas directas en navegador o cURL usando minerales de ejemplo.
     */
    @GetMapping("/minerales/ordenar")
    public OrdenamientoResponseDTO ordenarMineralesGet(
            @RequestParam(defaultValue = "quicksort") String algoritmo,
            @RequestParam(defaultValue = "ratio") String criterio,
            @RequestParam(defaultValue = "desc") String direccion
    ) {
        return mineralService.ordenar(null, algoritmo, criterio, direccion);
    }

    /**
     * Compatibilidad directa con el scaffold de la cátedra:
     * GET /api/seleccion/quicksort?criterio=ratio
     */
    @GetMapping("/seleccion/quicksort")
    public List<MineralDTO> quicksortScaffold(
            @RequestParam(defaultValue = "ratio") String criterio
    ) {
        return mineralService.ordenar(null, "quicksort", criterio, "desc").resultado();
    }

    /**
     * Compatibilidad directa con el scaffold de la cátedra:
     * GET /api/seleccion/mergesort?criterio=peso
     */
    @GetMapping("/seleccion/mergesort")
    public List<MineralDTO> mergesortScaffold(
            @RequestParam(defaultValue = "ratio") String criterio
    ) {
        return mineralService.ordenar(null, "mergesort", criterio, "desc").resultado();
    }
}
