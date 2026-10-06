package uade.prog3.tpo.algorithm;

/**
 * Estructura de datos Union-Find (Disjoint Set Union - DSU) implementada a mano.
 *
 * UNIDAD: Árbol Generador Mínimo (Hito 5) - PUNTAJE: 1 punto (en Kruskal).
 *
 * OPTIMIZACIONES IMPLEMENTADAS:
 * 1. Compresión de Caminos (Path Compression):
 *    Durante la operación find(u), todos los nodos del camino visitado se reasignan
 *    directamente como hijos de la raíz representativa, aplanando la estructura.
 * 2. Unión por Rango (Union by Rank):
 *    Al unir dos conjuntos disjuntos, el árbol de menor profundidad siempre se cuelga
 *    de la raíz del árbol de mayor profundidad, impidiendo que la altura crezca linealmente.
 *
 * COMPLEJIDAD ASINTÓTICA:
 * Con ambas optimizaciones, cualquier secuencia de M operaciones sobre N elementos
 * opera en tiempo O(M · α(N)), donde α es la función inversa de Ackermann.
 * Dado que α(N) ≤ 4 para cualquier valor computable en el universo físico, cada
 * operación se ejecuta en tiempo prácticamente lineal O(1) amortizado.
 */
public class UnionFind {

    private final int[] parent;
    private final int[] rank;
    private int cantidadComponentes;

    public UnionFind(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("La cantidad de elementos no puede ser negativa");
        }
        this.parent = new int[n];
        this.rank = new int[n];
        this.cantidadComponentes = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    /**
     * Encuentra la raíz canónica del elemento con compresión de camino.
     */
    public int find(int i) {
        if (i < 0 || i >= parent.length) {
            throw new IndexOutOfBoundsException("Índice de elemento fuera de rango: " + i);
        }
        if (parent[i] != i) {
            parent[i] = find(parent[i]); // Compresión de caminos (Path Compression)
        }
        return parent[i];
    }

    /**
     * Une los conjuntos de los elementos i y j según su rango.
     *
     * @return true si pertenecían a componentes distintas y se unieron exitosamente;
     *         false si ya compartían la misma componente (formaría un ciclo).
     */
    public boolean union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);

        if (rootI == rootJ) {
            return false; // Detección de ciclo
        }

        // Unión por rango (Union by Rank)
        if (rank[rootI] < rank[rootJ]) {
            parent[rootI] = rootJ;
        } else if (rank[rootI] > rank[rootJ]) {
            parent[rootJ] = rootI;
        } else {
            parent[rootJ] = rootI;
            rank[rootI]++;
        }

        cantidadComponentes--;
        return true;
    }

    public boolean conectados(int i, int j) {
        return find(i) == find(j);
    }

    public int getCantidadComponentes() {
        return cantidadComponentes;
    }
}
