
package uade.prog3.tpo.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import uade.prog3.tpo.algorithm.Mochila01.ResultadoMochila;
import uade.prog3.tpo.model.Mineral;

// >>> HITO 6: pruebas del algoritmo DP y recuperación de ítems.
// La comparación con Greedy está en ContraejemploGreedyVsDPTest.
class Mochila01Test {

    private final Mochila01 mochila = new Mochila01();

    private static final List<Mineral> CONTRAEJEMPLO = List.of(
            new Mineral("Cristal de Taquiones", 6, 66),
            new Mineral("Núcleo de Plasma", 5, 50),
            new Mineral("Aleación de Titanio", 5, 50)
    );

    @Test
    @DisplayName("DP encuentra el óptimo del contraejemplo: 100 CG")
    void encuentraOptimoContraejemplo() {

        ResultadoMochila dp = mochila.resolver(CONTRAEJEMPLO, 10);

        assertThat(dp.valorTotal()).isEqualTo(100.0);
        assertThat(dp.pesoOcupado()).isEqualTo(10.0);

        assertThat(dp.minerales())
                .extracting(Mineral::getNombre)
                .containsExactly(
                        "Núcleo de Plasma",
                        "Aleación de Titanio"
                );
    }

    @Test
    @DisplayName("La matriz DP tiene una fila inicial y una fila por cada mineral")
    void matrizTieneDimensionesCorrectas() {

        ResultadoMochila resultado = mochila.resolver(CONTRAEJEMPLO, 10);

        assertThat(resultado.matrizDP()).hasNumberOfRows(4);
        assertThat(resultado.matrizDP()[0]).hasSize(11);
        assertThat(resultado.matrizDP()[3][10]).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Recupera correctamente los ítems usando la matriz")
    void recuperaItems() {

        ResultadoMochila resultado = mochila.resolver(List.of(
                new Mineral("A", 3, 30),
                new Mineral("B", 4, 50),
                new Mineral("C", 5, 60)
        ), 7);

        assertThat(resultado.valorTotal()).isEqualTo(80.0);

        assertThat(resultado.minerales())
                .extracting(Mineral::getNombre)
                .containsExactly("A", "B");
    }

    @Test
    @DisplayName("También funciona con pesos decimales")
    void pesosDecimales() {

        ResultadoMochila resultado = mochila.resolver(List.of(
                new Mineral("A", 0.1, 10),
                new Mineral("B", 0.2, 20)
        ), 0.3);

        assertThat(resultado.valorTotal()).isEqualTo(30.0);

        assertThat(resultado.pesoOcupado())
                .isCloseTo(
                        0.3,
                        org.assertj.core.data.Offset.offset(1e-9)
                );

        assertThat(resultado.factorEscala()).isEqualTo(10);
        assertThat(resultado.minerales()).hasSize(2);
    }

    @Test
    @DisplayName("Capacidad cero no selecciona ningún mineral")
    void capacidadCero() {

        ResultadoMochila resultado = mochila.resolver(CONTRAEJEMPLO, 0);

        assertThat(resultado.valorTotal()).isZero();
        assertThat(resultado.minerales()).isEmpty();
    }

    @Test
    @DisplayName("Lista nula devuelve resultado vacío")
    void listaNula() {

        ResultadoMochila resultado = mochila.resolver(null, 10);

        assertThat(resultado.minerales()).isEmpty();
        assertThat(resultado.valorTotal()).isZero();
    }

    @Test
    @DisplayName("Una capacidad que no entra en memoria se rechaza sin reservar la matriz")
    void capacidadDemasiadoGrande() {

        assertThatThrownBy(() -> mochila.resolver(CONTRAEJEMPLO, 1e9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Capacidad negativa es inválida")
    void capacidadNegativa() {

        assertThatThrownBy(() -> mochila.resolver(CONTRAEJEMPLO, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
