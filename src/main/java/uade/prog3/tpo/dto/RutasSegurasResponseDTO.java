package uade.prog3.tpo.dto;

import java.util.List;

/**
 * DTO de respuesta para el Hito 8: Backtracking de Rutas Seguras con doble restricción.
 */
public record RutasSegurasResponseDTO(
        String origen,
        String destino,
        int maxCosto,
        int maxSaltos,
        List<RutaValidaDTO> rutasValidas,
        MetricasExploracionDTO metricasExploracion
) {
    public record RutaValidaDTO(
            List<String> camino,
            List<String> caminoIds,
            int costoTotal,
            int saltos
    ) {
    }

    public record MetricasExploracionDTO(
            int nodosExploradosConPoda,
            int nodosExploradosSinPoda,
            int ramasPodadas,
            String podaEfectiva,
            double porcentajePoda
    ) {
    }
}
