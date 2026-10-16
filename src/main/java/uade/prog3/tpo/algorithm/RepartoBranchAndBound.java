package uade.prog3.tpo.algorithm;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * UNIDAD: Ramificación y Poda / Branch & Bound (Hito 11).
 *
 * Ruta de reparto de menor costo: sale del punto 0 (la base), visita una vez cada uno de los
 * otros puntos y vuelve a la base. Trabaja sobre la matriz de costos mínimos entre los puntos
 * (el service la arma con Dijkstra, porque dos estaciones pueden no tener ruta directa).
 *
 * RAMIFICAR: un nodo del árbol es un recorrido parcial que empieza en la base; sus hijos
 * agregan cada punto todavía no visitado.
 *
 * ACOTAR (cota inferior optimista):
 *   cota(nodo) = costoAcumulado + Σ_{v no visitado} minSaliente(v)
 *   donde minSaliente(v) = min_{u != v} costo(v, u).
 * Es admisible: lo que falta del recorrido sale exactamente una vez de cada punto no visitado
 * (hacia otro punto o hacia la base) y cada una de esas salidas cuesta al menos minSaliente(v).
 * Por eso la cota nunca supera el costo real de completar el recorrido.
 *
 * PODAR: si cota(nodo) >= mejor costo encontrado, ningún descendiente puede mejorarlo y la rama
 * se descarta. La primera cota superior sale del recorrido del vecino más cercano.
 *
 * EXPLORACIÓN: primero el mejor (best-first), con una cola de prioridad ordenada por cota.
 *
 * Complejidad temporal: O(k! · k) en el peor caso, con k puntos a visitar (árbol de permutaciones;
 * calcular la cota de cada hijo cuesta O(k)). La poda reduce mucho el árbol explorado.
 * Complejidad espacial: O(k! · k) en el peor caso (nodos pendientes en la cola, cada uno con su camino).
 */
public class RepartoBranchAndBound {

    /** Recorrido parcial: camino[0..profundidad] con camino[0] = base. */
    private record Nodo(int[] camino, int profundidad, int visitados, int costo, int cota, long secuencia) {
        int actual() {
            return camino[profundidad];
        }
    }

    /**
     * @param recorrido           índices de los puntos en orden de visita, empezando y terminando en 0.
     * @param costoTotal          costo del recorrido óptimo.
     * @param cotaRaiz            cota inferior del nodo raíz (antes de elegir el primer punto).
     * @param cotaSuperiorInicial costo del recorrido del vecino más cercano, usado como primera cota superior.
     * @param nodosGenerados      nodos creados al ramificar (incluye la raíz y las hojas).
     * @param nodosExpandidos     nodos que se sacaron de la cola y se ramificaron.
     * @param nodosPodados        nodos descartados porque su cota no podía mejorar la mejor solución.
     * @param nodosArbolCompleto  nodos que tendría el árbol sin ninguna poda.
     */
    public record ResultadoReparto(
            int[] recorrido,
            int costoTotal,
            int cotaRaiz,
            int cotaSuperiorInicial,
            long nodosGenerados,
            long nodosExpandidos,
            long nodosPodados,
            long nodosArbolCompleto
    ) {
    }

    /** Límite de puntos a visitar: el árbol crece como k!, más allá deja de ser práctico. */
    public static final int MAX_PUNTOS = 12;

    /**
     * @param costos matriz cuadrada de costos mínimos entre puntos; el punto 0 es la base.
     *               Debe tener al menos 2 puntos (la base y uno a visitar) y costos >= 0.
     */
    public ResultadoReparto resolver(int[][] costos) {
        validar(costos);
        int n = costos.length;
        int todos = (1 << n) - 1;

        int[] minSaliente = new int[n];
        for (int v = 0; v < n; v++) {
            int min = Integer.MAX_VALUE;
            for (int u = 0; u < n; u++) {
                if (u != v && costos[v][u] < min) {
                    min = costos[v][u];
                }
            }
            minSaliente[v] = min;
        }

        // Primera cota superior: vecino más cercano
        int[] mejorRecorrido = vecinoMasCercano(costos);
        int mejorCosto = costoDe(mejorRecorrido, costos);
        int cotaSuperiorInicial = mejorCosto;

        long secuencia = 0;
        long generados = 1;
        long expandidos = 0;
        long podados = 0;

        int[] caminoRaiz = new int[n + 1];
        int cotaRaiz = cota(0, 1, minSaliente, n);
        PriorityQueue<Nodo> cola = new PriorityQueue<>(
                Comparator.comparingInt(Nodo::cota)
                        .thenComparing(Comparator.comparingInt(Nodo::profundidad).reversed())
                        .thenComparingLong(Nodo::secuencia));
        cola.add(new Nodo(caminoRaiz, 0, 1, 0, cotaRaiz, secuencia++));

        while (!cola.isEmpty()) {
            Nodo nodo = cola.poll();

            // Pudo encolarse antes de que apareciera una solución mejor
            if (nodo.cota() >= mejorCosto) {
                podados++;
                continue;
            }
            expandidos++;

            for (int v = 1; v < n; v++) {
                if ((nodo.visitados() & (1 << v)) != 0) {
                    continue;
                }
                generados++;
                int costo = nodo.costo() + costos[nodo.actual()][v];
                int visitados = nodo.visitados() | (1 << v);
                int[] camino = nodo.camino().clone();
                camino[nodo.profundidad() + 1] = v;

                if (visitados == todos) {
                    // Hoja: el recorrido se cierra volviendo a la base
                    int total = costo + costos[v][0];
                    if (total < mejorCosto) {
                        mejorCosto = total;
                        camino[n] = 0;
                        mejorRecorrido = camino;
                    }
                    continue;
                }

                int cotaHijo = cota(costo, visitados, minSaliente, n);
                if (cotaHijo >= mejorCosto) {
                    podados++; // Poda: la rama no puede mejorar la mejor solución
                    continue;
                }
                cola.add(new Nodo(camino, nodo.profundidad() + 1, visitados, costo, cotaHijo, secuencia++));
            }
        }

        return new ResultadoReparto(mejorRecorrido, mejorCosto, cotaRaiz, cotaSuperiorInicial,
                generados, expandidos, podados, nodosArbolCompleto(n - 1));
    }

    /** costoAcumulado + Σ minSaliente(v) de los puntos no visitados. O(n). */
    private int cota(int costoAcumulado, int visitados, int[] minSaliente, int n) {
        int cota = costoAcumulado;
        for (int v = 1; v < n; v++) {
            if ((visitados & (1 << v)) == 0) {
                cota += minSaliente[v];
            }
        }
        return cota;
    }

    /** Desde la base, ir siempre al punto no visitado más barato y al final volver. O(n²). */
    private int[] vecinoMasCercano(int[][] costos) {
        int n = costos.length;
        int[] recorrido = new int[n + 1];
        boolean[] visitado = new boolean[n];
        visitado[0] = true;
        int actual = 0;
        for (int paso = 1; paso < n; paso++) {
            int siguiente = -1;
            for (int v = 1; v < n; v++) {
                if (!visitado[v] && (siguiente < 0 || costos[actual][v] < costos[actual][siguiente])) {
                    siguiente = v;
                }
            }
            visitado[siguiente] = true;
            recorrido[paso] = siguiente;
            actual = siguiente;
        }
        recorrido[n] = 0;
        return recorrido;
    }

    private int costoDe(int[] recorrido, int[][] costos) {
        int total = 0;
        for (int i = 0; i + 1 < recorrido.length; i++) {
            total += costos[recorrido[i]][recorrido[i + 1]];
        }
        return total;
    }

    /** Nodos del árbol de permutaciones de k puntos: Σ_{i=0..k} k!/(k-i)!. */
    static long nodosArbolCompleto(int k) {
        long total = 0;
        long nivel = 1;
        for (int i = 0; i <= k; i++) {
            total += nivel;
            nivel *= (k - i);
        }
        return total;
    }

    private void validar(int[][] costos) {
        if (costos == null || costos.length < 2) {
            throw new IllegalArgumentException("Se necesita la base y al menos un punto a visitar");
        }
        if (costos.length - 1 > MAX_PUNTOS) {
            throw new IllegalArgumentException(
                    "Como máximo se pueden visitar " + MAX_PUNTOS + " puntos (el árbol crece como k!)");
        }
        for (int[] fila : costos) {
            if (fila == null || fila.length != costos.length) {
                throw new IllegalArgumentException("La matriz de costos debe ser cuadrada");
            }
            for (int c : fila) {
                if (c < 0) {
                    throw new IllegalArgumentException("Los costos no pueden ser negativos");
                }
            }
        }
    }
}
