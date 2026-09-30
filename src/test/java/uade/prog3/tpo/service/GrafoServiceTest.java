package uade.prog3.tpo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.dto.ResumenGrafoDTO;
import uade.prog3.tpo.repository.EstacionRepository;

class GrafoServiceTest {

    private final EstacionRepository repo = mock(EstacionRepository.class);
    private final GrafoService service = new GrafoService(repo);

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
}
