package uade.prog3.tpo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uade.prog3.tpo.dto.ResumenGrafoDTO;
import uade.prog3.tpo.repository.EstacionRepository;

@Service
public class GrafoService {

    private final EstacionRepository estacionRepository;

    public GrafoService(EstacionRepository estacionRepository) {
        this.estacionRepository = estacionRepository;
    }

    @Transactional(readOnly = true)
    public ResumenGrafoDTO resumen() {
        long vertices = estacionRepository.count();
        long aristas = estacionRepository.contarRutas();
        String estado = vertices == 0
                ? "Conectado a AuraDB, grafo vacio"
                : "Grafo cargado y conectado a AuraDB";
        return new ResumenGrafoDTO(vertices, aristas, estado);
    }
}
