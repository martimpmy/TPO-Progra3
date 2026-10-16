package uade.prog3.tpo.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MineralTest {

    @Test
    @DisplayName("Calcula el ratio valor/peso y recorta espacios del nombre")
    void ratioYNombre() {
        Mineral m = new Mineral("  Cristal de Taquiones ", 6, 66);

        assertThat(m.getNombre()).isEqualTo("Cristal de Taquiones");
        assertThat(m.getRatio()).isEqualTo(11.0);
    }

    @Test
    @DisplayName("Acepta valor 0 y pesos decimales")
    void limitesValidos() {
        assertThat(new Mineral("Escoria", 0.5, 0).getRatio()).isZero();
    }

    @Test
    @DisplayName("Rechaza nombre vacío, peso <= 0, valor negativo, NaN e infinitos")
    void validaciones() {
        assertThatThrownBy(() -> new Mineral(null, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("  ", 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("A", 0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("A", -2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("A", Double.NaN, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("A", Double.POSITIVE_INFINITY, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("A", 1, -0.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("A", 1, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mineral("A", 1, Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Dos lotes con mismo nombre, peso y valor son iguales (lo usa la lista de 'quedaron afuera')")
    void igualdad() {
        Mineral a = new Mineral("Titanio", 5, 50);
        Mineral b = new Mineral("Titanio", 5, 50);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(new Mineral("Titanio", 5, 51));
        assertThat(a).isNotEqualTo(null).isNotEqualTo("Titanio");
        assertThat(a.toString()).contains("Titanio");
    }
}
