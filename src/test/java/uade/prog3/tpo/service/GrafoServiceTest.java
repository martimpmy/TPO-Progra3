package uade.prog3.tpo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Grafo;
import uade.prog3.tpo.dto.RecorridoResponseDTO;
import uade.prog3.tpo.dto.ResumenGrafoDTO;
import uade.prog3.tpo.exception.EstacionNoEncontradaException;
import uade.prog3.tpo.repository.EstacionRepository;
import uade.prog3.tpo.repository.GrafoRepository;
import uade.prog3.tpo.repository.GrafoRepository.FilaRuta;

class GrafoServiceTest {

    private final EstacionRepository repo = mock(EstacionRepository.class);
    private final GrafoRepository grafoRepo = mock(GrafoRepository.class);
    private final GrafoService service = new GrafoService(repo, grafoRepo);

    /** Simula lo que devuelve Neo4j: cada ruta en ambos sentidos, ordenada por id y destino. */
    private static List<FilaRuta> filasDelDominio() {
        String[][] rutas = {
                {"ALPHA", "SIRIUS", "8"}, {"ALPHA", "SOL", "12"}, {"ALPHA", "VEGA", "18"},
                {"CITADEL", "NOVA", "7"}, {"CITADEL", "ORION", "19"},
                {"KEPLER", "NOVA", "14"}, {"KEPLER", "ORION", "9"}, {"KEPLER", "SIRIUS", "15"}, {"KEPLER", "VEGA", "10"},
                {"NOVA", "ORION", "11"}, {"ORION", "VEGA", "22"}, {"SIRIUS", "SOL", "25"}};
        java.util.Map<String, String> nombres = java.util.Map.of(
                "ALPHA", "Alpha Centauri", "CITADEL", "Ciudadela Omega", "KEPLER", "Colonia Kepler",
                "NOVA", "Puesto Nova", "ORION", "Nebulosa Orion", "SIRIUS", "Puerto Sirio",
                "SOL", "Base Solar", "VEGA", "Minas de Vega");
        List<FilaRuta> filas = new ArrayList<>();
        for (String[] r : rutas) {
            filas.add(new FilaRuta(r[0], nombres.get(r[0]), r[1], Integer.valueOf(r[2])));
            filas.add(new FilaRuta(r[1], nombres.get(r[1]), r[0], Integer.valueOf(r[2])));
        }
        filas.sort(java.util.Comparator.comparing(FilaRuta::id).thenComparing(FilaRuta::destino));
        return filas;
    }

    @Test
    void resumenDelGrafoDelDominio() {
        when(repo.count()).thenReturn(8L);
        when(repo.contarRutas()).thenReturn(12L);

        ResumenGrafoDTO r = service.resumen();

        assertThat(r.vertices()).isEqualTo(8);
        assertThat(r.aristas()).isEqualTo(12);
        assertThat(r.estado()).isEqualTo("Grafo cargado y conectado a AuraDB");
    }

    @Test
    void resumenConGrafoVacio() {
        when(repo.count()).thenReturn(0L);
        when(repo.contarRutas()).thenReturn(0L);

        ResumenGrafoDTO r = service.resumen();

        assertThat(r.vertices()).isZero();
        assertThat(r.aristas()).isZero();
        assertThat(r.estado()).isEqualTo("Conectado a AuraDB, grafo vacio");
    }

    @Test
    @DisplayName("cargarGrafo: las 24 filas (rutas en ambos sentidos) dan 8 vértices y 12 aristas")
    void cargarGrafoNoDuplicaRutas() {
        when(grafoRepo.leerGrafo()).thenReturn(filasDelDominio());

        Grafo g = service.cargarGrafo();

        assertThat(g.cantidadVertices()).isEqualTo(8);
        assertThat(g.cantidadAristas()).isEqualTo(12);
        assertThat(g.vecinos(g.indiceDe("KEPLER"))).extracting(a -> g.idDe(a.destino()))
                .containsExactly("NOVA", "ORION", "SIRIUS", "VEGA");
    }

    @Test
    @DisplayName("cargarGrafo: una estación sin rutas queda como vértice aislado")
    void cargarGrafoConEstacionAislada() {
        when(grafoRepo.leerGrafo()).thenReturn(List.of(new FilaRuta("SOL", "Base Solar", null, null)));

        Grafo g = service.cargarGrafo();

        assertThat(g.cantidadVertices()).isEqualTo(1);
        assertThat(g.cantidadAristas()).isZero();
    }

    @Test
    @DisplayName("grafo: devuelve las 8 estaciones y cada una de las 12 rutas una sola vez")
    void grafoParaElMapa() {
        when(grafoRepo.leerGrafo()).thenReturn(filasDelDominio());

        uade.prog3.tpo.dto.GrafoDTO dto = service.grafo();

        assertThat(dto.estaciones()).hasSize(8);
        assertThat(dto.rutas()).hasSize(12).doesNotHaveDuplicates();
        assertThat(dto.rutas()).contains(new uade.prog3.tpo.dto.GrafoDTO.RutaDTO("CITADEL", "NOVA", 7));
    }

    @Test
    @DisplayName("recorrer BFS: devuelve nombres en el orden del informe y lee la base una sola vez")
    void recorrerBfs() {
        when(grafoRepo.leerGrafo()).thenReturn(filasDelDominio());

        RecorridoResponseDTO r = service.recorrer("SOL", "bfs");

        assertThat(r.nodoInicial()).isEqualTo("Base Solar (SOL)");
        assertThat(r.recorrido()).isEqualTo("BFS");
        assertThat(r.ordenExploracion()).containsExactly(
                "Base Solar", "Alpha Centauri", "Puerto Sirio", "Minas de Vega",
                "Colonia Kepler", "Nebulosa Orion", "Puesto Nova", "Ciudadela Omega");
        verify(grafoRepo, times(1)).leerGrafo();
    }

    @Test
    @DisplayName("recorrer DFS: orden en profundidad")
    void recorrerDfs() {
        when(grafoRepo.leerGrafo()).thenReturn(filasDelDominio());

        RecorridoResponseDTO r = service.recorrer("SOL", "DFS");

        assertThat(r.ordenExploracion()).containsExactly(
                "Base Solar", "Alpha Centauri", "Puerto Sirio", "Colonia Kepler",
                "Puesto Nova", "Ciudadela Omega", "Nebulosa Orion", "Minas de Vega");
    }

    @Test
    @DisplayName("recorrer: tipo inválido es IllegalArgument (400) y estación inexistente es 404")
    void recorrerInvalido() {
        when(grafoRepo.leerGrafo()).thenReturn(filasDelDominio());

        assertThatThrownBy(() -> service.recorrer("SOL", "DIJKSTRA")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recorrer(" ", "BFS")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recorrer("PLUTON", "BFS")).isInstanceOf(EstacionNoEncontradaException.class);
    }
}
