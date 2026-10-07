package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import uade.prog3.tpo.dto.MstResponseDTO.AristaMstDTO;

/**
 * UNIDAD: Árbol Generador Mínimo - MST (Hito 5) - PUNTAJE: 2 puntos (Prim 1 pt, Kruskal 1 pt).
 *
 * Implementaciones de los algoritmos de Prim y Kruskal para diseñar la red troncal
 * de balizas y enlaces subespaciales que interconecta todas las estaciones al menor
 * costo global de Celdas de Antimateria (CA) sin generar ciclos.
 *
 * REGLAS DE CÁTEDRA:
 * - Prim: Crece un único árbol continuo evaluando aristas frontera con PriorityQueue en O(E log V).
 * - Kruskal: Ordena las aristas por costo ascendente y detecta ciclos con Union-Find propio
 *   (compresión de camino + unión por rango) en O(E log E).
 * - Reutiliza el algoritmo de ordenamiento propio (MergeSort del Hito 2) sin Collections.sort().
 * - Valida conectividad: lanza excepción con mensaje descriptivo si el grafo es disconexo.
 */
public class ArbolRecubrimiento {

    /**
     * Representación de una arista del MST con nombres e identificadores de las estaciones.
     */
    public record AristaMst(
            String origenId,
            String origenNombre,
            String destinoId,
            String destinoNombre,
            int costo
    ) {
    }

    /**
     * Resultado estructurado del Árbol Generador Mínimo.
     */
    public record ResultadoMst(
            String algoritmo,
            int costoTotal,
            List<AristaMst> aristas
    ) {
    }

    /**
     * Estructura compatible con el scaffold original del docente.
     */
    public record AristaScaffold(String origen, String destino, double costo) {
    }

    public record Mst(List<AristaScaffold> aristas, double costoTotal) {
    }

    private final Ordenamiento ordenamiento = new Ordenamiento();

    /**
     * Algoritmo de Prim con Cola de Prioridad: O(E log V).
     *
     * @param grafo    Grafo no dirigido ponderado cargado en memoria.
     * @param origenId Vértice desde donde comenzar el tendido (opcional; si es nulo o vacío usa el primer nodo).
     * @return ResultadoMst con el costo total y las aristas seleccionadas.
     */
    public ResultadoMst prim(Grafo grafo, String origenId) {
        if (grafo == null) {
            throw new IllegalArgumentException("El grafo no puede ser nulo");
        }
        int vCount = grafo.cantidadVertices();
        if (vCount == 0) {
            return new ResultadoMst("Prim (Árbol Troncal Continuo)", 0, List.of());
        }

        int inicio = 0;
        if (origenId != null && !origenId.isBlank()) {
            inicio = grafo.indiceDe(origenId);
            if (inicio < 0) {
                throw new IllegalArgumentException("No existe el vértice inicial '" + origenId + "'");
            }
        }

        boolean[] inMST = new boolean[vCount];
        List<AristaMst> aristasSeleccionadas = new ArrayList<>();
        int costoTotal = 0;

        record AristaFrontera(int u, int v, int peso) implements Comparable<AristaFrontera> {
            @Override
            public int compareTo(AristaFrontera o) {
                return Integer.compare(this.peso, o.peso);
            }
        }

        PriorityQueue<AristaFrontera> pq = new PriorityQueue<>();
        inMST[inicio] = true;

        for (Grafo.Arista a : grafo.vecinos(inicio)) {
            pq.add(new AristaFrontera(inicio, a.destino(), a.peso()));
        }

        while (!pq.isEmpty() && aristasSeleccionadas.size() < vCount - 1) {
            AristaFrontera mejor = pq.poll();
            int v = mejor.v();

            if (inMST[v]) {
                continue;
            }

            inMST[v] = true;
            costoTotal += mejor.peso();
            aristasSeleccionadas.add(new AristaMst(
                    grafo.idDe(mejor.u()), grafo.nombreDe(mejor.u()),
                    grafo.idDe(v), grafo.nombreDe(v),
                    mejor.peso()
            ));

            for (Grafo.Arista siguiente : grafo.vecinos(v)) {
                if (!inMST[siguiente.destino()]) {
                    pq.add(new AristaFrontera(v, siguiente.destino(), siguiente.peso()));
                }
            }
        }

        if (vCount > 1 && aristasSeleccionadas.size() < vCount - 1) {
            throw new IllegalStateException("El grafo no es conexo: no es posible generar un MST completo.");
        }

        return new ResultadoMst("Prim (Árbol Troncal Continuo)", costoTotal, aristasSeleccionadas);
    }

    /**
     * Algoritmo de Kruskal con Union-Find propio: O(E log E).
     *
     * @param grafo Grafo no dirigido ponderado cargado en memoria.
     * @return ResultadoMst con el costo total y las aristas seleccionadas.
     */
    public ResultadoMst kruskal(Grafo grafo) {
        if (grafo == null) {
            throw new IllegalArgumentException("El grafo no puede ser nulo");
        }
        int vCount = grafo.cantidadVertices();
        if (vCount == 0) {
            return new ResultadoMst("Kruskal (con Union-Find propio)", 0, List.of());
        }

        record AristaGrafo(int u, int v, int peso) {
        }

        // Recolectar aristas no dirigidas únicas (u < v)
        List<AristaGrafo> todasAristas = new ArrayList<>();
        for (int u = 0; u < vCount; u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (u < a.destino()) {
                    todasAristas.add(new AristaGrafo(u, a.destino(), a.peso()));
                }
            }
        }

        // Ordenamiento por costo ascendente empleando el MergeSort propio de Hito 2
        List<AristaGrafo> aristasOrdenadas = ordenamiento.mergeSort(
                todasAristas, Comparator.comparingInt(AristaGrafo::peso)
        );

        UnionFind dsu = new UnionFind(vCount);
        List<AristaMst> aristasSeleccionadas = new ArrayList<>();
        int costoTotal = 0;

        for (AristaGrafo a : aristasOrdenadas) {
            // Si no pertenecen al mismo conjunto conexo, se unen y se incorpora la arista
            if (dsu.union(a.u(), a.v())) {
                costoTotal += a.peso();
                aristasSeleccionadas.add(new AristaMst(
                        grafo.idDe(a.u()), grafo.nombreDe(a.u()),
                        grafo.idDe(a.v()), grafo.nombreDe(a.v()),
                        a.peso()
                ));

                if (aristasSeleccionadas.size() == vCount - 1) {
                    break;
                }
            }
        }

        if (vCount > 1 && aristasSeleccionadas.size() < vCount - 1) {
            throw new IllegalStateException("El grafo no es conexo: no es posible generar un MST completo.");
        }

        return new ResultadoMst("Kruskal (con Union-Find propio)", costoTotal, aristasSeleccionadas);
    }

    /**
     * Adaptador para Prim compatible con el scaffold del docente.
     */
    public Mst primScaffold(Grafo grafo, String origenId) {
        ResultadoMst res = prim(grafo, origenId);
        List<AristaScaffold> list = res.aristas().stream()
                .map(a -> new AristaScaffold(a.origenId(), a.destinoId(), (double) a.costo()))
                .toList();
        return new Mst(list, (double) res.costoTotal());
    }

    /**
     * Adaptador para Kruskal compatible con el scaffold del docente.
     */
    public Mst kruskalScaffold(Grafo grafo) {
        ResultadoMst res = kruskal(grafo);
        List<AristaScaffold> list = res.aristas().stream()
                .map(a -> new AristaScaffold(a.origenId(), a.destinoId(), (double) a.costo()))
                .toList();
        return new Mst(list, (double) res.costoTotal());
    }
}
