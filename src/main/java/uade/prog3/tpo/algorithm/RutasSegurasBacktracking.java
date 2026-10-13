package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * UNIDAD: Búsqueda con Retroceso / Backtracking con Poda (Hito 8).
 * PUNTAJE: 1 punto.
 *
 * Encuentra todas las rutas simples (sin ciclos) entre una estación origen y una destino,
 * acotadas simultáneamente por dos restricciones:
 * 1. Consumo total en Celdas de Antimateria (CA) <= maxCosto.
 * 2. Cantidad de saltos hiperespaciales (tramos transitados) <= maxSaltos.
 *
 * CRITERIO DE PODA (Pruning):
 * - Los costos de las aristas son estrictamente positivos (costoCA > 0) y cada salto suma 1.
 * - Ambas funciones de acumulación son monótonamente crecientes.
 * - Si costoActual + pesoArista > maxCosto o saltosActuales + 1 > maxSaltos, la rama se corta
 *   inmediatamente sin descender ni explorar sus hijos.
 *
 * MÉTRICA CUANTITATIVA:
 * Se contabilizan los nodos explorados en el árbol de recursión con poda vs. sin poda para
 * reportar la efectividad real de la reducción del espacio de estados.
 */
public class RutasSegurasBacktracking {

    public record RutaValida(
            List<String> caminoNombres,
            List<String> caminoIds,
            int costoTotal,
            int saltos
    ) {
    }

    public record MetricasPoda(
            int nodosExploradosConPoda,
            int nodosExploradosSinPoda,
            int ramasPodadas,
            String podaEfectiva,
            double porcentajePoda
    ) {
    }

    public record ResultadoBacktracking(
            String origenId,
            String origenNombre,
            String destinoId,
            String destinoNombre,
            int maxCosto,
            int maxSaltos,
            List<RutaValida> rutasValidas,
            MetricasPoda metricas
    ) {
    }

    private int contadorConPoda;
    private int contadorSinPoda;
    private int ramasPodadas;

    /**
     * Ejecuta la búsqueda de rutas seguras por Backtracking con poda y contrasta con la versión sin poda.
     *
     * @param grafo     Grafo espacial cargado en memoria.
     * @param origenId  ID de la estación origen (ej. "SOL").
     * @param destinoId ID de la estación destino (ej. "CITADEL").
     * @param maxCosto  Cota superior de consumo energético en CA (>= 0).
     * @param maxSaltos Cota superior de cantidad de saltos hiperespaciales (>= 0).
     * @return ResultadoBacktracking con las rutas encontradas y las métricas de poda.
     */
    public ResultadoBacktracking buscarRutas(Grafo grafo, String origenId, String destinoId, int maxCosto, int maxSaltos) {
        if (grafo == null) {
            throw new IllegalArgumentException("El grafo no puede ser nulo");
        }
        if (origenId == null || origenId.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro 'origen'");
        }
        if (destinoId == null || destinoId.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro 'destino'");
        }
        if (!grafo.contiene(origenId)) {
            throw new IllegalArgumentException("No existe la estación con id '" + origenId + "'");
        }
        if (!grafo.contiene(destinoId)) {
            throw new IllegalArgumentException("No existe la estación con id '" + destinoId + "'");
        }
        if (maxCosto < 0) {
            throw new IllegalArgumentException("El costo máximo no puede ser negativo: " + maxCosto);
        }
        if (maxSaltos < 0) {
            throw new IllegalArgumentException("La cantidad máxima de saltos no puede ser negativa: " + maxSaltos);
        }

        int origenIdx = grafo.indiceDe(origenId);
        int destinoIdx = grafo.indiceDe(destinoId);
        int v = grafo.cantidadVertices();

        // 1. Exploración CON poda
        contadorConPoda = 1; // raíz (nodo origen)
        ramasPodadas = 0;
        List<RutaValida> rutas = new ArrayList<>();
        boolean[] visitadoConPoda = new boolean[v];
        List<Integer> caminoConPoda = new ArrayList<>();

        visitadoConPoda[origenIdx] = true;
        caminoConPoda.add(origenIdx);

        if (origenIdx == destinoIdx) {
            rutas.add(crearRuta(grafo, caminoConPoda, 0, 0));
        } else {
            backtrackConPoda(grafo, origenIdx, destinoIdx, 0, 0, maxCosto, maxSaltos, visitadoConPoda, caminoConPoda, rutas);
        }

        // Ordenamos las rutas encontradas: primero por menor costo, luego por menor cantidad de saltos
        rutas.sort(Comparator.comparingInt(RutaValida::costoTotal).thenComparingInt(RutaValida::saltos));

        // 2. Exploración SIN poda (árbol completo de caminos simples)
        contadorSinPoda = 1;
        boolean[] visitadoSinPoda = new boolean[v];
        visitadoSinPoda[origenIdx] = true;

        if (origenIdx != destinoIdx) {
            backtrackSinPoda(grafo, origenIdx, destinoIdx, visitadoSinPoda);
        }

        // Cálculo de efectividad de la poda
        double porcentaje = contadorSinPoda > 0
                ? Math.max(0.0, ((double) (contadorSinPoda - contadorConPoda) / contadorSinPoda) * 100.0)
                : 0.0;
        String podaEfectivaStr = String.format(Locale.US, "%.1f%%", porcentaje);

        MetricasPoda metricas = new MetricasPoda(
                contadorConPoda,
                contadorSinPoda,
                ramasPodadas,
                podaEfectivaStr,
                porcentaje
        );

        return new ResultadoBacktracking(
                origenId,
                grafo.nombreDe(origenIdx),
                destinoId,
                grafo.nombreDe(destinoIdx),
                maxCosto,
                maxSaltos,
                Collections.unmodifiableList(rutas),
                metricas
        );
    }

    private void backtrackConPoda(
            Grafo grafo,
            int u,
            int destino,
            int costoActual,
            int saltosActuales,
            int maxCosto,
            int maxSaltos,
            boolean[] visitado,
            List<Integer> camino,
            List<RutaValida> rutas
    ) {
        if (u == destino) {
            rutas.add(crearRuta(grafo, camino, costoActual, saltosActuales));
            return;
        }

        for (Grafo.Arista arista : grafo.vecinos(u)) {
            int vecino = arista.destino();
            if (!visitado[vecino]) {
                int nuevoCosto = costoActual + arista.peso();
                int nuevosSaltos = saltosActuales + 1;

                // Doble Cota de Poda (Consumo y Saltos)
                if (nuevoCosto > maxCosto || nuevosSaltos > maxSaltos) {
                    ramasPodadas++;
                    continue; // Poda: no se desciende por este subárbol
                }

                contadorConPoda++;
                visitado[vecino] = true;
                camino.add(vecino);

                backtrackConPoda(grafo, vecino, destino, nuevoCosto, nuevosSaltos, maxCosto, maxSaltos, visitado, camino, rutas);

                camino.remove(camino.size() - 1);
                visitado[vecino] = false;
            }
        }
    }

    private void backtrackSinPoda(
            Grafo grafo,
            int u,
            int destino,
            boolean[] visitado
    ) {
        if (u == destino) {
            return;
        }

        for (Grafo.Arista arista : grafo.vecinos(u)) {
            int vecino = arista.destino();
            if (!visitado[vecino]) {
                contadorSinPoda++;
                visitado[vecino] = true;

                backtrackSinPoda(grafo, vecino, destino, visitado);

                visitado[vecino] = false;
            }
        }
    }

    private RutaValida crearRuta(Grafo grafo, List<Integer> indices, int costo, int saltos) {
        List<String> nombres = new ArrayList<>(indices.size());
        List<String> ids = new ArrayList<>(indices.size());
        for (int idx : indices) {
            ids.add(grafo.idDe(idx));
            nombres.add(grafo.nombreDe(idx));
        }
        return new RutaValida(nombres, ids, costo, saltos);
    }
}
