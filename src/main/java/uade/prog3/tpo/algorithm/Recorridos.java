package uade.prog3.tpo.algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * UNIDAD: Grafos y Recorridos (Hito 4) - BFS y DFS.
 *
 * Ambos devuelven los ids de los vértices ALCANZABLES desde el origen, en el orden
 * en que se visitan. Los vecinos se exploran en el orden de la lista de adyacencia.
 * Si el grafo no es conexo, los vértices de otras componentes no aparecen.
 *
 * Complejidad temporal: O(V + E) — cada vértice se visita una vez y cada arista
 * se examina dos veces (una por extremo).
 * Complejidad espacial auxiliar: O(V) — arreglo de visitados más la cola (BFS)
 * o la pila de llamadas (DFS).
 */
public class Recorridos {

    /**
     * Búsqueda en anchura: explora por niveles (primero todo lo que está a 1 salto,
     * después a 2, ...). Estructura auxiliar: cola FIFO.
     */
    public List<String> bfs(Grafo grafo, String origenId) {
        int origen = indiceDelOrigen(grafo, origenId);
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();
        Deque<Integer> cola = new ArrayDeque<>();

        // Se marca AL ENCOLAR (no al desencolar): así ningún vértice entra dos veces a la cola
        visitado[origen] = true;
        cola.add(origen);

        while (!cola.isEmpty()) {
            int u = cola.poll();
            orden.add(grafo.idDe(u));
            for (Grafo.Arista arista : grafo.vecinos(u)) {
                if (!visitado[arista.destino()]) {
                    visitado[arista.destino()] = true;
                    cola.add(arista.destino());
                }
            }
        }
        return orden;
    }

    /**
     * Búsqueda en profundidad: avanza por un camino hasta no poder seguir y recién
     * entonces retrocede. Estructura auxiliar: pila de llamadas recursivas.
     */
    public List<String> dfs(Grafo grafo, String origenId) {
        int origen = indiceDelOrigen(grafo, origenId);
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();
        dfsRecursivo(grafo, origen, visitado, orden);
        return orden;
    }

    private void dfsRecursivo(Grafo grafo, int u, boolean[] visitado, List<String> orden) {
        visitado[u] = true;
        orden.add(grafo.idDe(u));
        for (Grafo.Arista arista : grafo.vecinos(u)) {
            if (!visitado[arista.destino()]) {
                dfsRecursivo(grafo, arista.destino(), visitado, orden);
            }
        }
    }

    private int indiceDelOrigen(Grafo grafo, String origenId) {
        int origen = grafo.indiceDe(origenId);
        if (origen < 0) {
            throw new IllegalArgumentException("No existe el vértice de origen '" + origenId + "'");
        }
        return origen;
    }
}
