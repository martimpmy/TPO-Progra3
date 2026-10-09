package uade.prog3.tpo.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import uade.prog3.tpo.algorithm.GreedyCarga.ResultadoGreedy;
import uade.prog3.tpo.algorithm.Mochila01.ResultadoMochila;
import uade.prog3.tpo.model.Mineral;

/**
 * Hito 6: juego de datos donde el Greedy del Hito 3 NO da el óptimo.
 * El paso a paso de la matriz está en docs/CONTRAEJEMPLO.md.
 */
class ContraejemploGreedyVsDPTest {

    private static final double CAPACIDAD = 10;

    private static final List<Mineral> MINERALES = List.of(
            new Mineral("Cristal de Taquiones", 6, 66),
            new Mineral("Núcleo de Plasma", 5, 50),
            new Mineral("Aleación de Titanio", 5, 50)
    );

    private final GreedyCarga greedy = new GreedyCarga();
    private final Mochila01 mochila = new Mochila01();

    @Test
    @DisplayName("Greedy obtiene 66 CG y DP obtiene 100 CG sobre los mismos 3 minerales")
    void greedyNoEsOptimo() {
        double valorGreedy = greedy.cargar(MINERALES, CAPACIDAD).valorTotal();
        double valorDP = mochila.resolver(MINERALES, CAPACIDAD).valorTotal();

        assertEquals(66, valorGreedy);
        assertEquals(100, valorDP);
        assertTrue(valorDP > valorGreedy);
    }

    @Test
    @DisplayName("Greedy carga solo el Cristal (6 t); DP carga Plasma + Titanio (10 t)")
    void itemsElegidosPorCadaAlgoritmo() {
        ResultadoGreedy resultadoGreedy = greedy.cargar(MINERALES, CAPACIDAD);
        ResultadoMochila resultadoDP = mochila.resolver(MINERALES, CAPACIDAD);

        assertEquals(List.of("Cristal de Taquiones"),
                resultadoGreedy.minerales().stream().map(Mineral::getNombre).toList());
        assertEquals(6, resultadoGreedy.pesoOcupado());

        assertEquals(List.of("Núcleo de Plasma", "Aleación de Titanio"),
                resultadoDP.minerales().stream().map(Mineral::getNombre).toList());
        assertEquals(10, resultadoDP.pesoOcupado());
    }

    @Test
    @DisplayName("La matriz DP coincide con el paso a paso de docs/CONTRAEJEMPLO.md")
    void matrizCoincideConElDocumento() {
        double[][] matriz = mochila.resolver(MINERALES, CAPACIDAD).matrizDP();

        assertEquals(4, matriz.length);
        // columnas:                     0  1  2  3  4  5   6   7   8   9   10
        assertFila(matriz[0], new double[]{0, 0, 0, 0, 0, 0,  0,  0,  0,  0,  0});
        assertFila(matriz[1], new double[]{0, 0, 0, 0, 0, 0,  66, 66, 66, 66, 66});
        assertFila(matriz[2], new double[]{0, 0, 0, 0, 0, 50, 66, 66, 66, 66, 66});
        assertFila(matriz[3], new double[]{0, 0, 0, 0, 0, 50, 66, 66, 66, 66, 100});
    }

    private void assertFila(double[] obtenida, double[] esperada) {
        assertEquals(esperada.length, obtenida.length);
        for (int c = 0; c < esperada.length; c++) {
            assertEquals(esperada[c], obtenida[c], "columna " + c);
        }
    }
}
