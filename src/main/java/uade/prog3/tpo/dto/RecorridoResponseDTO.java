package uade.prog3.tpo.dto;

import java.util.List;

public record RecorridoResponseDTO(
        String nodoInicial,
        String recorrido,
        List<String> ordenExploracion
) {
}
