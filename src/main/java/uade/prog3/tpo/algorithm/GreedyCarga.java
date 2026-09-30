package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import uade.prog3.tpo.model.Mineral;

/**
 * UNIDAD: Algoritmos Voraces (Hito 3) - Carga rápida de bahía.
 *
 * - Candidatos: los N minerales disponibles.
 * - Selección: mayor ratio valor/peso primero.
 * - Factibilidad: pesoActual + peso <= capacidad.
 * - Objetivo: maximizar el valor cargado.
 *
 * NO es óptimo para Mochila 0/1: con capacidad 10 y minerales (6t, 66), (5t, 50), (5t, 50)
 * elige el de 6t (ratio 11) y obtiene 66, cuando el óptimo es 100 (los dos de 5t).
 *
 * Complejidad temporal: O(N log N), dominada por el ordenamiento (MergeSort propio).
 * Complejidad espacial auxiliar: O(N), por la copia ordenada y la lista de seleccionados.
 */
public class GreedyCarga {

    /** Tolerancia para comparar sumas de pesos decimales (en double, 0.1 + 0.2 > 0.3). */
    private static final double EPSILON = 1e-9;

    private final Ordenamiento ordenamiento = new Ordenamiento();

    public ResultadoGreedy cargar(List<Mineral> minerales, double capacidad) {

        if (minerales == null) {
            return new ResultadoGreedy(new ArrayList<>(), 0, 0);
        }

        if (capacidad < 0) {
            throw new IllegalArgumentException(
                    "La capacidad de la bodega no puede ser negativa"
            );
        }

        Comparator<Mineral> comparadorPorRatio =
                Comparator.comparingDouble(Mineral::getRatio).reversed();

        // MergeSort es estable: ante ratios iguales respeta el orden de entrada,
        // así la misma lista produce siempre la misma carga.
        List<Mineral> ordenados =
                ordenamiento.mergeSort(minerales, comparadorPorRatio);

        List<Mineral> seleccionados = new ArrayList<>();

        double pesoActual = 0;
        double valorTotal = 0;

        for (Mineral mineral : ordenados) {

            if (pesoActual + mineral.getPeso() <= capacidad + EPSILON) {

                seleccionados.add(mineral);

                pesoActual += mineral.getPeso();
                valorTotal += mineral.getValor();
            }
        }

        return new ResultadoGreedy(
                seleccionados,
                pesoActual,
                valorTotal
        );
    }

    public record ResultadoGreedy(
            List<Mineral> minerales,
            double pesoOcupado,
            double valorTotal
    ) {
    }
}