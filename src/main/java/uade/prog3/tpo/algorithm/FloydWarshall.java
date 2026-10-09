package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIDAD: Programación Dinámica sobre grafos (Hito 7) - Caminos mínimos entre todos los pares.
 *
 * D_k[i][j] es el costo mínimo de i a j usando como intermedios solo los vértices 0..k:
 *
 *   D_k[i][j] = min(D_{k-1}[i][j], D_{k-1}[i][k] + D_{k-1}[k][j])
 *
 * La matriz se actualiza en el lugar, por eso alcanza con una sola double[V][V].
 * A diferencia de Dijkstra admite aristas de peso negativo; si al terminar queda
 * D[i][i] < 0, el vértice i está en un ciclo negativo y las distancias no son válidas.
 *
 * Trabaja sobre una matriz de adyacencia en memoria, sin consultas a la base.
 *
 * Complejidad temporal: O(V^3), tres bucles anidados sobre los V vértices.
 * Complejidad espacial: O(V^2), la matriz de distancias.
 */
public class FloydWarshall {

    /** Marca que no hay arista (o camino) entre dos vértices. */
    public static final double INF = Double.POSITIVE_INFINITY;

    /**
     * @param distancias           D[i][j]: costo mínimo de i a j; INF si j no es alcanzable desde i.
     *                             No son válidas si cicloNegativo es true.
     * @param cicloNegativo        true si algún D[i][i] quedó menor a 0.
     * @param verticesEnCicloNegativo índices i con D[i][i] menor a 0.
     * @param estadosExpandidos    ternas (k, i, j) evaluadas: siempre V^3.
     */
    public record ResultadoFloyd(
            double[][] distancias,
            boolean cicloNegativo,
            List<Integer> verticesEnCicloNegativo,
            long estadosExpandidos
    ) {
    }

    /**
     * @param pesos matriz de adyacencia cuadrada: pesos[i][j] es el costo de la arista i -> j,
     *              o INF si no existe. La diagonal se toma como 0 salvo que tenga un lazo negativo.
     */
    public ResultadoFloyd resolver(double[][] pesos) {
        if (pesos == null) {
            throw new IllegalArgumentException("La matriz de adyacencia no puede ser nula");
        }
        int v = pesos.length;
        double[][] d = new double[v][v];

        for (int i = 0; i < v; i++) {
            if (pesos[i] == null || pesos[i].length != v) {
                throw new IllegalArgumentException("La matriz de adyacencia debe ser cuadrada");
            }
            for (int j = 0; j < v; j++) {
                double peso = pesos[i][j];
                if (Double.isNaN(peso) || peso == Double.NEGATIVE_INFINITY) {
                    throw new IllegalArgumentException("Peso inválido en la arista " + i + " -> " + j);
                }
                d[i][j] = peso;
            }
            d[i][i] = Math.min(0, pesos[i][i]);
        }

        long estadosExpandidos = 0;
        for (int k = 0; k < v; k++) {
            for (int i = 0; i < v; i++) {
                for (int j = 0; j < v; j++) {
                    estadosExpandidos++;
                    // Si falta alguno de los dos tramos la suma da INF y no mejora nada.
                    double porK = d[i][k] + d[k][j];
                    if (porK < d[i][j]) {
                        d[i][j] = porK;
                    }
                }
            }
        }

        // Ir de un vértice a sí mismo cuesta 0: si cuesta menos, hay un ciclo negativo que lo incluye.
        List<Integer> enCicloNegativo = new ArrayList<>();
        for (int i = 0; i < v; i++) {
            if (d[i][i] < 0) {
                enCicloNegativo.add(i);
            }
        }

        return new ResultadoFloyd(d, !enCicloNegativo.isEmpty(), List.copyOf(enCicloNegativo), estadosExpandidos);
    }

    /** Pasa la lista de adyacencia a matriz: 0 en la diagonal, INF donde no hay ruta. O(V^2 + E). */
    public static double[][] matrizDeAdyacencia(Grafo grafo) {
        if (grafo == null) {
            throw new IllegalArgumentException("El grafo no puede ser nulo");
        }
        int v = grafo.cantidadVertices();
        double[][] pesos = new double[v][v];
        for (int i = 0; i < v; i++) {
            for (int j = 0; j < v; j++) {
                pesos[i][j] = i == j ? 0 : INF;
            }
            for (Grafo.Arista arista : grafo.vecinos(i)) {
                pesos[i][arista.destino()] = Math.min(pesos[i][arista.destino()], arista.peso());
            }
        }
        return pesos;
    }
}
