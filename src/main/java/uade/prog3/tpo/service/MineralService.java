package uade.prog3.tpo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.dto.MineralDTO;
import uade.prog3.tpo.dto.OrdenamientoResponseDTO;
import uade.prog3.tpo.model.Mineral;

@Service
public class MineralService {

    private final Ordenamiento ordenamiento;

    public MineralService() {
        this.ordenamiento = new Ordenamiento();
    }

    public MineralService(Ordenamiento ordenamiento) {
        this.ordenamiento = ordenamiento != null ? ordenamiento : new Ordenamiento();
    }

    /**
     * Catálogo canónico de minerales del dominio "Odisea Galáctica".
     * Pesos y valores enteros positivos (Z+).
     */
    public List<Mineral> obtenerMineralesPorDefecto() {
        return List.of(
                new Mineral("Cristal de Taquiones", 6, 66),
                new Mineral("Núcleo de Plasma", 5, 50),
                new Mineral("Aleación de Titanio", 5, 50),
                new Mineral("Fragmento de Antimateria", 2, 30),
                new Mineral("Lingote de Iridio", 4, 44),
                new Mineral("Celdas de Helio-3", 3, 27)
        );
    }

    /**
     * Ordena una lista de minerales según el algoritmo y criterio especificado.
     * Solo si dtos es null (body ausente) se usa el catálogo por defecto.
     * Si dtos es una lista vacía ([]), se procesa y devuelve vacía.
     */
    public OrdenamientoResponseDTO ordenar(List<MineralDTO> dtos, String algoritmo, String criterio, String direccion) {
        List<Mineral> minerales;
        if (dtos == null) {
            minerales = new ArrayList<>(obtenerMineralesPorDefecto());
        } else {
            minerales = dtos.stream().map(MineralDTO::toModel).toList();
        }

        Comparator<Mineral> comparadorBase = obtenerComparador(criterio);
        boolean descendente = resolverDireccion(direccion);
        Comparator<Mineral> comparadorFinal = descendente ? comparadorBase.reversed() : comparadorBase;

        String algoNormalizado = algoritmo != null ? algoritmo.trim().toLowerCase() : "quicksort";
        List<Mineral> ordenados;
        String nombreAlgoritmo;

        switch (algoNormalizado) {
            case "quicksort" -> {
                ordenados = ordenamiento.quickSort(minerales, comparadorFinal);
                nombreAlgoritmo = "QuickSort (Propio)";
            }
            case "mergesort" -> {
                ordenados = ordenamiento.mergeSort(minerales, comparadorFinal);
                nombreAlgoritmo = "MergeSort (Propio)";
            }
            default -> throw new IllegalArgumentException(
                    "Algoritmo desconocido: '" + algoritmo + "'. Opciones válidas: 'quicksort', 'mergesort'.");
        }

        List<MineralDTO> resultadoDTO = ordenados.stream()
                .map(MineralDTO::fromModel)
                .toList();

        return new OrdenamientoResponseDTO(
                nombreAlgoritmo,
                criterio != null ? criterio.toLowerCase() : "ratio",
                resultadoDTO
        );
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
