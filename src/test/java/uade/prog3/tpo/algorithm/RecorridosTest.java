package uade.prog3.tpo.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecorridosTest {

    private final Recorridos recorridos = new Recorridos();

    @Test
    @DisplayName("El grafo del dominio tiene 8 vértices y 12 aristas")
    void grafoDelDominio() {
        Grafo g = GrafoDominio.crear();

        assertThat(g.cantidadVertices()).isEqualTo(8);
        assertThat(g.cantidadAristas()).isEqualTo(12);
    }

    @Test
    @DisplayName("BFS desde Base Solar recorre por niveles (orden del informe)")
    void bfsDesdeSol() {
        // Nivel 0: SOL | 1: ALPHA, SIRIUS | 2: VEGA, KEPLER | 3: ORION, NOVA | 4: CITADEL
        assertThat(recorridos.bfs(GrafoDominio.crear(), "SOL"))
                .containsExactly("SOL", "ALPHA", "SIRIUS", "VEGA", "KEPLER", "ORION", "NOVA", "CITADEL");
    }

    @Test
    @DisplayName("DFS desde Base Solar avanza en profundidad antes de retroceder")
    void dfsDesdeSol() {
        // SOL→ALPHA→SIRIUS→KEPLER→NOVA→CITADEL→ORION→VEGA (un único camino, sin retrocesos con hallazgos)
        assertThat(recorridos.dfs(GrafoDominio.crear(), "SOL"))
                .containsExactly("SOL", "ALPHA", "SIRIUS", "KEPLER", "NOVA", "CITADEL", "ORION", "VEGA");
    }

    @Test
    @DisplayName("Desde cualquier estación se visitan las 8 exactamente una vez (grafo conexo)")
    void visitanTodoSinRepetir() {
        Grafo g = GrafoDominio.crear();
        for (int i = 0; i < g.cantidadVertices(); i++) {
            String origen = g.idDe(i);
            assertThat(recorridos.bfs(g, origen)).hasSize(8).doesNotHaveDuplicates().startsWith(origen);
            assertThat(recorridos.dfs(g, origen)).hasSize(8).doesNotHaveDuplicates().startsWith(origen);
        }
    }

    @Test
    @DisplayName("Grafo de un solo vértice: el recorrido es ese vértice")
    void unSoloVertice() {
        Grafo g = new Grafo();
        g.agregarVertice("A", "Única");

        assertThat(recorridos.bfs(g, "A")).containsExactly("A");
        assertThat(recorridos.dfs(g, "A")).containsExactly("A");
    }

    @Test
    @DisplayName("Vértices desconectados del origen no aparecen")
    void componenteInalcanzable() {
        Grafo g = new Grafo();
        for (String id : List.of("A", "B", "C", "D")) {
            g.agregarVertice(id, id);
        }
        g.agregarArista("A", "B", 1);
        g.agregarArista("C", "D", 1);

        assertThat(recorridos.bfs(g, "A")).containsExactly("A", "B");
        assertThat(recorridos.dfs(g, "A")).containsExactly("A", "B");
        assertThat(recorridos.bfs(g, "D")).containsExactly("D", "C");
    }

    @Test
    @DisplayName("BFS y DFS difieren: en un ciclo de 4, BFS toma ambos vecinos antes de alejarse")
    void diferenciaEntreBfsYDfs() {
        // A - B
        // |   |
        // D - C
        Grafo g = new Grafo();
        for (String id : List.of("A", "B", "C", "D")) {
            g.agregarVertice(id, id);
        }
        g.agregarArista("A", "B", 1);
        g.agregarArista("A", "D", 1);
        g.agregarArista("B", "C", 1);
        g.agregarArista("C", "D", 1);

        assertThat(recorridos.bfs(g, "A")).containsExactly("A", "B", "D", "C");
        assertThat(recorridos.dfs(g, "A")).containsExactly("A", "B", "C", "D");
    }

    @Test
    @DisplayName("Camino largo (5.000 vértices): no se repiten vértices ni se cuelga")
    void caminoLargo() {
        Grafo g = new Grafo();
        int n = 5_000;
        for (int i = 0; i < n; i++) {
            g.agregarVertice("V" + i, "V" + i);
        }
        for (int i = 0; i < n - 1; i++) {
            g.agregarArista("V" + i, "V" + (i + 1), 1);
        }

        assertThat(recorridos.bfs(g, "V0")).hasSize(n).endsWith("V" + (n - 1));
        assertThat(recorridos.dfs(g, "V0")).hasSize(n).endsWith("V" + (n - 1));
    }

    @Test
    @DisplayName("Origen inexistente lanza IllegalArgumentException")
    void origenInexistente() {
        Grafo g = GrafoDominio.crear();

        assertThatThrownBy(() -> recorridos.bfs(g, "PLUTON")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recorridos.dfs(g, "PLUTON")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Grafo: rechaza lazos, pesos no positivos y vértices inexistentes; ignora ids repetidos")
    void validacionesDelGrafo() {
        Grafo g = new Grafo();
        g.agregarVertice("A", "A");
        g.agregarVertice("A", "Otra vez");
        g.agregarVertice("B", "B");

        assertThat(g.cantidadVertices()).isEqualTo(2);
        assertThat(g.indiceDe("Z")).isEqualTo(-1);
        assertThatThrownBy(() -> g.agregarArista("A", "A", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> g.agregarArista("A", "B", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> g.agregarArista("A", "Z", 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
