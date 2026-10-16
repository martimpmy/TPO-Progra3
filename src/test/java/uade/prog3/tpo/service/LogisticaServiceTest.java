package uade.prog3.tpo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Grafo;
import uade.prog3.tpo.algorithm.GrafoDominio;
import uade.prog3.tpo.dto.RepartoResponseDTO;
import uade.prog3.tpo.exception.EstacionNoEncontradaException;

class LogisticaServiceTest {

    private final GrafoService grafoService = mock(GrafoService.class);
    private final LogisticaService service = new LogisticaService(grafoService);

    @Test
    @DisplayName("ALPHA, VEGA y KEPLER desde SOL: 75 CA, con el regreso por estaciones intermedias")
    void repartoDelEjemplo() {
        when(grafoService.cargarGrafo()).thenReturn(GrafoDominio.crear());

        RepartoResponseDTO r = service.repartoOptimo("SOL", List.of("ALPHA", "VEGA", "KEPLER"));

        assertThat(r.costoMinimoTotal()).isEqualTo(75);
        assertThat(r.recorridoOptimo()).startsWith("Base Solar").endsWith("Base Solar").hasSize(5);
        assertThat(r.recorridoOptimo()).contains("Alpha Centauri", "Minas de Vega", "Colonia Kepler");
        // El recorrido completo pasa por estaciones que no se pidieron (no hay ruta directa KEPLER–SOL)
        assertThat(r.recorridoCompleto()).contains("Puerto Sirio").startsWith("Base Solar").endsWith("Base Solar");
        assertThat(r.tramos()).hasSize(4);
        assertThat(r.tramos().stream().mapToInt(RepartoResponseDTO.TramoDTO::costo).sum()).isEqualTo(75);
        assertThat(r.metricas().nodosArbolCompleto()).isEqualTo(16);
        verify(grafoService, times(1)).cargarGrafo();
    }

    @Test
    @DisplayName("El recorrido completo es una cadena de rutas reales del grafo cuyo costo suma el total")
    void recorridoCompletoUsaRutasReales() {
        Grafo g = GrafoDominio.crear();
        when(grafoService.cargarGrafo()).thenReturn(g);

        RepartoResponseDTO r = service.repartoOptimo("SOL", List.of("CITADEL", "VEGA", "SIRIUS"));

        int suma = 0;
        List<String> camino = r.recorridoCompleto();
        for (int i = 0; i + 1 < camino.size(); i++) {
            int u = indicePorNombre(g, camino.get(i));
            int v = indicePorNombre(g, camino.get(i + 1));
            Grafo.Arista arista = g.vecinos(u).stream().filter(a -> a.destino() == v).findFirst()
                    .orElseThrow(() -> new AssertionError("no hay ruta directa entre consecutivos"));
            suma += arista.peso();
        }
        assertThat(suma).isEqualTo(r.costoMinimoTotal());
    }

    private static int indicePorNombre(Grafo g, String nombre) {
        for (int i = 0; i < g.cantidadVertices(); i++) {
            if (g.nombreDe(i).equals(nombre)) {
                return i;
            }
        }
        throw new AssertionError("estación desconocida " + nombre);
    }

    @Test
    @DisplayName("Ignora estaciones repetidas y el propio origen en la lista pedida")
    void ignoraRepetidasYOrigen() {
        when(grafoService.cargarGrafo()).thenReturn(GrafoDominio.crear());

        RepartoResponseDTO r = service.repartoOptimo("SOL", List.of("ALPHA", "SOL", "ALPHA"));

        assertThat(r.estacionesRequeridas()).containsExactly("Alpha Centauri");
        assertThat(r.costoMinimoTotal()).isEqualTo(24);
    }

    @Test
    @DisplayName("Lista vacía, ids vacíos u origen vacío son 400 y se validan antes de leer la base")
    void validaciones() {
        assertThatThrownBy(() -> service.repartoOptimo("SOL", List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.repartoOptimo("SOL", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.repartoOptimo("SOL", Arrays.asList("ALPHA", " "))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.repartoOptimo(" ", List.of("ALPHA"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.repartoOptimo("SOL", List.of("SOL"))).isInstanceOf(IllegalArgumentException.class);
        verify(grafoService, never()).cargarGrafo();
    }

    @Test
    @DisplayName("Estación desconocida (en la lista o como origen) es 404")
    void estacionDesconocida() {
        when(grafoService.cargarGrafo()).thenReturn(GrafoDominio.crear());

        assertThatThrownBy(() -> service.repartoOptimo("SOL", List.of("PLUTON"))).isInstanceOf(EstacionNoEncontradaException.class);
        assertThatThrownBy(() -> service.repartoOptimo("PLUTON", List.of("ALPHA"))).isInstanceOf(EstacionNoEncontradaException.class);
    }

    @Test
    @DisplayName("Estación sin rutas: IllegalStateException (la API responde 409)")
    void estacionInalcanzable() {
        Grafo g = GrafoDominio.crear();
        g.agregarVertice("AISLADA", "Estación Aislada");
        when(grafoService.cargarGrafo()).thenReturn(g);

        assertThatThrownBy(() -> service.repartoOptimo("SOL", List.of("ALPHA", "AISLADA")))
                .isInstanceOf(IllegalStateException.class);
    }
}
