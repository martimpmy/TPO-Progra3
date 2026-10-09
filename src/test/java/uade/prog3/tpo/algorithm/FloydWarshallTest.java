package uade.prog3.tpo.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uade.prog3.tpo.algorithm.FloydWarshall.INF;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.FloydWarshall.ResultadoFloyd;

class FloydWarshallTest {

    private final FloydWarshall floydWarshall = new FloydWarshall();
    private final Grafo grafo = GrafoDominio.crear();

    private double distancia(ResultadoFloyd r, String origen, String destino) {
        return r.distancias()[grafo.indiceDe(origen)][grafo.indiceDe(destino)];
    }

    @Test
    @DisplayName("Grafo del dominio: las distancias desde Base Solar coinciden con el cálculo manual")
    void distanciasDesdeBaseSolar() {
        ResultadoFloyd r = floydWarshall.resolver(FloydWarshall.matrizDeAdyacencia(grafo));

        assertThat(distancia(r, "SOL", "ALPHA")).isEqualTo(12);
        assertThat(distancia(r, "SOL", "SIRIUS")).isEqualTo(20); // vía ALPHA, mejor que la directa de 25
        assertThat(distancia(r, "SOL", "VEGA")).isEqualTo(30);
        assertThat(distancia(r, "SOL", "KEPLER")).isEqualTo(35);
        assertThat(distancia(r, "SOL", "ORION")).isEqualTo(44);
        assertThat(distancia(r, "SOL", "NOVA")).isEqualTo(49);
        assertThat(distancia(r, "SOL", "CITADEL")).isEqualTo(56);
    }

    @Test
    @DisplayName("Grafo del dominio: sin ciclos negativos, diagonal en 0 y matriz simétrica")
    void grafoNormalSinCicloNegativo() {
        ResultadoFloyd r = floydWarshall.resolver(FloydWarshall.matrizDeAdyacencia(grafo));
        int v = grafo.cantidadVertices();

        assertThat(r.cicloNegativo()).isFalse();
        assertThat(r.verticesEnCicloNegativo()).isEmpty();
        for (int i = 0; i < v; i++) {
            assertThat(r.distancias()[i][i]).isZero();
            for (int j = 0; j < v; j++) {
                assertThat(r.distancias()[i][j]).isEqualTo(r.distancias()[j][i]);
            }
        }
    }

    @Test
    @DisplayName("Coincide con Dijkstra para los 64 pares de estaciones")
    void coincideConDijkstraEnTodosLosPares() {
        ResultadoFloyd r = floydWarshall.resolver(FloydWarshall.matrizDeAdyacencia(grafo));
        CaminosMinimos caminosMinimos = new CaminosMinimos();
        int v = grafo.cantidadVertices();

        for (int i = 0; i < v; i++) {
            int[] dijkstra = caminosMinimos.dijkstraDesde(grafo, i).distancias();
            for (int j = 0; j < v; j++) {
                assertThat(r.distancias()[i][j])
                        .as("%s -> %s", grafo.idDe(i), grafo.idDe(j))
                        .isEqualTo(dijkstra[j]);
            }
        }
    }

    @Test
    @DisplayName("Expande V^3 estados: 512 para las 8 estaciones")
    void estadosExpandidos() {
        ResultadoFloyd r = floydWarshall.resolver(FloydWarshall.matrizDeAdyacencia(grafo));

        assertThat(r.estadosExpandidos()).isEqualTo(512);
    }

    @Test
    @DisplayName("Arista negativa sin ciclo: usa el atajo y no dispara la alerta")
    void aristaNegativaSinCiclo() {
        // A -> B (5), B -> C (-2), A -> C (4): conviene A -> B -> C = 3
        double[][] pesos = {
                {0, 5, 4},
                {INF, 0, -2},
                {INF, INF, 0}
        };

        ResultadoFloyd r = floydWarshall.resolver(pesos);

        assertThat(r.cicloNegativo()).isFalse();
        assertThat(r.distancias()[0][2]).isEqualTo(3);
        assertThat(r.distancias()[2][0]).isEqualTo(INF); // el grafo es dirigido: no hay vuelta
    }

    @Test
    @DisplayName("Ciclo negativo: lo detecta por la diagonal e informa qué vértices lo forman")
    void detectaCicloNegativo() {
        // A -> B (1), B -> C (-3), C -> A (1): el ciclo suma -1. D solo recibe una arista desde A.
        double[][] pesos = {
                {0, 1, INF, 5},
                {INF, 0, -3, INF},
                {1, INF, 0, INF},
                {INF, INF, INF, 0}
        };

        ResultadoFloyd r = floydWarshall.resolver(pesos);

        assertThat(r.cicloNegativo()).isTrue();
        assertThat(r.verticesEnCicloNegativo()).containsExactly(0, 1, 2);
        assertThat(r.distancias()[0][0]).isNegative();
        assertThat(r.distancias()[3][3]).isZero();
    }

    @Test
    @DisplayName("Una ruta de ida y vuelta con costo negativo ya es un ciclo negativo")
    void aristaNoDirigidaNegativa() {
        double[][] pesos = {
                {0, -4},
                {-4, 0}
        };

        assertThat(floydWarshall.resolver(pesos).cicloNegativo()).isTrue();
    }

    @Test
    @DisplayName("Vértice aislado: queda a distancia infinita del resto")
    void verticeInalcanzable() {
        double[][] pesos = {
                {0, 7, INF},
                {7, 0, INF},
                {INF, INF, 0}
        };

        ResultadoFloyd r = floydWarshall.resolver(pesos);

        assertThat(r.distancias()[0][2]).isEqualTo(INF);
        assertThat(r.distancias()[0][1]).isEqualTo(7);
        assertThat(r.cicloNegativo()).isFalse();
    }

    @Test
    @DisplayName("No modifica la matriz de entrada")
    void noModificaLaEntrada() {
        double[][] pesos = {
                {0, 1, 10},
                {1, 0, 1},
                {10, 1, 0}
        };

        ResultadoFloyd r = floydWarshall.resolver(pesos);

        assertThat(r.distancias()[0][2]).isEqualTo(2);
        assertThat(pesos[0][2]).isEqualTo(10);
    }

    @Test
    @DisplayName("Matriz nula, no cuadrada o con pesos inválidos: IllegalArgumentException")
    void entradasInvalidas() {
        assertThatThrownBy(() -> floydWarshall.resolver(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> floydWarshall.resolver(new double[][]{{0, 1}, {1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> floydWarshall.resolver(new double[][]{{0, Double.NaN}, {1, 0}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FloydWarshall.matrizDeAdyacencia(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Grafo vacío: matriz vacía y cero estados")
    void grafoVacio() {
        ResultadoFloyd r = floydWarshall.resolver(new double[0][0]);

        assertThat(r.distancias()).isEmpty();
        assertThat(r.cicloNegativo()).isFalse();
        assertThat(r.estadosExpandidos()).isZero();
    }
}
