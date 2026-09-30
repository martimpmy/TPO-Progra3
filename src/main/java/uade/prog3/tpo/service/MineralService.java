package uade.prog3.tpo.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uade.prog3.tpo.algorithm.GreedyCarga;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.dto.GreedyResponseDTO;
import uade.prog3.tpo.dto.MineralDTO;
import uade.prog3.tpo.dto.OrdenamientoResponseDTO;
import uade.prog3.tpo.exception.MineralNoEncontradoException;
import uade.prog3.tpo.model.Mineral;
import uade.prog3.tpo.repository.MineralRepository;


@Service
public class MineralService {

    private final MineralRepository mineralRepository;
    private final Ordenamiento ordenamiento = new Ordenamiento();
    private final GreedyCarga greedyCarga = new GreedyCarga();

    public MineralService(MineralRepository mineralRepository) {
        this.mineralRepository = mineralRepository;
    }

    @Transactional(readOnly = true)
    public List<MineralDTO> listar() {
        return mineralRepository.findAll().stream().map(MineralDTO::fromModel).toList();
    }

    /** Valida y persiste los minerales recibidos. Si uno es invalido no se guarda ninguno. */
    @Transactional
    public List<MineralDTO> crear(List<MineralDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new IllegalArgumentException("Se debe enviar al menos un mineral");
        }
        List<Mineral> nuevos = aModelos(dtos);
        return mineralRepository.saveAll(nuevos).stream().map(MineralDTO::fromModel).toList();
    }

    @Transactional
    public void eliminar(String id) {
        if (!mineralRepository.existsById(id)) {
            throw new MineralNoEncontradoException(id);
        }
        mineralRepository.deleteById(id);
    }

    /**
     * Ordena los minerales del body; si no se envia body, ordena los persistidos en Neo4j.
     * Los minerales se leen una sola vez y el algoritmo trabaja sobre la lista en memoria.
     */
    @Transactional(readOnly = true)
    public OrdenamientoResponseDTO ordenar(List<MineralDTO> dtos, String algoritmo, String criterio, String direccion) {
        Comparator<Mineral> comparadorBase = obtenerComparador(criterio);
        Comparator<Mineral> comparador = resolverDireccion(direccion) ? comparadorBase.reversed() : comparadorBase;

        List<Mineral> minerales = dtos == null ? mineralRepository.findAll() : aModelos(dtos);

        String algoNormalizado = algoritmo != null ? algoritmo.trim().toLowerCase() : "quicksort";
        List<Mineral> ordenados;
        String nombreAlgoritmo;

        switch (algoNormalizado) {
            case "quicksort" -> {
                ordenados = ordenamiento.quickSort(minerales, comparador);
                nombreAlgoritmo = "QuickSort (Propio)";
            }
            case "mergesort" -> {
                ordenados = ordenamiento.mergeSort(minerales, comparador);
                nombreAlgoritmo = "MergeSort (Propio)";
            }
            default -> throw new IllegalArgumentException(
                    "Algoritmo desconocido: '" + algoritmo + "'. Opciones válidas: 'quicksort', 'mergesort'.");
        }

        return new OrdenamientoResponseDTO(
                nombreAlgoritmo,
                criterio != null ? criterio.trim().toLowerCase() : "ratio",
                ordenados.stream().map(MineralDTO::fromModel).toList()
        );
    }

    private List<Mineral> aModelos(List<MineralDTO> dtos) {
        return dtos.stream().map(dto -> {
            if (dto == null) {
                throw new IllegalArgumentException("La lista de minerales no puede contener elementos nulos");
            }
            return dto.toModel();
        }).toList();
    }

    private boolean resolverDireccion(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return true; // Default descendente para rentabilidad / valor
        }
        String dir = direccion.trim().toLowerCase();
        if (dir.equals("desc") || dir.equals("descendente")) {
            return true;
        }
        if (dir.equals("asc") || dir.equals("ascendente")) {
            return false;
        }
        throw new IllegalArgumentException(
                "Dirección inválida: '" + direccion + "'. Opciones válidas: 'desc', 'asc'.");
    }

    @Transactional(readOnly = true)
public GreedyResponseDTO cargarGreedy(
        List<MineralDTO> dtos,
        double capacidad
) {

    if (capacidad < 0) {
        throw new IllegalArgumentException(
                "La capacidad de la bodega no puede ser negativa"
        );
    }

    if (dtos == null || dtos.isEmpty()) {
        throw new IllegalArgumentException(
                "Se debe enviar al menos un mineral"
        );
    }

    List<Mineral> minerales = aModelos(dtos);

    GreedyCarga.ResultadoGreedy resultado =
            greedyCarga.cargar(minerales, capacidad);

    return new GreedyResponseDTO(
            "Greedy (Selección por Ratio Valor/Peso)",
            capacidad,
            resultado.pesoOcupado(),
            resultado.valorTotal(),
            resultado.minerales()
                    .stream()
                    .map(MineralDTO::fromModel)
                    .toList()
    );
}

    private Comparator<Mineral> obtenerComparador(String criterio) {
        String crit = criterio != null ? criterio.trim().toLowerCase() : "ratio";
        return switch (crit) {
            case "valor" -> Comparator.comparingDouble(Mineral::getValor);
            case "peso" -> Comparator.comparingDouble(Mineral::getPeso);
            case "ratio" -> Comparator.comparingDouble(Mineral::getRatio);
            default -> throw new IllegalArgumentException(
                    "Criterio inválido: '" + criterio + "'. Opciones válidas: 'valor', 'peso', 'ratio'.");
        };
    }
}
