package uade.prog3.tpo.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import uade.prog3.tpo.model.Estacion;

public interface EstacionRepository extends Neo4jRepository<Estacion, String> {

    /**
     * Cantidad de rutas hiperespaciales NO dirigidas.
     * Cada ruta se persiste en ambos sentidos (a->b y b->a), asi que se
     * cuentan pares distintos de estaciones para no contarla dos veces.
     */
    @Query("""
            MATCH (a:Estacion)-[:RUTA_HIPERESPACIAL]-(b:Estacion)
            WHERE a.id < b.id
            RETURN count(DISTINCT [a.id, b.id])
            """)
    long contarRutas();
}
