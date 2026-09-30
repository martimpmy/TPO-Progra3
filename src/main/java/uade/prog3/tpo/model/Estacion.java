package uade.prog3.tpo.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Vertice del grafo: estacion espacial / baliza orbital.
 * Se persiste en Neo4j como (:Estacion {id, nombre, sector}) y sus rutas
 * salientes como -[:RUTA_HIPERESPACIAL {costoCA}]->. Como cada ruta se guarda
 * en ambos sentidos, las rutas salientes alcanzan para armar el grafo no dirigido.
 */
@Node("Estacion")
public class Estacion {

    @Id
    private String id;
    private String nombre;
    private String sector;

    @Relationship(type = "RUTA_HIPERESPACIAL", direction = Relationship.Direction.OUTGOING)
    private List<RutaHiperespacial> rutas = new ArrayList<>();

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
    public List<RutaHiperespacial> getRutas() { return rutas; }
}
