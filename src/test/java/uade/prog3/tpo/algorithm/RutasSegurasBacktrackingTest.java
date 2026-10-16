package uade.prog3.tpo.algorithm;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RutasSegurasBacktrackingTest {

    private Grafo grafo;
    private RutasSegurasBacktracking backtracking;

    @BeforeEach
    void setUp() {
        grafo = GrafoDominio.crear();
        backtracking = new RutasSegurasBacktracking();
    }

    @Test
    @DisplayName("SOL a CITADEL con maxCosto=70 y maxSaltos=5: encuentra 8 rutas válidas y poda el árbol")
    void solACitadelConPoda() {
        RutasSegurasBacktracking.ResultadoBacktracking res =
                backtracking.buscarRutas(grafo, "SOL", "CITADEL", 70, 5);

        assertThat(res.rutasValidas()).hasSize(8);

        // La ruta más corta (56 CA, 5 saltos) debe figurar en primer lugar
        RutasSegurasBacktracking.RutaValida optima = res.rutasValidas().get(0);
        assertThat(optima.costoTotal()).isEqualTo(56);
        assertThat(optima.saltos()).isEqualTo(5);
        assertThat(optima.caminoIds()).containsExactly("SOL", "ALPHA", "SIRIUS", "KEPLER", "NOVA", "CITADEL");

        // Todas las rutas deben ser simples y respetar las restricciones
        for (RutasSegurasBacktracking.RutaValida r : res.rutasValidas()) {
            assertThat(r.costoTotal()).isLessThanOrEqualTo(70);
            assertThat(r.saltos()).isLessThanOrEqualTo(5);
            assertThat(r.caminoIds().get(0)).isEqualTo("SOL");
            assertThat(r.caminoIds().get(r.caminoIds().size() - 1)).isEqualTo("CITADEL");
            assertThat(new HashSet<>(r.caminoIds())).hasSize(r.caminoIds().size()); // Simple (sin ciclos)
        }

        // Métricas de poda: con poda debe explorar estrictamente menos estados que sin poda
        assertThat(res.metricas().nodosExploradosConPoda())
                .isLessThan(res.metricas().nodosExploradosSinPoda());
        assertThat(res.metricas().ramasPodadas()).isPositive();
        assertThat(res.metricas().porcentajePoda()).isPositive();
    }

    @Test
    @DisplayName("Ejemplo del informe: métricas exactas (41 con poda, 79 sin poda, 19 ramas podadas, 48.1%)")
    void metricasExactasDelEjemplo() {
        RutasSegurasBacktracking.ResultadoBacktracking res =
                backtracking.buscarRutas(grafo, "SOL", "CITADEL", 70, 5);

        assertThat(res.metricas().nodosExploradosConPoda()).isEqualTo(41);
        assertThat(res.metricas().nodosExploradosSinPoda()).isEqualTo(79);
        assertThat(res.metricas().ramasPodadas()).isEqualTo(19);
        assertThat(res.metricas().podaEfectiva()).isEqualTo("48.1%");
    }

    @Test
    @DisplayName("Rutas ordenadas por costo y, a igual costo, por menos saltos (MergeSort propio)")
    void rutasOrdenadasPorCostoYSaltos() {
        List<RutasSegurasBacktracking.RutaValida> rutas =
                backtracking.buscarRutas(grafo, "SOL", "CITADEL", 70, 5).rutasValidas();

        assertThat(rutas).extracting(r -> r.costoTotal() + "/" + r.saltos())
                .containsExactly("56/5", "61/4", "61/5", "63/5", "67/5", "68/4", "68/5", "70/5");
    }

    @Test
    @DisplayName("Búsquedas simultáneas con la misma instancia no mezclan las métricas")
    void busquedasSimultaneasNoMezclanMetricas() throws Exception {
        // Una sola instancia compartida, como en GrafoService
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            List<java.util.concurrent.Future<RutasSegurasBacktracking.MetricasPoda>> futuros = new java.util.ArrayList<>();
            for (int i = 0; i < 200; i++) {
                boolean holgada = i % 2 == 0;
                futuros.add(pool.submit(() -> backtracking.buscarRutas(
                        grafo, "SOL", "CITADEL", holgada ? 999 : 70, holgada ? 10 : 5).metricas()));
            }
            for (int i = 0; i < futuros.size(); i++) {
                RutasSegurasBacktracking.MetricasPoda m = futuros.get(i).get();
                if (i % 2 == 0) {
                    assertThat(m.nodosExploradosConPoda()).isEqualTo(79);
                    assertThat(m.ramasPodadas()).isZero();
                } else {
                    assertThat(m.nodosExploradosConPoda()).isEqualTo(41);
                    assertThat(m.ramasPodadas()).isEqualTo(19);
                }
                assertThat(m.nodosExploradosSinPoda()).isEqualTo(79);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Cota ajustada (maxCosto=56, maxSaltos=5): encuentra solo la ruta óptima y demuestra poda efectiva")
    void cotaAjustadaSoloOptimo() {
        RutasSegurasBacktracking.ResultadoBacktracking res =
                backtracking.buscarRutas(grafo, "SOL", "CITADEL", 56, 5);

        assertThat(res.rutasValidas()).hasSize(1);
        assertThat(res.rutasValidas().get(0).costoTotal()).isEqualTo(56);
        assertThat(res.rutasValidas().get(0).saltos()).isEqualTo(5);

        assertThat(res.metricas().nodosExploradosConPoda())
                .isLessThan(res.metricas().nodosExploradosSinPoda());
        assertThat(res.metricas().porcentajePoda()).isGreaterThan(50.0);
    }

    @Test
    @DisplayName("Cota muy restrictiva (maxCosto=50): no existen rutas y el árbol se poda rápidamente")
    void cotaMuyRestrictivaSinRutas() {
        RutasSegurasBacktracking.ResultadoBacktracking res =
                backtracking.buscarRutas(grafo, "SOL", "CITADEL", 50, 5);

        assertThat(res.rutasValidas()).isEmpty();
        assertThat(res.metricas().nodosExploradosConPoda())
                .isLessThan(res.metricas().nodosExploradosSinPoda());
        assertThat(res.metricas().ramasPodadas()).isPositive();
    }

    @Test
    @DisplayName("Cota holgada (maxCosto=999, maxSaltos=10): encuentra las 26 rutas simples y no poda")
    void cotaHolgadaTodasLasRutas() {
        RutasSegurasBacktracking.ResultadoBacktracking res =
                backtracking.buscarRutas(grafo, "SOL", "CITADEL", 999, 10);

        assertThat(res.rutasValidas()).hasSize(26);
        assertThat(res.metricas().nodosExploradosConPoda())
                .isEqualTo(res.metricas().nodosExploradosSinPoda());
        assertThat(res.metricas().ramasPodadas()).isZero();
        assertThat(res.metricas().porcentajePoda()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Origen igual a destino: ruta trivial de costo 0 y 0 saltos")
    void origenIgualDestino() {
        RutasSegurasBacktracking.ResultadoBacktracking res =
                backtracking.buscarRutas(grafo, "SOL", "SOL", 50, 3);

        assertThat(res.rutasValidas()).hasSize(1);
        RutasSegurasBacktracking.RutaValida r = res.rutasValidas().get(0);
        assertThat(r.caminoIds()).containsExactly("SOL");
        assertThat(r.costoTotal()).isZero();
        assertThat(r.saltos()).isZero();
        assertThat(res.metricas().nodosExploradosConPoda()).isEqualTo(1);
        assertThat(res.metricas().nodosExploradosSinPoda()).isEqualTo(1);
    }

    @Test
    @DisplayName("Grafo con nodo aislado: no encuentra rutas")
    void grafoConNodoAislado() {
        Grafo g = new Grafo();
        g.agregarVertice("A", "Estacion A");
        g.agregarVertice("B", "Estacion B");
        // Sin aristas

        RutasSegurasBacktracking.ResultadoBacktracking res =
                backtracking.buscarRutas(g, "A", "B", 100, 5);

        assertThat(res.rutasValidas()).isEmpty();
        assertThat(res.metricas().nodosExploradosConPoda()).isEqualTo(1);
        assertThat(res.metricas().nodosExploradosSinPoda()).isEqualTo(1);
    }

    @Test
    @DisplayName("Validación de parámetros inválidos lanza IllegalArgumentException")
    void validacionParametros() {
        assertThatThrownBy(() -> backtracking.buscarRutas(null, "SOL", "CITADEL", 70, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> backtracking.buscarRutas(grafo, "", "CITADEL", 70, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> backtracking.buscarRutas(grafo, "SOL", "   ", 70, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> backtracking.buscarRutas(grafo, "INEXISTENTE", "CITADEL", 70, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> backtracking.buscarRutas(grafo, "SOL", "INEXISTENTE", 70, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> backtracking.buscarRutas(grafo, "SOL", "CITADEL", -1, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> backtracking.buscarRutas(grafo, "SOL", "CITADEL", 70, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
