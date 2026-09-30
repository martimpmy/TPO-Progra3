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
import uade.prog3.tpo.dto.ResumenGrafoDTO;
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
    void baseCaidaDevuelve503SinStackTrace() throws Exception {
        when(grafoService.resumen())
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        mvc.perform(get("/api/grafo/resumen"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value(503))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
