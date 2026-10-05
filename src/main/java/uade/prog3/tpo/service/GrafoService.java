package uade.prog3.tpo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uade.prog3.tpo.algorithm.Grafo;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.dto.GrafoDTO;
import uade.prog3.tpo.dto.RecorridoResponseDTO;
import uade.prog3.tpo.dto.ResumenGrafoDTO;
import uade.prog3.tpo.exception.EstacionNoEncontradaException;
import uade.prog3.tpo.repository.EstacionRepository;
import uade.prog3.tpo.repository.GrafoRepository;
import uade.prog3.tpo.repository.GrafoRepository.FilaRuta;

@Service
public class GrafoService {

    private final EstacionRepository estacionRepository;
    private final GrafoRepository grafoRepository;
    private final Recorridos recorridos = new Recorridos();

    public GrafoService(EstacionRepository estacionRepository, GrafoRepository grafoRepository) {
        this.estacionRepository = estacionRepository;
        this.grafoRepository = grafoRepository;
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

    /**
     * Lee el grafo de Neo4j una única vez y lo arma como lista de adyacencia en memoria.
     * Los algoritmos trabajan solo sobre el Grafo devuelto, sin más consultas a la base.
     *
     * Las rutas se guardan en ambos sentidos, así que cada par de estaciones se agrega
     * una sola vez. Como las filas llegan ordenadas por id, los vecinos de cada estación
     * quedan en orden alfabético de id: los recorridos son determinísticos.
     */
    @Transactional(readOnly = true)
    public Grafo cargarGrafo() {
        List<FilaRuta> filas = grafoRepository.leerGrafo();
        Grafo grafo = new Grafo();

        for (FilaRuta fila : filas) {
            grafo.agregarVertice(fila.id(), fila.nombre());
        }

        Set<String> rutasAgregadas = new HashSet<>();
        for (FilaRuta fila : filas) {
            if (fila.destino() == null || fila.costo() == null || fila.id().equals(fila.destino())) {
                continue;
            }
            boolean idPrimero = fila.id().compareTo(fila.destino()) < 0;
            String clave = idPrimero ? fila.id() + "|" + fila.destino() : fila.destino() + "|" + fila.id();
            if (rutasAgregadas.add(clave)) {
                grafo.agregarArista(fila.id(), fila.destino(), fila.costo());
            }
        }
        return grafo;
    }

    /** Estaciones y rutas del grafo (cada ruta una sola vez), para dibujar el mapa. */
    public GrafoDTO grafo() {
        Grafo grafo = cargarGrafo();
        List<GrafoDTO.EstacionDTO> estaciones = new ArrayList<>();
        List<GrafoDTO.RutaDTO> rutas = new ArrayList<>();
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            estaciones.add(new GrafoDTO.EstacionDTO(grafo.idDe(u), grafo.nombreDe(u)));
            for (Grafo.Arista arista : grafo.vecinos(u)) {
                if (u < arista.destino()) {
                    rutas.add(new GrafoDTO.RutaDTO(grafo.idDe(u), grafo.idDe(arista.destino()), arista.peso()));
                }
            }
        }
        return new GrafoDTO(estaciones, rutas);
    }

    /**
     * Hito 4: recorrido BFS o DFS desde una estación.
     * Sin @Transactional a propósito: los parámetros se validan antes de tocar la base
     * (400 aunque Neo4j no esté disponible) y la única lectura es la de cargarGrafo().
     */
    public RecorridoResponseDTO recorrer(String origen, String tipo) {
        String tipoNormalizado = tipo != null ? tipo.trim().toUpperCase() : "";
        if (!tipoNormalizado.equals("BFS") && !tipoNormalizado.equals("DFS")) {
            throw new IllegalArgumentException(
                    "Tipo de recorrido inválido: '" + tipo + "'. Opciones válidas: 'BFS', 'DFS'.");
        }
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro 'origen'");
        }
        String origenId = origen.trim();

        Grafo grafo = cargarGrafo();
        if (!grafo.contiene(origenId)) {
            throw new EstacionNoEncontradaException(origenId);
        }

        List<String> ordenIds = tipoNormalizado.equals("BFS")
                ? recorridos.bfs(grafo, origenId)
                : recorridos.dfs(grafo, origenId);

        List<String> ordenNombres = ordenIds.stream()
                .map(id -> grafo.nombreDe(grafo.indiceDe(id)))
                .toList();
        String nodoInicial = grafo.nombreDe(grafo.indiceDe(origenId)) + " (" + origenId + ")";

        return new RecorridoResponseDTO(nodoInicial, tipoNormalizado, ordenNombres);
    }
}
