package uade.prog3.tpo.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import uade.prog3.tpo.model.Mineral;

public interface MineralRepository extends Neo4jRepository<Mineral, String> {
}
