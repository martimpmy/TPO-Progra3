package uade.prog3.tpo.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.RepartoBranchAndBound.ResultadoReparto;

class RepartoBranchAndBoundTest {

    private final RepartoBranchAndBound bb = new RepartoBranchAndBound();

    /** Matriz de costos mínimos del grafo del dominio entre los ids dados (el primero es la base). */
    private static int[][] costosDominio(List<String> ids) {
        Grafo g = GrafoDominio.crear();
        CaminosMinimos cm = new CaminosMinimos();
        int[][] c = new int[ids.size()][ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            for (int j = 0; j < ids.size(); j++) {
                c[i][j] = i == j ? 0 : cm.dijkstra(g, ids.get(i), ids.get(j)).consumoTotal();
            }
        }
        return c;
    }

    /** Óptimo por fuerza bruta: prueba todas las permutaciones de los puntos 1..n-1. */
    private static int fuerzaBruta(int[][] c) {
        int n = c.length;
        int[] perm = new int[n - 1];
        for (int i = 0; i < n - 1; i++) {
            perm[i] = i + 1;
        }
        return permutar(c, perm, 0);
    }

    private static int permutar(int[][] c, int[] perm, int k) {
        if (k == perm.length) {
            int total = c[0][perm[0]] + c[perm[perm.length - 1]][0];
            for (int i = 0; i + 1 < perm.length; i++) {
                total += c[perm[i]][perm[i + 1]];
            }
            return total;
        }
        int mejor = Integer.MAX_VALUE;
        for (int i = k; i < perm.length; i++) {
            int t = perm[k]; perm[k] = perm[i]; perm[i] = t;
            mejor = Math.min(mejor, permutar(c, perm, k + 1));
            t = perm[k]; perm[k] = perm[i]; perm[i] = t;
        }
        return mejor;
    }

    private static int costoDe(int[] recorrido, int[][] c) {
        int total = 0;
        for (int i = 0; i + 1 < recorrido.length; i++) {
            total += c[recorrido[i]][recorrido[i + 1]];
        }
        return total;
    }

    private static void esRecorridoValido(int[] recorrido, int n) {
        assertThat(recorrido).hasSize(n + 1);
        assertThat(recorrido[0]).isZero();
        assertThat(recorrido[n]).isZero();
        boolean[] visto = new boolean[n];
        for (int i = 1; i < n; i++) {
            assertThat(visto[recorrido[i]]).as("punto repetido").isFalse();
            visto[recorrido[i]] = true;
        }
    }

    @Test
    @DisplayName("Ejemplo del informe (ALPHA, VEGA, KEPLER desde SOL): el óptimo es 75 CA, no 65")
    void ejemploDelInforme() {
        int[][] c = costosDominio(List.of("SOL", "ALPHA", "VEGA", "KEPLER"));

        ResultadoReparto r = bb.resolver(c);

        // No existe ruta directa KEPLER–SOL: volver cuesta 35 (KEPLER→SIRIUS→ALPHA→SOL)
        assertThat(r.costoTotal()).isEqualTo(75).isEqualTo(fuerzaBruta(c));
        assertThat(costoDe(r.recorrido(), c)).isEqualTo(75);
        esRecorridoValido(r.recorrido(), 4);
    }

    @Test
    @DisplayName("Coincide con la fuerza bruta en 300 matrices al azar de 2 a 8 puntos (simétricas y no simétricas)")
    void coincideConFuerzaBruta() {
        Random rnd = new Random(2026);
        for (int caso = 0; caso < 300; caso++) {
            int n = 2 + rnd.nextInt(7);
            boolean simetrica = caso % 2 == 0;
            int[][] c = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    if (simetrica && j < i) { c[i][j] = c[j][i]; continue; }
                    c[i][j] = 1 + rnd.nextInt(50);
                }
            }

            ResultadoReparto r = bb.resolver(c);
            int optimo = fuerzaBruta(c);

            assertThat(r.costoTotal()).as("caso %d", caso).isEqualTo(optimo);
            assertThat(costoDe(r.recorrido(), c)).isEqualTo(optimo);
            esRecorridoValido(r.recorrido(), n);
            // Cota optimista: la de la raíz nunca supera el óptimo
            assertThat(r.cotaRaiz()).isLessThanOrEqualTo(optimo);
            assertThat(r.cotaSuperiorInicial()).isGreaterThanOrEqualTo(optimo);
        }
    }

    @Test
    @DisplayName("Las 7 estaciones desde SOL: encuentra el óptimo generando menos de un tercio del árbol (71 % de poda)")
    void todasLasEstacionesPodaElArbol() {
        int[][] c = costosDominio(List.of("SOL", "ALPHA", "SIRIUS", "VEGA", "KEPLER", "ORION", "NOVA", "CITADEL"));

        ResultadoReparto r = bb.resolver(c);

        assertThat(r.costoTotal()).isEqualTo(fuerzaBruta(c));
        assertThat(r.nodosArbolCompleto()).isEqualTo(13_700); // Σ 7!/(7-i)! para i = 0..7
        assertThat(r.nodosPodados()).isPositive();
        // Determinístico: 3.995 de 13.700 nodos (se poda el 71 % del árbol)
        assertThat(r.nodosGenerados()).isEqualTo(3_995).isLessThan(r.nodosArbolCompleto() / 3);
    }

    @Test
    @DisplayName("Una sola estación: ida y vuelta")
    void unaSolaEstacion() {
        ResultadoReparto r = bb.resolver(new int[][]{{0, 12}, {12, 0}});

        assertThat(r.recorrido()).containsExactly(0, 1, 0);
        assertThat(r.costoTotal()).isEqualTo(24);
    }

    @Test
    @DisplayName("Tamaño del árbol completo: 1 + k + k(k-1) + ... + k!")
    void tamanioDelArbolCompleto() {
        assertThat(RepartoBranchAndBound.nodosArbolCompleto(1)).isEqualTo(2);
        assertThat(RepartoBranchAndBound.nodosArbolCompleto(3)).isEqualTo(1 + 3 + 6 + 6);
    }

    @Test
    @DisplayName("Matriz nula, de un solo punto, no cuadrada, con costos negativos o demasiado grande: IllegalArgumentException")
    void entradasInvalidas() {
        assertThatThrownBy(() -> bb.resolver(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bb.resolver(new int[][]{{0}})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bb.resolver(new int[][]{{0, 1}, {1}})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bb.resolver(new int[][]{{0, -1}, {1, 0}})).isInstanceOf(IllegalArgumentException.class);
        int n = RepartoBranchAndBound.MAX_PUNTOS + 2;
        assertThatThrownBy(() -> bb.resolver(new int[n][n])).isInstanceOf(IllegalArgumentException.class);
    }
}
