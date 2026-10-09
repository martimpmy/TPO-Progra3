package uade.prog3.tpo.controller;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uade.prog3.tpo.dto.DijkstraResponseDTO;
import uade.prog3.tpo.dto.FloydWarshallResponseDTO;
import uade.prog3.tpo.dto.GrafoDTO;
import uade.prog3.tpo.dto.MstResponseDTO;
import uade.prog3.tpo.dto.MstResponseDTO.AristaMstDTO;
import uade.prog3.tpo.exception.EstacionNoEncontradaException;
import uade.prog3.tpo.service.GrafoService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NavegacionController.class)
class NavegacionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GrafoService grafoService;

    @Test
    @DisplayName("GET /api/navegacion/dijkstra responde 200 OK con consumo óptimo y camino reconstruido")
    void dijkstraResponde200Ok() throws Exception {
        DijkstraResponseDTO mockDto = new DijkstraResponseDTO(
                "Base Solar",
                "Ciudadela Omega",
                56,
                "Celdas de Antimateria (CA)",
                List.of("Base Solar", "Alpha Centauri", "Puerto Sirio", "Colonia Kepler", "Puesto Nova", "Ciudadela Omega"),
                List.of("SOL", "ALPHA", "SIRIUS", "KEPLER", "NOVA", "CITADEL")
        );

        when(grafoService.dijkstra("SOL", "CITADEL")).thenReturn(mockDto);

        mvc.perform(get("/api/navegacion/dijkstra")
                        .param("origen", "SOL")
                        .param("destino", "CITADEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("Base Solar"))
                .andExpect(jsonPath("$.destino").value("Ciudadela Omega"))
                .andExpect(jsonPath("$.consumoTotal").value(56))
                .andExpect(jsonPath("$.unidad").value("Celdas de Antimateria (CA)"))
                .andExpect(jsonPath("$.caminoReconstruido[0]").value("Base Solar"))
                .andExpect(jsonPath("$.caminoReconstruido[5]").value("Ciudadela Omega"))
                .andExpect(jsonPath("$.caminoIds[0]").value("SOL"))
                .andExpect(jsonPath("$.caminoIds[5]").value("CITADEL"));
    }

    @Test
    @DisplayName("GET /api/navegacion/dijkstra responde 404 estructurado cuando el nodo no existe")
    void dijkstraNodoInexistenteRetorna404() throws Exception {
        when(grafoService.dijkstra("INEXISTENTE", "CITADEL"))
                .thenThrow(new EstacionNoEncontradaException("INEXISTENTE"));

        mvc.perform(get("/api/navegacion/dijkstra")
                        .param("origen", "INEXISTENTE")
                        .param("destino", "CITADEL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.mensaje").value("No existe la estacion con id 'INEXISTENTE'"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/red/mst responde 200 OK con costo total 72 CA y aristas seleccionadas")
    void mstResponde200Ok() throws Exception {
        MstResponseDTO mockMst = new MstResponseDTO(
                "Kruskal (con Union-Find propio)",
                72,
                "Celdas de Antimateria (CA)",
                List.of(
                        new AristaMstDTO("Puesto Nova", "Ciudadela Omega", 7),
                        new AristaMstDTO("Alpha Centauri", "Puerto Sirio", 8),
                        new AristaMstDTO("Colonia Kepler", "Nebulosa Orión", 9),
                        new AristaMstDTO("Minas de Vega", "Colonia Kepler", 10),
                        new AristaMstDTO("Puesto Nova", "Nebulosa Orión", 11),
                        new AristaMstDTO("Base Solar", "Alpha Centauri", 12),
                        new AristaMstDTO("Puerto Sirio", "Colonia Kepler", 15)
                )
        );

        when(grafoService.mst("kruskal")).thenReturn(mockMst);

        mvc.perform(get("/api/red/mst").param("metodo", "kruskal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algoritmo").value("Kruskal (con Union-Find propio)"))
                .andExpect(jsonPath("$.costoTotalMST").value(72))
                .andExpect(jsonPath("$.aristasSeleccionadas").isArray())
                .andExpect(jsonPath("$.aristasSeleccionadas[0].costo").value(7));
    }

    @Test
    @DisplayName("GET /api/red/mst responde 400 Bad Request estructurado ante método desconocido")
    void mstMetodoInvalidoRetorna400() throws Exception {
        when(grafoService.mst("desconocido"))
                .thenThrow(new IllegalArgumentException("Método de MST desconocido: 'desconocido'. Opciones válidas: 'prim', 'kruskal'."));

        mvc.perform(get("/api/red/mst").param("metodo", "desconocido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/navegacion/todos-contra-todos responde 200 OK con la matriz y la comparativa")
    void todosContraTodosResponde200Ok() throws Exception {
        FloydWarshallResponseDTO mockDto = new FloydWarshallResponseDTO(
                "Floyd-Warshall (Programación Dinámica)",
                "Celdas de Antimateria (CA)",
                List.of(new GrafoDTO.EstacionDTO("ALPHA", "Alpha Centauri"), new GrafoDTO.EstacionDTO("SOL", "Base Solar")),
                new Double[][]{{0.0, 12.0}, {12.0, null}},
                false,
                List.of(),
                null,
                null,
                new FloydWarshallResponseDTO.ComparativaDTO(8, 12, 512, 256, 64, 192)
        );

        when(grafoService.todosContraTodos(null, null, null)).thenReturn(mockDto);

        mvc.perform(get("/api/navegacion/todos-contra-todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaciones[1].id").value("SOL"))
                .andExpect(jsonPath("$.distancias[0][1]").value(12.0))
                .andExpect(jsonPath("$.distancias[1][1]").isEmpty())
                .andExpect(jsonPath("$.cicloNegativo").value(false))
                .andExpect(jsonPath("$.comparativa.estadosFloydWarshall").value(512))
                .andExpect(jsonPath("$.comparativa.estadosDijkstraVVeces").value(256));
    }

    @Test
    @DisplayName("GET /api/navegacion/todos-contra-todos pasa la ruta simulada y devuelve la alerta de ciclo negativo")
    void todosContraTodosConRutaSimulada() throws Exception {
        FloydWarshallResponseDTO mockDto = new FloydWarshallResponseDTO(
                "Floyd-Warshall (Programación Dinámica)",
                "Celdas de Antimateria (CA)",
                List.of(new GrafoDTO.EstacionDTO("NOVA", "Puesto Nova"), new GrafoDTO.EstacionDTO("ORION", "Nebulosa Orion")),
                null,
                true,
                List.of("NOVA", "ORION"),
                "Anomalía gravitacional",
                new FloydWarshallResponseDTO.RutaSimuladaDTO("NOVA", "ORION", -30),
                new FloydWarshallResponseDTO.ComparativaDTO(8, 12, 512, 256, 64, 192)
        );

        when(grafoService.todosContraTodos("NOVA", "ORION", -30.0)).thenReturn(mockDto);

        mvc.perform(get("/api/navegacion/todos-contra-todos")
                        .param("simularOrigen", "NOVA")
                        .param("simularDestino", "ORION")
                        .param("simularCosto", "-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cicloNegativo").value(true))
                .andExpect(jsonPath("$.distancias").isEmpty())
                .andExpect(jsonPath("$.estacionesEnCicloNegativo[0]").value("NOVA"))
                .andExpect(jsonPath("$.rutaSimulada.costoCA").value(-30.0));
    }

    @Test
    @DisplayName("GET /api/navegacion/todos-contra-todos responde 400 si simularCosto no es un número")
    void todosContraTodosCostoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/navegacion/todos-contra-todos")
                        .param("simularOrigen", "NOVA")
                        .param("simularDestino", "ORION")
                        .param("simularCosto", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
