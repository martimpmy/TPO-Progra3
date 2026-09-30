package uade.prog3.tpo.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/** Arista del grafo: ruta hiperespacial con su costo en Celdas de Antimateria (CA). */
@RelationshipProperties
public class RutaHiperespacial {

    @RelationshipId
    private String elementId;

    private int costoCA;

    @TargetNode
    private Estacion destino;

    public RutaHiperespacial() {
    }

    public RutaHiperespacial(int costoCA, Estacion destino) {
        this.costoCA = costoCA;
        this.destino = destino;
    }

    public int getCostoCA() { return costoCA; }
    public Estacion getDestino() { return destino; }
}
