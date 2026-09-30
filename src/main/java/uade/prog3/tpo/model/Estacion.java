package uade.prog3.tpo.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Vertice del grafo: estacion espacial / baliza orbital.
 * Se persiste en Neo4j como (:Estacion {id, nombre, sector}).
 */
@Node("Estacion")
public class Estacion {

    @Id
    private String id;
    private String nombre;
    private String sector;

    public Estacion() {
    }

    public Estacion(String id, String nombre, String sector) {
        this.id = id;
        this.nombre = nombre;
        this.sector = sector;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getSector() { return sector; }
}
