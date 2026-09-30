package uade.prog3.tpo.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.GreedyCarga.ResultadoGreedy;
import uade.prog3.tpo.model.Mineral;

class GreedyCargaTest {

    private final GreedyCarga greedy = new GreedyCarga();

    private static final List<Mineral> CONTRAEJEMPLO_PDF = List.of(
            new Mineral("Cristal de Taquiones", 6, 66),  // ratio 11
            new Mineral("Núcleo de Plasma", 5, 50),      // ratio 10
            new Mineral("Aleación de Titanio", 5, 50)    // ratio 10
    );

    @Test
    @DisplayName("Contraejemplo del PDF: Greedy carga solo el Cristal y obtiene 66 (el óptimo es 100)")
    void contraejemploDelPdf() {
        ResultadoGreedy r = greedy.cargar(CONTRAEJEMPLO_PDF, 10);

        assertThat(r.minerales()).extracting(Mineral::getNombre).containsExactly("Cristal de Taquiones");
        assertThat(r.pesoOcupado()).isEqualTo(6.0);
        assertThat(r.valorTotal()).isEqualTo(66.0);
    }

    @Test
    @DisplayName("Elige por mayor ratio y sigue probando los siguientes aunque uno no entre")
    void eligePorRatioYSaltaLosQueNoEntran() {
        List<Mineral> minerales = List.of(
                new Mineral("Grande", 8, 80),   // ratio 10, no entra después del primero
                new Mineral("Top", 3, 45),      // ratio 15
                new Mineral("Chico", 2, 10));   // ratio 5

        ResultadoGreedy r = greedy.cargar(minerales, 6);

        assertThat(r.minerales()).extracting(Mineral::getNombre).containsExactly("Top", "Chico");
        assertThat(r.pesoOcupado()).isEqualTo(5.0);
        assertThat(r.valorTotal()).isEqualTo(55.0);
    }

    @Test
    @DisplayName("Capacidad 0: no carga nada")
    void capacidadCero() {
        ResultadoGreedy r = greedy.cargar(CONTRAEJEMPLO_PDF, 0);

        assertThat(r.minerales()).isEmpty();
        assertThat(r.pesoOcupado()).isZero();
        assertThat(r.valorTotal()).isZero();
    }

    @Test
    @DisplayName("Mineral más pesado que toda la bodega: se descarta")
    void mineralQueSuperaLaBodega() {
        ResultadoGreedy r = greedy.cargar(List.of(new Mineral("Asteroide", 50, 5000)), 10);

        assertThat(r.minerales()).isEmpty();
    }

    @Test
    @DisplayName("Todo entra: carga todos los minerales")
    void todoEntra() {
        ResultadoGreedy r = greedy.cargar(CONTRAEJEMPLO_PDF, 100);

        assertThat(r.minerales()).hasSize(3);
        assertThat(r.valorTotal()).isEqualTo(166.0);
    }

    @Test
    @DisplayName("Pesos decimales que llenan la bodega exacto entran (0.1 + 0.2 con capacidad 0.3)")
    void decimalesJustos() {
        ResultadoGreedy r = greedy.cargar(List.of(
                new Mineral("A", 0.1, 10),
                new Mineral("B", 0.2, 10)), 0.3);

        assertThat(r.minerales()).hasSize(2);
    }

    @Test
    @DisplayName("Ratios empatados respetan el orden de entrada (ordenamiento estable)")
    void empatesEstables() {
        ResultadoGreedy r = greedy.cargar(CONTRAEJEMPLO_PDF.subList(1, 3), 5);

        assertThat(r.minerales()).extracting(Mineral::getNombre).containsExactly("Núcleo de Plasma");
    }

    @Test
    @DisplayName("Lista nula devuelve carga vacía; capacidad negativa lanza excepción")
    void casosInvalidos() {
        assertThat(greedy.cargar(null, 10).minerales()).isEmpty();
        assertThatThrownBy(() -> greedy.cargar(CONTRAEJEMPLO_PDF, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("No modifica la lista de entrada")
    void noModificaLaEntrada() {
        List<Mineral> entrada = new java.util.ArrayList<>(CONTRAEJEMPLO_PDF);

        greedy.cargar(entrada, 10);

        assertThat(entrada).containsExactlyElementsOf(CONTRAEJEMPLO_PDF);
    }
}
