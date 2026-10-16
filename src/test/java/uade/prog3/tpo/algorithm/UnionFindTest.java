package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnionFindTest {

    @Test
    @DisplayName("Inicializa con N componentes disjuntas donde cada elemento es su propia raíz")
    void inicializacionCorrecta() {
        UnionFind uf = new UnionFind(5);
        assertThat(uf.getCantidadComponentes()).isEqualTo(5);
        for (int i = 0; i < 5; i++) {
            assertThat(uf.find(i)).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("Unión de elementos disjuntos reduce la cantidad de componentes y conecta los nodos")
    void unionYConectividad() {
        UnionFind uf = new UnionFind(4);

        boolean u1 = uf.union(0, 1);
        assertThat(u1).isTrue();
        assertThat(uf.conectados(0, 1)).isTrue();
        assertThat(uf.getCantidadComponentes()).isEqualTo(3);

        boolean u2 = uf.union(2, 3);
        assertThat(u2).isTrue();
        assertThat(uf.conectados(2, 3)).isTrue();
        assertThat(uf.conectados(0, 2)).isFalse();
        assertThat(uf.getCantidadComponentes()).isEqualTo(2);

        boolean u3 = uf.union(1, 3);
        assertThat(u3).isTrue();
        assertThat(uf.conectados(0, 3)).isTrue();
        assertThat(uf.getCantidadComponentes()).isEqualTo(1);
    }

    @Test
    @DisplayName("Detección de ciclo: unir elementos que ya pertenecen al mismo conjunto retorna false")
    void deteccionDeCiclos() {
        UnionFind uf = new UnionFind(3);
        assertThat(uf.union(0, 1)).isTrue();
        assertThat(uf.union(1, 2)).isTrue();

        // Arista 0-2 forma un ciclo porque 0 y 2 ya están conectados
        assertThat(uf.union(0, 2)).isFalse();
        assertThat(uf.getCantidadComponentes()).isEqualTo(1);
    }

    @Test
    @DisplayName("Compresión de caminos aplana el árbol al consultar find")
    void compresionDeCaminos() {
        UnionFind uf = new UnionFind(10);
        uf.union(0, 1);
        uf.union(1, 2);
        uf.union(2, 3);

        // find(3) debe retornar la misma raíz que find(0)
        int root = uf.find(0);
        assertThat(uf.find(3)).isEqualTo(root);
    }

    @Test
    @DisplayName("Índices fuera de rango lanzan IndexOutOfBoundsException")
    void indicesInvalidos() {
        UnionFind uf = new UnionFind(3);
        assertThatThrownBy(() -> uf.find(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> uf.find(3))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("Unión por rango: el árbol más bajo se cuelga del más alto, en ambos sentidos")
    void unionPorRango() {
        UnionFind uf = new UnionFind(4);
        uf.union(0, 1);            // rango(0) pasa a 1; 0 es la raíz
        int raiz = uf.find(0);

        // El primer argumento tiene MENOR rango: igual queda colgado de la raíz más alta
        assertThat(uf.union(2, 0)).isTrue();
        assertThat(uf.find(2)).isEqualTo(raiz);

        // El primer argumento tiene MAYOR rango
        assertThat(uf.union(0, 3)).isTrue();
        assertThat(uf.find(3)).isEqualTo(raiz);
        assertThat(uf.getCantidadComponentes()).isEqualTo(1);
    }

    @Test
    @DisplayName("Unir elementos ya conectados devuelve false (así Kruskal detecta el ciclo)")
    void unionRepetidaDetectaCiclo() {
        UnionFind uf = new UnionFind(3);
        uf.union(0, 1);
        uf.union(1, 2);

        assertThat(uf.union(0, 2)).isFalse();
        assertThat(uf.getCantidadComponentes()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tamaño negativo lanza IllegalArgumentException; tamaño 0 es válido")
    void tamanioInvalido() {
        assertThatThrownBy(() -> new UnionFind(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new UnionFind(0).getCantidadComponentes()).isZero();
    }
}
