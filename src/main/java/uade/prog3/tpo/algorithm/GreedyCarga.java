package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import uade.prog3.tpo.model.Mineral;

public class GreedyCarga {

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

        List<Mineral> ordenados =
                ordenamiento.quickSort(minerales, comparadorPorRatio);

        List<Mineral> seleccionados = new ArrayList<>();

        double pesoActual = 0;
        double valorTotal = 0;

        for (Mineral mineral : ordenados) {

            if (pesoActual + mineral.getPeso() <= capacidad) {

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