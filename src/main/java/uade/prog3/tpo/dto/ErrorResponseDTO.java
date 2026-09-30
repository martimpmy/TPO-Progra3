package uade.prog3.tpo.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(LocalDateTime fecha, int codigo, String error, String mensaje) {
}
