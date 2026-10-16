package uade.prog3.tpo.dto;

import java.util.List;

public record RepartoResponseDTO(
        String algoritmo,
        String origen,
        List<String> estacionesRequeridas,
        List<String> recorridoOptimo,
        List<String> recorridoCompleto,
        List<TramoDTO> tramos,
        int costoMinimoTotal,
        String unidad,
        String cotaUtilizada,
        MetricasDTO metricas
) {

    /** Un tramo entre dos paradas consecutivas, con las estaciones intermedias del camino mínimo. */
    public record TramoDTO(String desde, String hasta, int costo, List<String> camino) {
    }

    public record MetricasDTO(
            int cotaInferiorRaiz,
            int cotaSuperiorInicial,
            long nodosGenerados,
            long nodosExpandidos,
            long nodosArbolPodados,
            long nodosArbolCompleto,
            String podaEfectiva
    ) {
    }
}
