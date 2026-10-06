package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.CaminosMinimos.ResultadoDijkstra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaminosMinimosTest {

    private CaminosMinimos caminosMinimos;
    private Grafo grafo;

    @BeforeEach
    void setUp() {
        caminosMinimos = new CaminosMinimos();
        grafo = GrafoDominio.crear();
    }

    @Test
    @DisplayName("Dijkstra desde Base Solar hasta Ciudadela Omega coincide con cálculo manual (56 CA)")
    void dijkstraRutaSolarACitadel() {
        ResultadoDijkstra res = caminosMinimos.dijkstra(grafo, "SOL", "CITADEL");

        assertThat(res.consumoTotal()).isEqualTo(56);
        assertThat(res.caminoIds()).containsExactly(
                "SOL", "ALPHA", "SIRIUS", "KEPLER", "NOVA", "CITADEL"
        );
        assertThat(res.caminoNombres()).containsExactly(
                "Base Solar", "Alpha Centauri", "Puerto Sirio", "Colonia Kepler", "Puesto Nova", "Ciudadela Omega"
        );
    }

    @Test
    @DisplayName("Dijkstra verifica todos los costos mínimos desde Base Solar documentados en el informe")
    void dijkstraTodosLosDestinosDesdeSolar() {
        // V1 -> V2 (ALPHA): 12 CA
        assertThat(caminosMinimos.dijkstra(grafo, "SOL", "ALPHA").consumoTotal()).isEqualTo(12);

        // V1 -> V3 (SIRIUS): 20 CA (vía ALPHA: 12 + 8, mejor que directa 25)
        assertThat(caminosMinimos.dijkstra(grafo, "SOL", "SIRIUS").consumoTotal()).isEqualTo(20);

        // V1 -> V4 (VEGA): 30 CA (vía ALPHA: 12 + 18)
        assertThat(caminosMinimos.dijkstra(grafo, "SOL", "VEGA").consumoTotal()).isEqualTo(30);

        // V1 -> V5 (KEPLER): 35 CA (vía SIRIUS: 20 + 15)
        assertThat(caminosMinimos.dijkstra(grafo, "SOL", "KEPLER").consumoTotal()).isEqualTo(35);

        // V1 -> V6 (ORION): 44 CA (vía KEPLER: 35 + 9)
        assertThat(caminosMinimos.dijkstra(grafo, "SOL", "ORION").consumoTotal()).isEqualTo(44);

        // V1 -> V7 (NOVA): 49 CA (vía KEPLER: 35 + 14)
        assertThat(caminosMinimos.dijkstra(grafo, "SOL", "NOVA").consumoTotal()).isEqualTo(49);

        // V1 -> V8 (CITADEL): 56 CA (vía NOVA: 49 + 7)
        assertThat(caminosMinimos.dijkstra(grafo, "SOL", "CITADEL").consumoTotal()).isEqualTo(56);
    }

    @Test
    @DisplayName("Dijkstra con origen igual al destino retorna costo cero y camino de un solo nodo")
    void dijkstraMismoOrigenYDestino() {
        ResultadoDijkstra res = caminosMinimos.dijkstra(grafo, "SOL", "SOL");

        assertThat(res.consumoTotal()).isZero();
        assertThat(res.caminoIds()).containsExactly("SOL");
    }

    @Test
    @DisplayName("Dijkstra falla con mensaje claro si el destino es inalcanzable (grafo disconexo)")
    void dijkstraDestinoInalcanzable() {
        Grafo disconexo = new Grafo();
        disconexo.agregarVertice("A", "Nodo A");
        disconexo.agregarVertice("B", "Nodo B");
        disconexo.agregarVertice("C", "Nodo C");
        disconexo.agregarArista("A", "B", 10);
        // C queda aislado

        assertThatThrownBy(() -> caminosMinimos.dijkstra(disconexo, "A", "C"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe ruta transitable");
    }

    @Test
    @DisplayName("Dijkstra lanza IllegalArgumentException si se consultan vértices inexistentes")
    void dijkstraVerticesInexistentes() {
        assertThatThrownBy(() -> caminosMinimos.dijkstra(grafo, "INEXISTENTE", "SOL"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> caminosMinimos.dijkstra(grafo, "SOL", "DESCONOCIDO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
