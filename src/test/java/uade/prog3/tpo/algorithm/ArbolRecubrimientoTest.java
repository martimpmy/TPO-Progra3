package uade.prog3.tpo.algorithm;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento.AristaMst;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento.ResultadoMst;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArbolRecubrimientoTest {

    private ArbolRecubrimiento arbolRecubrimiento;
    private Grafo grafo;

    @BeforeEach
    void setUp() {
        arbolRecubrimiento = new ArbolRecubrimiento();
        grafo = GrafoDominio.crear();
    }

    @Test
    @DisplayName("Kruskal con Union-Find propio coincide con el MST óptimo (72 CA, 7 aristas)")
    void kruskalCalculoManual() {
        ResultadoMst res = arbolRecubrimiento.kruskal(grafo);

        assertThat(res.costoTotal()).isEqualTo(72);
        assertThat(res.aristas()).hasSize(7); // V - 1 = 8 - 1 = 7

        List<Integer> costosAristas = res.aristas().stream().map(AristaMst::costo).toList();
        // Las aristas seleccionadas en orden por Kruskal sobre el grafo:
        assertThat(costosAristas).containsExactly(7, 8, 9, 10, 11, 12, 15);
    }

    @Test
    @DisplayName("Prim produce un Árbol Generador Mínimo de exactamente 72 CA")
    void primCalculoManual() {
        ResultadoMst res = arbolRecubrimiento.prim(grafo, "SOL");

        assertThat(res.costoTotal()).isEqualTo(72);
        assertThat(res.aristas()).hasSize(7);
    }

    @Test
    @DisplayName("Prim y Kruskal producen el mismo costo óptimo total sobre el grafo del dominio (72 CA)")
    void equivalenciaPrimYKruskal() {
        ResultadoMst resPrim = arbolRecubrimiento.prim(grafo, "SOL");
        ResultadoMst resKruskal = arbolRecubrimiento.kruskal(grafo);

        assertThat(resPrim.costoTotal()).isEqualTo(resKruskal.costoTotal()).isEqualTo(72);
    }

    @Test
    @DisplayName("Prim comenzando desde cualquier vértice genera el mismo costo óptimo de 72 CA")
    void primIndependienteDelVerticeInicial() {
        ResultadoMst desdeSolar = arbolRecubrimiento.prim(grafo, "SOL");
        ResultadoMst desdeCitadel = arbolRecubrimiento.prim(grafo, "CITADEL");
        ResultadoMst desdeKepler = arbolRecubrimiento.prim(grafo, "KEPLER");

        assertThat(desdeSolar.costoTotal()).isEqualTo(72);
        assertThat(desdeCitadel.costoTotal()).isEqualTo(72);
        assertThat(desdeKepler.costoTotal()).isEqualTo(72);
    }

    @Test
    @DisplayName("Prim y Kruskal fallan con IllegalStateException si el grafo no es conexo")
    void arbolRecubrimientoGrafoDisconexo() {
        Grafo disconexo = new Grafo();
        disconexo.agregarVertice("A", "Nodo A");
        disconexo.agregarVertice("B", "Nodo B");
        disconexo.agregarVertice("C", "Nodo C");
        disconexo.agregarArista("A", "B", 10);
        // C queda desconectado

        assertThatThrownBy(() -> arbolRecubrimiento.prim(disconexo, "A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no es conexo");

        assertThatThrownBy(() -> arbolRecubrimiento.kruskal(disconexo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no es conexo");
    }

    @Test
    @DisplayName("Manejo de casos borde: grafo con un solo vértice retorna costo 0 y 0 aristas")
    void grafoConUnSoloVertice() {
        Grafo unNodo = new Grafo();
        unNodo.agregarVertice("SOL", "Base Solar");

        ResultadoMst resPrim = arbolRecubrimiento.prim(unNodo, "SOL");
        assertThat(resPrim.costoTotal()).isZero();
        assertThat(resPrim.aristas()).isEmpty();

        ResultadoMst resKruskal = arbolRecubrimiento.kruskal(unNodo);
        assertThat(resKruskal.costoTotal()).isZero();
        assertThat(resKruskal.aristas()).isEmpty();
    }
}
