package uade.prog3.tpo.dto;

import java.util.List;

public record MochilaResponseDTO(
        String metodo,
        double capacidadBodega,
        double pesoOcupado,
        double valorTotalObtenido,
        List<MineralDTO> mineralesCargados,
        double[][] matrizDP,
        /** La columna c de la matriz equivale a una capacidad de c / factorEscala toneladas. */
        int factorEscala
) {
}
