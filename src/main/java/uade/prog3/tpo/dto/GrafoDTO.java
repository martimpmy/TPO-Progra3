package uade.prog3.tpo.dto;

import java.util.List;

/** Grafo completo para dibujar el mapa: cada ruta no dirigida aparece una sola vez. */
public record GrafoDTO(List<EstacionDTO> estaciones, List<RutaDTO> rutas) {

    public record EstacionDTO(String id, String nombre) {
    }

    public record RutaDTO(String origen, String destino, int costoCA) {
    }
}
