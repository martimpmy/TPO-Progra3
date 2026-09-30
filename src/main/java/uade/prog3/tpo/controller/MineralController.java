package uade.prog3.tpo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uade.prog3.tpo.dto.MineralDTO;
import uade.prog3.tpo.dto.OrdenamientoResponseDTO;
import uade.prog3.tpo.service.MineralService;

/**
 * Minerales de la bodega: alta, consulta y baja en Neo4j, y ordenamiento propio (Hito 2).
 * Sin logica algoritmica: solo ruteo y delegacion en MineralService.
 */
@RestController
@RequestMapping("/api/minerales")
public class MineralController {

    private final MineralService mineralService;

    public MineralController(MineralService mineralService) {
        this.mineralService = mineralService;
    }

    /** Lista los minerales persistidos en Neo4j. */
    @GetMapping
    public List<MineralDTO> listar() {
        return mineralService.listar();
    }

    /** Persiste uno o mas minerales: [{ "nombre": ..., "peso": ..., "valor": ... }]. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<MineralDTO> crear(@RequestBody List<MineralDTO> minerales) {
        return mineralService.crear(minerales);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        mineralService.eliminar(id);
    }

    /**
     * Hito 2: POST /api/minerales/ordenar?algoritmo=quicksort&criterio=ratio&direccion=desc
     * Con body ordena esa lista; sin body ordena los minerales persistidos.
     */
    @PostMapping("/ordenar")
    public OrdenamientoResponseDTO ordenar(
            @RequestParam(defaultValue = "quicksort") String algoritmo,
            @RequestParam(defaultValue = "ratio") String criterio,
            @RequestParam(defaultValue = "desc") String direccion,
            @RequestBody(required = false) List<MineralDTO> minerales
    ) {
        return mineralService.ordenar(minerales, algoritmo, criterio, direccion);
    }
}
