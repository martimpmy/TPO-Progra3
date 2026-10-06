package uade.prog3.tpo.dto;

import java.util.List;

public record DijkstraResponseDTO(
        String origen,
        String destino,
        int consumoTotal,
        String unidad,
        List<String> caminoReconstruido,
        List<String> caminoIds
) {
}
