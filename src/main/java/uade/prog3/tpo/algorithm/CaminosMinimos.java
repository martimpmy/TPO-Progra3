package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * UNIDAD: Caminos Mínimos (Hito 5) - PUNTAJE: 1 punto (Dijkstra).
 *
 * Algoritmo voraz de Dijkstra para calcular el camino de menor consumo energético
 * (Celdas de Antimateria - CA) desde un nodo origen a un nodo destino en el grafo interestelar.
 *
 * REGLAS DE CÁTEDRA:
 * - Uso de Cola de Prioridad (PriorityQueue - Min-Heap binario) en lugar de búsqueda lineal
 *   para garantizar la complejidad O((V + E) log V).
 * - Vector de predecesores para reconstruir el camino completo (no solo el costo).
 * - Código Java puro y agnóstico a frameworks, sin consultas a base de datos.
 */
public class CaminosMinimos {

    /**
     * Nodo auxiliar en la cola de prioridad de Dijkstra.
     */
    public record NodoDistancia(int vertice, int distancia) implements Comparable<NodoDistancia> {
        @Override
        public int compareTo(NodoDistancia otro) {
            return Integer.compare(this.distancia, otro.distancia);
        }
    }

    /**
     * Resultado estructurado del camino mínimo con nombres, identificadores y costo.
     */
    public record ResultadoDijkstra(
            String origenId,
            String origenNombre,
            String destinoId,
            String destinoNombre,
            int consumoTotal,
            List<String> caminoIds,
            List<String> caminoNombres
    ) {
    }

    /**
     * Estructura compatible con el scaffold original de la cátedra.
     */
    public record Camino(List<String> vertices, double costoTotal) {
    }

    /**
     * Calcula la trayectoria óptima en consumo energético mediante Dijkstra con Min-Heap.
     *
     * @param grafo     Grafo espacial cargado en memoria.
     * @param origenId  Identificador alfanumérico del vértice origen (ej. "SOL").
     * @param destinoId Identificador alfanumérico del vértice destino (ej. "CITADEL").
     * @return Objeto ResultadoDijkstra con el camino reconstruido y consumo total.
     */
    public ResultadoDijkstra dijkstra(Grafo grafo, String origenId, String destinoId) {
        if (grafo == null) {
            throw new IllegalArgumentException("El grafo no puede ser nulo");
        }
        if (origenId == null || origenId.isBlank()) {
            throw new IllegalArgumentException("El identificador de origen es obligatorio");
        }
        if (destinoId == null || destinoId.isBlank()) {
            throw new IllegalArgumentException("El identificador de destino es obligatorio");
        }

        int uOrigen = grafo.indiceDe(origenId);
        if (uOrigen < 0) {
            throw new IllegalArgumentException("No existe el vértice de origen '" + origenId + "'");
        }
        int uDestino = grafo.indiceDe(destinoId);
        if (uDestino < 0) {
            throw new IllegalArgumentException("No existe el vértice de destino '" + destinoId + "'");
        }

        int cantidadVertices = grafo.cantidadVertices();
        int[] dist = new int[cantidadVertices];
        int[] predecesor = new int[cantidadVertices];
        boolean[] visitado = new boolean[cantidadVertices];

        for (int i = 0; i < cantidadVertices; i++) {
            dist[i] = Integer.MAX_VALUE;
            predecesor[i] = -1;
        }

        dist[uOrigen] = 0;
        PriorityQueue<NodoDistancia> colaPrioridad = new PriorityQueue<>();
        colaPrioridad.add(new NodoDistancia(uOrigen, 0));

        while (!colaPrioridad.isEmpty()) {
            NodoDistancia nodoActual = colaPrioridad.poll();
            int u = nodoActual.vertice();

            if (visitado[u]) {
                continue;
            }
            visitado[u] = true;

            // Parada temprana si ya expandimos el destino de forma óptima
            if (u == uDestino) {
                break;
            }

            for (Grafo.Arista arista : grafo.vecinos(u)) {
                int v = arista.destino();
                int peso = arista.peso();

                if (peso <= 0) {
                    throw new IllegalStateException(
                            "Dijkstra no admite aristas de peso menor o igual a cero: " + peso);
                }

                // Relajación de arista: dist[u] + peso < dist[v]
                if (!visitado[v] && dist[u] != Integer.MAX_VALUE && dist[u] + peso < dist[v]) {
                    dist[v] = dist[u] + peso;
                    predecesor[v] = u;
                    colaPrioridad.add(new NodoDistancia(v, dist[v]));
                }
            }
        }

        if (dist[uDestino] == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "No existe ruta transitable entre '" + origenId + "' y '" + destinoId + "'");
        }

        // Reconstrucción del camino desde el destino hacia el origen usando el vector de predecesores
        List<String> caminoIds = new ArrayList<>();
        List<String> caminoNombres = new ArrayList<>();
        int paso = uDestino;
        while (paso != -1) {
            caminoIds.add(grafo.idDe(paso));
            caminoNombres.add(grafo.nombreDe(paso));
            paso = predecesor[paso];
        }
        Collections.reverse(caminoIds);
        Collections.reverse(caminoNombres);

        return new ResultadoDijkstra(
                origenId,
                grafo.nombreDe(uOrigen),
                destinoId,
                grafo.nombreDe(uDestino),
                dist[uDestino],
                caminoIds,
                caminoNombres
        );
    }

    /**
     * Adaptador para compatibilidad estricta con el scaffold original del docente.
     */
    public Camino dijkstraScaffold(Grafo grafo, String origenId, String destinoId) {
        ResultadoDijkstra r = dijkstra(grafo, origenId, destinoId);
        return new Camino(r.caminoIds(), (double) r.consumoTotal());
    }
}
