package uade.prog3.tpo.repository;

import java.util.List;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

/**
 * Lee el grafo completo (estaciones + rutas) de Neo4j en UNA sola consulta Cypher.
 * Es la única lectura que hacen los algoritmos de grafos: después trabajan en memoria.
 */
@Repository
public class GrafoRepository {

    /**
     * Una fila por ruta saliente de cada estación. Una estación sin rutas aparece
     * una vez con destino y costo nulos.
     */
    public record FilaRuta(String id, String nombre, String destino, Integer costo) {
    }

    private static final String LEER_GRAFO = """
            MATCH (e:Estacion)
            OPTIONAL MATCH (e)-[r:RUTA_HIPERESPACIAL]-(d:Estacion)
            RETURN DISTINCT e.id AS id, e.nombre AS nombre, d.id AS destino, r.costoCA AS costo
            ORDER BY id, destino
            """;

    private final Neo4jClient neo4jClient;

    public GrafoRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /** Filas ordenadas por id de estación y luego por id de destino. */
    public List<FilaRuta> leerGrafo() {
        return List.copyOf(neo4jClient.query(LEER_GRAFO)
                .fetchAs(FilaRuta.class)
                .mappedBy((tipos, fila) -> new FilaRuta(
                        fila.get("id").asString(),
                        fila.get("nombre").isNull() ? null : fila.get("nombre").asString(),
                        fila.get("destino").isNull() ? null : fila.get("destino").asString(),
                        fila.get("costo").isNull() ? null : fila.get("costo").asNumber().intValue()))
                .all());
    }
}
