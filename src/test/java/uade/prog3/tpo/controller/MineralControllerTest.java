package uade.prog3.tpo.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import uade.prog3.tpo.model.Mineral;
import uade.prog3.tpo.repository.MineralRepository;
import uade.prog3.tpo.service.MineralService;

/**
 * Controller + service reales; solo el repositorio de Neo4j esta simulado.
 */
@WebMvcTest(MineralController.class)
@Import(MineralService.class)
class MineralControllerTest {

    private static final String EJEMPLO_PDF = """
            [
              { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66 },
              { "nombre": "Núcleo de Plasma", "peso": 5, "valor": 50 },
              { "nombre": "Aleación de Titanio", "peso": 5, "valor": 50 }
            ]
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MineralRepository mineralRepository;

    @Test
    @DisplayName("Ordenar con body: ordena esa lista por ratio sin tocar la base")
    void ordenarConBody() throws Exception {
        mvc.perform(post("/api/minerales/ordenar")
                        .param("algoritmo", "quicksort")
                        .param("criterio", "ratio")
                        .param("direccion", "desc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EJEMPLO_PDF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algoritmoUtilizado").value("QuickSort (Propio)"))
                .andExpect(jsonPath("$.criterio").value("ratio"))
                .andExpect(jsonPath("$.resultado[0].nombre").value("Cristal de Taquiones"))
                .andExpect(jsonPath("$.resultado[0].peso").value(6.0))
                .andExpect(jsonPath("$.resultado[0].ratio").value(11.0))
                .andExpect(jsonPath("$.resultado[0].id").doesNotExist())
                .andExpect(jsonPath("$.resultado[1].ratio").value(10.0))
                .andExpect(jsonPath("$.resultado[2].ratio").value(10.0));

        verify(mineralRepository, never()).findAll();
    }

    @Test
    @DisplayName("Ordenar acepta pesos y valores con decimales")
    void ordenarConDecimales() throws Exception {
        mvc.perform(post("/api/minerales/ordenar")
                        .param("criterio", "peso")
                        .param("direccion", "asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [ { "nombre": "A", "peso": 2.75, "valor": 10 },
                                  { "nombre": "B", "peso": 0.5,  "valor": 3.5 } ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado[0].nombre").value("B"))
                .andExpect(jsonPath("$.resultado[0].peso").value(0.5))
                .andExpect(jsonPath("$.resultado[0].ratio").value(7.0))
                .andExpect(jsonPath("$.resultado[1].peso").value(2.75));
    }

    @Test
    @DisplayName("Ordenar sin body: ordena los minerales persistidos en Neo4j")
    void ordenarSinBodyUsaLaBase() throws Exception {
        when(mineralRepository.findAll()).thenReturn(List.of(
                new Mineral("Lingote de Iridio", 4, 44),
                new Mineral("Fragmento de Antimateria", 2, 30)));

        mvc.perform(post("/api/minerales/ordenar")
                        .param("algoritmo", "mergesort")
                        .param("criterio", "peso")
                        .param("direccion", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algoritmoUtilizado").value("MergeSort (Propio)"))
                .andExpect(jsonPath("$.resultado[0].nombre").value("Fragmento de Antimateria"))
                .andExpect(jsonPath("$.resultado[1].nombre").value("Lingote de Iridio"));
    }

    @Test
    @DisplayName("Ordenar con body vacío [] devuelve resultado vacío")
    void ordenarConBodyVacio() throws Exception {
        mvc.perform(post("/api/minerales/ordenar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").isEmpty());
    }

    @Test
    @DisplayName("Algoritmo, criterio o dirección inválidos devuelven 400 sin stack trace")
    void parametrosInvalidos() throws Exception {
        mvc.perform(post("/api/minerales/ordenar").param("algoritmo", "bogosort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(post("/api/minerales/ordenar").param("criterio", "color"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/minerales/ordenar").param("direccion", "cualquiercosa"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Dirección inválida: 'cualquiercosa'. Opciones válidas: 'desc', 'asc'."));
    }

    @Test
    @DisplayName("Mineral con peso 0, sin nombre o sin valor devuelve 400")
    void mineralInvalido() throws Exception {
        for (String body : List.of(
                "[{\"nombre\":\"X\",\"peso\":0,\"valor\":10}]",
                "[{\"nombre\":\"\",\"peso\":1,\"valor\":10}]",
                "[{\"nombre\":\"X\",\"peso\":1}]")) {
            mvc.perform(post("/api/minerales/ordenar").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(400));
        }
    }

    @Test
    @DisplayName("JSON mal formado devuelve 400, no 500")
    void jsonMalFormado() throws Exception {
        mvc.perform(post("/api/minerales/ordenar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @Test
    @DisplayName("POST /api/minerales persiste y devuelve 201 con los minerales guardados")
    void crearPersiste() throws Exception {
        when(mineralRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/minerales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EJEMPLO_PDF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Cristal de Taquiones"))
                .andExpect(jsonPath("$[0].ratio").value(11.0));

        verify(mineralRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("POST /api/minerales con un mineral inválido no guarda nada")
    void crearInvalidoNoPersiste() throws Exception {
        mvc.perform(post("/api/minerales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"nombre\":\"Ok\",\"peso\":1,\"valor\":1},{\"nombre\":\"Mal\",\"peso\":-2,\"valor\":1}]"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/minerales").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest());

        verify(mineralRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("GET /api/minerales lista los persistidos")
    void listar() throws Exception {
        when(mineralRepository.findAll()).thenReturn(List.of(new Mineral("Celdas de Helio-3", 3, 27)));

        mvc.perform(get("/api/minerales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Celdas de Helio-3"))
                .andExpect(jsonPath("$[0].ratio").value(9.0));
    }

    @Test
    @DisplayName("DELETE de un mineral inexistente devuelve 404")
    void eliminarInexistente() throws Exception {
        when(mineralRepository.existsById("nope")).thenReturn(false);

        mvc.perform(delete("/api/minerales/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    @DisplayName("DELETE de un mineral existente devuelve 204")
    void eliminarExistente() throws Exception {
        when(mineralRepository.existsById("m1")).thenReturn(true);

        mvc.perform(delete("/api/minerales/m1"))
                .andExpect(status().isNoContent());

        verify(mineralRepository).deleteById("m1");
    }
}
