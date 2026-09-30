package uade.prog3.tpo.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uade.prog3.tpo.service.MineralService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MineralController.class)
@Import(MineralService.class)
class MineralControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("POST /api/minerales/ordenar con body ordena correctamente por ratio y devuelve enteros en peso y valor")
    void ordenarConBodyPost() throws Exception {
        String jsonBody = """
                [
                  { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66 },
                  { "nombre": "Núcleo de Plasma", "peso": 5, "valor": 50 },
                  { "nombre": "Aleación de Titanio", "peso": 5, "valor": 50 }
                ]
                """;

        mvc.perform(post("/api/minerales/ordenar")
                        .param("algoritmo", "quicksort")
                        .param("criterio", "ratio")
                        .param("direccion", "desc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algoritmoUtilizado").value("QuickSort (Propio)"))
                .andExpect(jsonPath("$.criterio").value("ratio"))
                .andExpect(jsonPath("$.resultado[0].nombre").value("Cristal de Taquiones"))
                .andExpect(jsonPath("$.resultado[0].peso").value(6))
                .andExpect(jsonPath("$.resultado[0].valor").value(66))
                .andExpect(jsonPath("$.resultado[0].ratio").value(11.0))
                .andExpect(jsonPath("$.resultado[1].ratio").value(10.0))
                .andExpect(jsonPath("$.resultado[2].ratio").value(10.0));
    }

    @Test
    @DisplayName("POST /api/minerales/ordenar con body vacío [] devuelve resultado vacío []")
    void ordenarConBodyVacio() throws Exception {
        mvc.perform(post("/api/minerales/ordenar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").isArray())
                .andExpect(jsonPath("$.resultado").isEmpty());
    }

    @Test
    @DisplayName("GET /api/minerales/ordenar sin body utiliza los minerales por defecto")
    void ordenarPorDefectoGet() throws Exception {
        mvc.perform(get("/api/minerales/ordenar")
                        .param("algoritmo", "mergesort")
                        .param("criterio", "peso")
                        .param("direccion", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algoritmoUtilizado").value("MergeSort (Propio)"))
                .andExpect(jsonPath("$.criterio").value("peso"))
                .andExpect(jsonPath("$.resultado[0].peso").value(2)); // Fragmento de Antimateria
    }

    @Test
    @DisplayName("Algoritmo desconocido retorna 400 Bad Request estructurado sin stack trace")
    void algoritmoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/minerales/ordenar")
                        .param("algoritmo", "bogosort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Criterio inválido retorna 400 Bad Request estructurado sin stack trace")
    void criterioInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/minerales/ordenar")
                        .param("criterio", "color"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Dirección inválida retorna 400 Bad Request estructurado sin stack trace")
    void direccionInvalidaRetorna400() throws Exception {
        mvc.perform(get("/api/minerales/ordenar")
                        .param("direccion", "cualquiercosa"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("Dirección inválida: 'cualquiercosa'. Opciones válidas: 'desc', 'asc'."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/seleccion/quicksort responde lista de minerales para compatibilidad con scaffold")
    void compatibilidadScaffold() throws Exception {
        mvc.perform(get("/api/seleccion/quicksort")
                        .param("criterio", "ratio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nombre").isNotEmpty());
    }
}
