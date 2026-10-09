package uade.prog3.tpo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.FloydWarshall;
import uade.prog3.tpo.algorithm.Grafo;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.dto.FloydWarshallResponseDTO;
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

    private final uade.prog3.tpo.algorithm.CaminosMinimos caminosMinimos = new uade.prog3.tpo.algorithm.CaminosMinimos();
    private final uade.prog3.tpo.algorithm.ArbolRecubrimiento arbolRecubrimiento = new uade.prog3.tpo.algorithm.ArbolRecubrimiento();

    /**
     * Hito 5: Dijkstra con reconstrucción de camino desde origen hasta destino.
     */
    public uade.prog3.tpo.dto.DijkstraResponseDTO dijkstra(String origen, String destino) {
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro 'origen'");
        }
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro 'destino'");
        }

        String origenId = origen.trim();
        String destinoId = destino.trim();

        Grafo grafo = cargarGrafo();
        if (!grafo.contiene(origenId)) {
            throw new EstacionNoEncontradaException(origenId);
        }
        if (!grafo.contiene(destinoId)) {
            throw new EstacionNoEncontradaException(destinoId);
        }

        uade.prog3.tpo.algorithm.CaminosMinimos.ResultadoDijkstra res =
                caminosMinimos.dijkstra(grafo, origenId, destinoId);

        return new uade.prog3.tpo.dto.DijkstraResponseDTO(
                res.origenNombre(),
                res.destinoNombre(),
                res.consumoTotal(),
                "Celdas de Antimateria (CA)",
                res.caminoNombres(),
                res.caminoIds()
        );
    }

    /**
     * Hito 5: Árbol Generador Mínimo (MST) por Prim o Kruskal.
     */
    public uade.prog3.tpo.dto.MstResponseDTO mst(String metodo) {
        String metodoNormalizado = metodo != null ? metodo.trim().toLowerCase() : "kruskal";
        if (!metodoNormalizado.equals("prim") && !metodoNormalizado.equals("kruskal")) {
            throw new IllegalArgumentException(
                    "Método de MST desconocido: '" + metodo + "'. Opciones válidas: 'prim', 'kruskal'.");
        }

        Grafo grafo = cargarGrafo();
        uade.prog3.tpo.algorithm.ArbolRecubrimiento.ResultadoMst res =
                metodoNormalizado.equals("prim")
                        ? arbolRecubrimiento.prim(grafo, null)
                        : arbolRecubrimiento.kruskal(grafo);

        List<uade.prog3.tpo.dto.MstResponseDTO.AristaMstDTO> aristasDTO = res.aristas().stream()
                .map(a -> new uade.prog3.tpo.dto.MstResponseDTO.AristaMstDTO(
                        a.origenNombre(),
                        a.destinoNombre(),
                        a.costo()
                ))
                .toList();

        return new uade.prog3.tpo.dto.MstResponseDTO(
                res.algoritmo(),
                res.costoTotal(),
                "Celdas de Antimateria (CA)",
                aristasDTO
        );
    }

    private final FloydWarshall floydWarshall = new FloydWarshall();

    /**
     * Hito 7: Floyd-Warshall entre todos los pares de estaciones, con detección de ciclos
     * negativos y comparativa de estados expandidos contra correr Dijkstra V veces.
     *
     * Las rutas persistidas tienen costo positivo, así que nunca forman un ciclo negativo.
     * Los tres parámetros de simulación (todos o ninguno) agregan una ruta dirigida
     * origen -> destino solo en la matriz en memoria, para probar la detección.
     */
    public FloydWarshallResponseDTO todosContraTodos(String simularOrigen, String simularDestino, Double simularCosto) {
        boolean hayOrigen = simularOrigen != null && !simularOrigen.isBlank();
        boolean hayDestino = simularDestino != null && !simularDestino.isBlank();
        boolean simular = hayOrigen || hayDestino || simularCosto != null;
        if (simular && !(hayOrigen && hayDestino && simularCosto != null)) {
            throw new IllegalArgumentException(
                    "Para simular una ruta se necesitan 'simularOrigen', 'simularDestino' y 'simularCosto'");
        }
        if (simular && !Double.isFinite(simularCosto)) {
            throw new IllegalArgumentException("'simularCosto' debe ser un número finito");
        }

        Grafo grafo = cargarGrafo();
        int v = grafo.cantidadVertices();
        double[][] pesos = FloydWarshall.matrizDeAdyacencia(grafo);

        FloydWarshallResponseDTO.RutaSimuladaDTO rutaSimulada = null;
        if (simular) {
            String origenId = simularOrigen.trim();
            String destinoId = simularDestino.trim();
            if (!grafo.contiene(origenId)) {
                throw new EstacionNoEncontradaException(origenId);
            }
            if (!grafo.contiene(destinoId)) {
                throw new EstacionNoEncontradaException(destinoId);
            }
            if (origenId.equals(destinoId)) {
                throw new IllegalArgumentException("La ruta simulada debe unir dos estaciones distintas");
            }
            pesos[grafo.indiceDe(origenId)][grafo.indiceDe(destinoId)] = simularCosto;
            rutaSimulada = new FloydWarshallResponseDTO.RutaSimuladaDTO(origenId, destinoId, simularCosto);
        }

        FloydWarshall.ResultadoFloyd resultado = floydWarshall.resolver(pesos);

        List<GrafoDTO.EstacionDTO> estaciones = new ArrayList<>();
        for (int i = 0; i < v; i++) {
            estaciones.add(new GrafoDTO.EstacionDTO(grafo.idDe(i), grafo.nombreDe(i)));
        }

        // Con ciclo negativo las distancias no tienen sentido (se pueden bajar sin límite): no se devuelven.
        Double[][] distancias = null;
        if (!resultado.cicloNegativo()) {
            distancias = new Double[v][v];
            for (int i = 0; i < v; i++) {
                for (int j = 0; j < v; j++) {
                    double d = resultado.distancias()[i][j];
                    distancias[i][j] = d == FloydWarshall.INF ? null : d;
                }
            }
        }

        List<String> enCiclo = resultado.verticesEnCicloNegativo().stream().map(grafo::idDe).toList();
        String alerta = resultado.cicloNegativo()
                ? "Anomalía gravitacional: ciclo de costo negativo que afecta a " + String.join(", ", enCiclo)
                        + ". Las distancias mínimas no están definidas."
                : null;

        // Dijkstra no admite pesos negativos: la comparativa se mide sobre el grafo persistido.
        long nodosExpandidos = 0;
        long aristasExaminadas = 0;
        for (int origen = 0; origen < v; origen++) {
            CaminosMinimos.ResultadoDesdeOrigen desdeOrigen = caminosMinimos.dijkstraDesde(grafo, origen);
            nodosExpandidos += desdeOrigen.nodosExpandidos();
            aristasExaminadas += desdeOrigen.aristasExaminadas();
        }

        return new FloydWarshallResponseDTO(
                "Floyd-Warshall (Programación Dinámica)",
                "Celdas de Antimateria (CA)",
                estaciones,
                distancias,
                resultado.cicloNegativo(),
                enCiclo,
                alerta,
                rutaSimulada,
                new FloydWarshallResponseDTO.ComparativaDTO(
                        v,
                        grafo.cantidadAristas(),
                        resultado.estadosExpandidos(),
                        nodosExpandidos + aristasExaminadas,
                        nodosExpandidos,
                        aristasExaminadas
                )
        );
    }
}
