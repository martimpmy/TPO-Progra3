package uade.prog3.tpo.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uade.prog3.tpo.controller.GrafoController;
import uade.prog3.tpo.controller.MineralController;
import uade.prog3.tpo.controller.NavegacionController;
import uade.prog3.tpo.service.GrafoService;
import uade.prog3.tpo.service.MineralService;

/**
 * Cada tipo de error termina en un ErrorResponseDTO con el código HTTP correcto
 * y sin exponer el stack trace (pauta de la cátedra).
 */
@WebMvcTest(controllers = {GrafoController.class, NavegacionController.class, MineralController.class})
class ManejadorDeErroresTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GrafoService grafoService;

    @MockitoBean
    private MineralService mineralService;

    private static void esError(ResultActions r, int codigo) throws Exception {
        r.andExpect(status().is(codigo))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Destino inalcanzable (Dijkstra) responde 409 con el mensaje del algoritmo")
    void destinoInalcanzableDevuelve409() throws Exception {
        when(grafoService.dijkstra("SOL", "AISLADA"))
                .thenThrow(new IllegalStateException("No existe ruta transitable entre 'SOL' y 'AISLADA'"));

        esError(mvc.perform(get("/api/navegacion/dijkstra").param("origen", "SOL").param("destino", "AISLADA")), 409);
        mvc.perform(get("/api/navegacion/dijkstra").param("origen", "SOL").param("destino", "AISLADA"))
                .andExpect(jsonPath("$.mensaje").value("No existe ruta transitable entre 'SOL' y 'AISLADA'"));
    }

    @Test
    @DisplayName("Grafo no conexo (MST) responde 409")
    void grafoNoConexoDevuelve409() throws Exception {
        when(grafoService.mst("prim"))
                .thenThrow(new IllegalStateException("El grafo no es conexo: no es posible generar un MST completo."));

        esError(mvc.perform(get("/api/red/mst").param("metodo", "prim")), 409);
    }

    @Test
    @DisplayName("Ruta inexistente responde 404")
    void rutaInexistenteDevuelve404() throws Exception {
        esError(mvc.perform(get("/api/no-existe")), 404);
    }

    @Test
    @DisplayName("Método HTTP no soportado responde 405")
    void metodoNoSoportadoDevuelve405() throws Exception {
        esError(mvc.perform(post("/api/grafo/resumen")), 405);
    }

    @Test
    @DisplayName("Cuerpo que no es JSON responde 415")
    void tipoDeContenidoNoSoportadoDevuelve415() throws Exception {
        esError(mvc.perform(post("/api/minerales").contentType(MediaType.TEXT_PLAIN).content("Cristal")), 415);
    }

    @Test
    @DisplayName("Parámetro numérico con texto responde 400")
    void parametroMalTipadoDevuelve400() throws Exception {
        esError(mvc.perform(get("/api/navegacion/rutas-seguras")
                .param("origen", "SOL").param("destino", "CITADEL")
                .param("maxCosto", "setenta").param("maxSaltos", "5")), 400);
    }

    @Test
    @DisplayName("Error no previsto responde 500 genérico, sin detalles internos")
    void errorInesperadoDevuelve500() throws Exception {
        when(grafoService.resumen()).thenThrow(new RuntimeException("NullPointer en línea 42 de GrafoService"));

        esError(mvc.perform(get("/api/grafo/resumen")), 500);
        mvc.perform(get("/api/grafo/resumen"))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor."));
    }
}
