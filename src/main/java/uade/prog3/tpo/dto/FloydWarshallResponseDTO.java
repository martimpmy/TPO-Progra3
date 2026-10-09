package uade.prog3.tpo.dto;

import java.util.List;

/**
 * Hito 7: distancias mínimas entre todos los pares de estaciones.
 *
 * @param estaciones                orden de las filas y columnas de la matriz.
 * @param distancias                distancias[i][j] en CA; null en una celda si no hay camino.
 *                                  La matriz completa es null cuando hay ciclo negativo.
 * @param estacionesEnCicloNegativo ids de las estaciones con D[i][i] menor a 0.
 * @param rutaSimulada              ruta agregada solo en memoria para esta consulta, o null.
 */
public record FloydWarshallResponseDTO(
        String algoritmo,
        String unidad,
        List<GrafoDTO.EstacionDTO> estaciones,
        Double[][] distancias,
        boolean cicloNegativo,
        List<String> estacionesEnCicloNegativo,
        String alerta,
        RutaSimuladaDTO rutaSimulada,
        ComparativaDTO comparativa
) {

    /** Ruta dirigida origen -> destino; el costo puede ser negativo. */
    public record RutaSimuladaDTO(String origen, String destino, double costoCA) {
    }

    /**
     * Estados expandidos por cada estrategia sobre el grafo persistido (sin la ruta simulada).
     *
     * @param estadosFloydWarshall      ternas (k, i, j) evaluadas: V^3.
     * @param estadosDijkstraVVeces     nodosExpandidosDijkstra + aristasExaminadasDijkstra.
     * @param nodosExpandidosDijkstra   vértices extraídos del heap sumando las V corridas.
     * @param aristasExaminadasDijkstra aristas recorridas sumando las V corridas.
     */
    public record ComparativaDTO(
            int vertices,
            int aristas,
            long estadosFloydWarshall,
            long estadosDijkstraVVeces,
            long nodosExpandidosDijkstra,
            long aristasExaminadasDijkstra
    ) {
    }
}
