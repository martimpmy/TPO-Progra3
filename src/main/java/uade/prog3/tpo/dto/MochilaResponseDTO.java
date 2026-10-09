package uade.prog3.tpo.dto;

import java.util.List;

public record MochilaResponseDTO(
        String metodo,
        double capacidadBodega,
        double pesoOcupado,
        double valorTotalObtenido,
        List<MineralDTO> mineralesCargados,
        double[][] matrizDP
) {
}
