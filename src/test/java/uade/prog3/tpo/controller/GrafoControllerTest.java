package uade.prog3.tpo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import uade.prog3.tpo.dto.RecorridoResponseDTO;
import uade.prog3.tpo.dto.ResumenGrafoDTO;
import uade.prog3.tpo.exception.EstacionNoEncontradaException;
import uade.prog3.tpo.service.GrafoService;

@WebMvcTest(GrafoController.class)
class GrafoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GrafoService grafoService;

    @Test
    void resumenDevuelve200ConConteos() throws Exception {
        when(grafoService.resumen())
                .thenReturn(new ResumenGrafoDTO(8, 12, "Grafo cargado y conectado a AuraDB"));

        mvc.perform(get("/api/grafo/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vertices").value(8))
                .andExpect(jsonPath("$.aristas").value(12))
                .andExpect(jsonPath("$.estado").value("Grafo cargado y conectado a AuraDB"));
    }

    @Test
    void recorrerDevuelve200() throws Exception {
        when(grafoService.recorrer("SOL", "DFS")).thenReturn(new RecorridoResponseDTO(
                "Base Solar (SOL)", "DFS", List.of("Base Solar", "Alpha Centauri")));

        mvc.perform(get("/api/grafo/recorrer").param("origen", "SOL").param("tipo", "DFS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodoInicial").value("Base Solar (SOL)"))
                .andExpect(jsonPath("$.recorrido").value("DFS"))
                .andExpect(jsonPath("$.ordenExploracion[1]").value("Alpha Centauri"));
    }

    @Test
    void recorrerUsaBfsPorDefecto() throws Exception {
        when(grafoService.recorrer("SOL", "BFS")).thenReturn(new RecorridoResponseDTO(
                "Base Solar (SOL)", "BFS", List.of("Base Solar")));

        mvc.perform(get("/api/grafo/recorrer").param("origen", "SOL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorrido").value("BFS"));
    }

    @Test
    void recorrerSinOrigenDevuelve400() throws Exception {
        mvc.perform(get("/api/grafo/recorrer").param("tipo", "BFS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @Test
    void recorrerEstacionInexistenteDevuelve404() throws Exception {
        when(grafoService.recorrer("PLUTON", "BFS")).thenThrow(new EstacionNoEncontradaException("PLUTON"));

        mvc.perform(get("/api/grafo/recorrer").param("origen", "PLUTON"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la estacion con id 'PLUTON'"));
    }

    @Test
    void recorrerTipoInvalidoDevuelve400() throws Exception {
        when(grafoService.recorrer("SOL", "XYZ")).thenThrow(new IllegalArgumentException("Tipo de recorrido inválido"));

        mvc.perform(get("/api/grafo/recorrer").param("origen", "SOL").param("tipo", "XYZ"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void baseCaidaDevuelve503SinStackTrace() throws Exception {
        when(grafoService.resumen())
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        mvc.perform(get("/api/grafo/resumen"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value(503))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
