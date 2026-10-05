package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grafo no dirigido y ponderado en memoria, representado con lista de adyacencia.
 *
 * Cada vértice (estación) recibe un índice entero 0..V-1 en el orden en que se agrega,
 * así los algoritmos pueden usar arreglos (visitado[], dist[], ...) en lugar de mapas.
 * El service lo arma UNA sola vez a partir de Neo4j; los algoritmos nunca consultan la base.
 *
 * Espacio: O(V + E).
 */
public class Grafo {

    /** Extremo de una arista vista desde un vértice: a quién llega y cuánto cuesta (CA). */
    public record Arista(int destino, int peso) {
    }

    private final List<String> ids = new ArrayList<>();
    private final List<String> nombres = new ArrayList<>();
    private final Map<String, Integer> indicePorId = new HashMap<>();
    private final List<List<Arista>> adyacencia = new ArrayList<>();
    private int cantidadAristas = 0;

    /** Agrega un vértice. Si el id ya existe no hace nada. O(1). */
    public void agregarVertice(String id, String nombre) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del vértice es obligatorio");
        }
        if (indicePorId.containsKey(id)) {
            return;
        }
        indicePorId.put(id, ids.size());
        ids.add(id);
        nombres.add(nombre != null ? nombre : id);
        adyacencia.add(new ArrayList<>());
    }

    /**
     * Agrega una arista no dirigida u–v (queda en la lista de ambos extremos).
     * Los vecinos se recorren en el orden en que se agregaron las aristas.
     */
    public void agregarArista(String idU, String idV, int peso) {
        int u = indiceObligatorio(idU);
        int v = indiceObligatorio(idV);
        if (u == v) {
            throw new IllegalArgumentException("No se admiten lazos: " + idU);
        }
        if (peso <= 0) {
            throw new IllegalArgumentException("El peso de la arista debe ser mayor a 0");
        }
        adyacencia.get(u).add(new Arista(v, peso));
        adyacencia.get(v).add(new Arista(u, peso));
        cantidadAristas++;
    }

    public boolean contiene(String id) {
        return indicePorId.containsKey(id);
    }

    /** Índice del vértice, o -1 si no existe. */
    public int indiceDe(String id) {
        Integer i = indicePorId.get(id);
        return i != null ? i : -1;
    }

    public String idDe(int indice) {
        return ids.get(indice);
    }

    public String nombreDe(int indice) {
        return nombres.get(indice);
    }

    public List<Arista> vecinos(int indice) {
        return adyacencia.get(indice);
    }

    public int cantidadVertices() {
        return ids.size();
    }

    public int cantidadAristas() {
        return cantidadAristas;
    }

    private int indiceObligatorio(String id) {
        int i = indiceDe(id);
        if (i < 0) {
            throw new IllegalArgumentException("No existe el vértice '" + id + "'");
        }
        return i;
    }
}
