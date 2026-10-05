package uade.prog3.tpo.seed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.repository.EstacionRepository;

/**
 * Carga el grafo semilla del dominio (8 estaciones, 12 rutas) al iniciar,
 * solo si la base no tiene estaciones. Se desactiva con app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class CargaInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private final EstacionRepository estacionRepository;
    private final Neo4jClient neo4jClient;

    public CargaInicial(EstacionRepository estacionRepository, Neo4jClient neo4jClient) {
        this.estacionRepository = estacionRepository;
        this.neo4jClient = neo4jClient;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        try {
            if (estacionRepository.count() > 0) {
                log.info("La base ya tiene estaciones, se omite la carga inicial.");
                return;
            }
            String cypher = new ClassPathResource("db/seed-dominio.cypher")
                    .getContentAsString(StandardCharsets.UTF_8);
            neo4jClient.query(cypher).run();
            log.info("Grafo semilla cargado: {} estaciones, {} rutas.",
                    estacionRepository.count(), estacionRepository.contarRutas());
        } catch (RuntimeException e) {
            // Sin base la app arranca igual: los endpoints que la necesitan responden 503
            log.warn("No se pudo acceder a Neo4j al iniciar; se omite la carga inicial. "
                    + "Verificar NEO4J_URI y que la instancia de AuraDB no esté pausada. Causa: {}",
                    e.getMessage());
        }
    }
}
