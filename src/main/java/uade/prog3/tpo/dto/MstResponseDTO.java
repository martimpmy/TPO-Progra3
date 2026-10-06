package uade.prog3.tpo.dto;

import java.util.List;

public record MstResponseDTO(
        String algoritmo,
        int costoTotalMST,
        String unidad,
        List<AristaMstDTO> aristasSeleccionadas
) {
    public record AristaMstDTO(
            String origen,
            String destino,
            int costo
    ) {
    }
}
