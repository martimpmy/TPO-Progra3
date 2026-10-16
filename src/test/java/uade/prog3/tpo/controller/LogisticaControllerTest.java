package uade.prog3.tpo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uade.prog3.tpo.algorithm.GrafoDominio;
import uade.prog3.tpo.service.GrafoService;
import uade.prog3.tpo.service.LogisticaService;

/** Controller + LogisticaService reales; solo la carga del grafo está simulada. */
@WebMvcTest(LogisticaController.class)
@Import(LogisticaService.class)
class LogisticaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GrafoService grafoService;

    @Test
    @DisplayName("POST /api/logistica/reparto-optimo responde 200 con el recorrido, el costo y las métricas de poda")
    void repartoResponde200() throws Exception {
        when(grafoService.cargarGrafo()).thenReturn(GrafoDominio.crear());

        mvc.perform(post("/api/logistica/reparto-optimo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"ALPHA\", \"VEGA\", \"KEPLER\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algoritmo").value("Branch & Bound (Ramificación y Poda)"))
                .andExpect(jsonPath("$.origen").value("Base Solar"))
                .andExpect(jsonPath("$.costoMinimoTotal").value(75))
                .andExpect(jsonPath("$.recorridoOptimo[0]").value("Base Solar"))
                .andExpect(jsonPath("$.recorridoOptimo[4]").value("Base Solar"))
                .andExpect(jsonPath("$.metricas.nodosArbolCompleto").value(16))
                .andExpect(jsonPath("$.metricas.nodosExpandidos").isNumber())
                .andExpect(jsonPath("$.metricas.nodosArbolPodados").isNumber());
    }

    @Test
    @DisplayName("Lista vacía o cuerpo ausente responde 400")
    void pedidoInvalido() throws Exception {
        mvc.perform(post("/api/logistica/reparto-optimo").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/logistica/reparto-optimo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Estación desconocida responde 404")
    void estacionDesconocida() throws Exception {
        when(grafoService.cargarGrafo()).thenReturn(GrafoDominio.crear());

        mvc.perform(post("/api/logistica/reparto-optimo")
                        .param("origen", "PLUTON")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(List.of("\"ALPHA\"").toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404));
    }
}
