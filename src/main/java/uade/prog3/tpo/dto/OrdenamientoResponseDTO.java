package uade.prog3.tpo.dto;

import java.util.List;

public record OrdenamientoResponseDTO(
        String algoritmoUtilizado,
        String criterio,
        List<MineralDTO> resultado
) {
}
