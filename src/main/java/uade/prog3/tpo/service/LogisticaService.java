package uade.prog3.tpo.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.Grafo;
import uade.prog3.tpo.algorithm.RepartoBranchAndBound;
import uade.prog3.tpo.dto.RepartoResponseDTO;
import uade.prog3.tpo.exception.EstacionNoEncontradaException;

/**
 * Hito 11: ruta de reparto óptima con Branch & Bound.
 *
 * El grafo se lee una sola vez; con Dijkstra se arma la matriz de costos mínimos entre la base
 * y las estaciones pedidas (puede no haber ruta directa entre dos de ellas), y sobre esa matriz
 * corre el Branch & Bound. Al final cada tramo se expande con su camino de estaciones intermedias.
 */
@Service
public class LogisticaService {

    private final GrafoService grafoService;
    private final CaminosMinimos caminosMinimos = new CaminosMinimos();
    private final RepartoBranchAndBound branchAndBound = new RepartoBranchAndBound();

    public LogisticaService(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    public RepartoResponseDTO repartoOptimo(String origen, List<String> estacionesRequeridas) {
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro 'origen'");
        }
        if (estacionesRequeridas == null || estacionesRequeridas.isEmpty()) {
            throw new IllegalArgumentException("Se debe enviar al menos una estación a visitar");
        }
        String origenId = origen.trim();

        // Sin repetidas y sin la base (de ahí se sale y ahí se vuelve)
        Set<String> paradas = new LinkedHashSet<>();
        for (String id : estacionesRequeridas) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("La lista de estaciones no puede contener ids vacíos");
            }
            if (!id.trim().equals(origenId)) {
                paradas.add(id.trim());
            }
        }
        if (paradas.isEmpty()) {
            throw new IllegalArgumentException("Se debe pedir al menos una estación distinta del origen");
        }
        if (paradas.size() > RepartoBranchAndBound.MAX_PUNTOS) {
            throw new IllegalArgumentException(
                    "Como máximo se pueden pedir " + RepartoBranchAndBound.MAX_PUNTOS + " estaciones");
        }

        Grafo grafo = grafoService.cargarGrafo();
        List<String> puntos = new ArrayList<>();
        puntos.add(origenId);
        puntos.addAll(paradas);
        for (String id : puntos) {
            if (!grafo.contiene(id)) {
                throw new EstacionNoEncontradaException(id);
            }
        }

        // Costos y caminos mínimos entre cada par de puntos (Dijkstra). Sin ruta: IllegalStateException -> 409
        int p = puntos.size();
        int[][] costos = new int[p][p];
        List<List<List<String>>> caminos = new ArrayList<>();
        for (int i = 0; i < p; i++) {
            List<List<String>> fila = new ArrayList<>();
            for (int j = 0; j < p; j++) {
                if (i == j) {
                    fila.add(List.of(puntos.get(i)));
                    continue;
                }
                CaminosMinimos.ResultadoDijkstra r = caminosMinimos.dijkstra(grafo, puntos.get(i), puntos.get(j));
                costos[i][j] = r.consumoTotal();
                fila.add(r.caminoIds());
            }
            caminos.add(fila);
        }

        RepartoBranchAndBound.ResultadoReparto res = branchAndBound.resolver(costos);

        List<String> recorridoOptimo = new ArrayList<>();
        List<String> recorridoCompleto = new ArrayList<>();
        List<RepartoResponseDTO.TramoDTO> tramos = new ArrayList<>();
        int[] orden = res.recorrido();
        for (int i = 0; i < orden.length; i++) {
            recorridoOptimo.add(nombre(grafo, puntos.get(orden[i])));
        }
        recorridoCompleto.add(nombre(grafo, origenId));
        for (int i = 0; i + 1 < orden.length; i++) {
            List<String> camino = caminos.get(orden[i]).get(orden[i + 1]);
            List<String> nombres = camino.stream().map(id -> nombre(grafo, id)).toList();
            tramos.add(new RepartoResponseDTO.TramoDTO(
                    nombres.get(0), nombres.get(nombres.size() - 1), costos[orden[i]][orden[i + 1]], nombres));
            recorridoCompleto.addAll(nombres.subList(1, nombres.size()));
        }

        double poda = 100.0 * (1.0 - (double) res.nodosGenerados() / res.nodosArbolCompleto());
        RepartoResponseDTO.MetricasDTO metricas = new RepartoResponseDTO.MetricasDTO(
                res.cotaRaiz(),
                res.cotaSuperiorInicial(),
                res.nodosGenerados(),
                res.nodosExpandidos(),
                res.nodosPodados(),
                res.nodosArbolCompleto(),
                String.format(Locale.US, "%.1f%%", Math.max(0.0, poda)));

        return new RepartoResponseDTO(
                "Branch & Bound (Ramificación y Poda)",
                nombre(grafo, origenId),
                paradas.stream().map(id -> nombre(grafo, id)).toList(),
                recorridoOptimo,
                recorridoCompleto,
                tramos,
                res.costoTotal(),
                "Celdas de Antimateria (CA)",
                "Costo acumulado + suma de la arista saliente mínima de cada estación no visitada (optimista/admisible)",
                metricas);
    }

    private String nombre(Grafo grafo, String id) {
        return grafo.nombreDe(grafo.indiceDe(id));
    }
}
