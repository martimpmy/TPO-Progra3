package uade.prog3.tpo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uade.prog3.tpo.repository.MineralRepository;
import uade.prog3.tpo.service.MineralService;

/**
 * Controller + service reales; el repositorio se simula porque el Greedy no usa la base.
 */
@WebMvcTest(CargaController.class)
@Import(MineralService.class)
class CargaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MineralRepository mineralRepository;

    private void esperar400(String body) throws Exception {
        mvc.perform(post("/api/bodega/cargar-greedy").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Ejemplo del PDF: responde 66 CG con el Cristal de Taquiones")
    void ejemploDelPdf() throws Exception {
        mvc.perform(post("/api/bodega/cargar-greedy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "capacidadBodega": 10,
                                  "itemsDisponibles": [
                                    { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66 },
                                    { "nombre": "Núcleo de Plasma", "peso": 5, "valor": 50 },
                                    { "nombre": "Aleación de Titanio", "peso": 5, "valor": 50 }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metodo").value("Greedy (Selección por Ratio Valor/Peso)"))
                .andExpect(jsonPath("$.capacidadBodega").value(10.0))
                .andExpect(jsonPath("$.pesoOcupado").value(6.0))
                .andExpect(jsonPath("$.valorTotalObtenido").value(66.0))
                .andExpect(jsonPath("$.mineralesCargados.length()").value(1))
                .andExpect(jsonPath("$.mineralesCargados[0].nombre").value("Cristal de Taquiones"))
                .andExpect(jsonPath("$.mineralesCargados[0].ratio").value(11.0));
    }

    @Test
    @DisplayName("Suma decimal exacta: 0.1 + 0.2 en capacidad 0.3 entra y se muestra 0.3")
    void decimales() throws Exception {
        mvc.perform(post("/api/bodega/cargar-greedy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "capacidadBodega": 0.3, "itemsDisponibles": [
                                    { "nombre": "A", "peso": 0.1, "valor": 10 },
                                    { "nombre": "B", "peso": 0.2, "valor": 10 } ] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mineralesCargados.length()").value(2))
                .andExpect(jsonPath("$.pesoOcupado").value(0.3));
    }

    @Test
    @DisplayName("Falta capacidadBodega, capacidad negativa, lista vacía, mineral inválido o sin body: 400")
    void pedidosInvalidos() throws Exception {
        esperar400("{\"itemsDisponibles\":[{\"nombre\":\"A\",\"peso\":1,\"valor\":1}]}");
        esperar400("{\"capacidadBodega\":-1,\"itemsDisponibles\":[{\"nombre\":\"A\",\"peso\":1,\"valor\":1}]}");
        esperar400("{\"capacidadBodega\":10,\"itemsDisponibles\":[]}");
        esperar400("{\"capacidadBodega\":10}");
        esperar400("{\"capacidadBodega\":10,\"itemsDisponibles\":[{\"nombre\":\"A\",\"peso\":0,\"valor\":1}]}");
        esperar400("");
    }
}
