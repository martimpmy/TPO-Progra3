// Grafo semilla del dominio Odisea Galactica: 8 estaciones y 12 rutas.
// Es UNA sola sentencia (sin ';' intermedios) para que las variables de las
// estaciones sigan vigentes al crear las rutas. MERGE la hace idempotente.
MERGE (sol:Estacion {id: "SOL"})         SET sol.nombre = "Base Solar",          sol.sector = "Origen Central"
MERGE (alpha:Estacion {id: "ALPHA"})     SET alpha.nombre = "Alpha Centauri",    alpha.sector = "Brazo Local"
MERGE (sirius:Estacion {id: "SIRIUS"})   SET sirius.nombre = "Puerto Sirio",     sirius.sector = "Sector Canis"
MERGE (vega:Estacion {id: "VEGA"})       SET vega.nombre = "Minas de Vega",      vega.sector = "Constelacion Lyra"
MERGE (kepler:Estacion {id: "KEPLER"})   SET kepler.nombre = "Colonia Kepler",   kepler.sector = "Frontera Externa"
MERGE (orion:Estacion {id: "ORION"})     SET orion.nombre = "Nebulosa Orion",    orion.sector = "Cinturon Profundo"
MERGE (nova:Estacion {id: "NOVA"})       SET nova.nombre = "Puesto Nova",        nova.sector = "Vacio Exterior"
MERGE (citadel:Estacion {id: "CITADEL"}) SET citadel.nombre = "Ciudadela Omega", citadel.sector = "Nucleo Galactico"

// Rutas bidireccionales: se persisten en ambos sentidos con el mismo costo.
MERGE (sol)-[:RUTA_HIPERESPACIAL {costoCA: 12}]->(alpha)
MERGE (alpha)-[:RUTA_HIPERESPACIAL {costoCA: 12}]->(sol)
MERGE (sol)-[:RUTA_HIPERESPACIAL {costoCA: 25}]->(sirius)
MERGE (sirius)-[:RUTA_HIPERESPACIAL {costoCA: 25}]->(sol)
MERGE (alpha)-[:RUTA_HIPERESPACIAL {costoCA: 8}]->(sirius)
MERGE (sirius)-[:RUTA_HIPERESPACIAL {costoCA: 8}]->(alpha)
MERGE (alpha)-[:RUTA_HIPERESPACIAL {costoCA: 18}]->(vega)
MERGE (vega)-[:RUTA_HIPERESPACIAL {costoCA: 18}]->(alpha)
MERGE (sirius)-[:RUTA_HIPERESPACIAL {costoCA: 15}]->(kepler)
MERGE (kepler)-[:RUTA_HIPERESPACIAL {costoCA: 15}]->(sirius)
MERGE (vega)-[:RUTA_HIPERESPACIAL {costoCA: 10}]->(kepler)
MERGE (kepler)-[:RUTA_HIPERESPACIAL {costoCA: 10}]->(vega)
MERGE (vega)-[:RUTA_HIPERESPACIAL {costoCA: 22}]->(orion)
MERGE (orion)-[:RUTA_HIPERESPACIAL {costoCA: 22}]->(vega)
MERGE (kepler)-[:RUTA_HIPERESPACIAL {costoCA: 9}]->(orion)
MERGE (orion)-[:RUTA_HIPERESPACIAL {costoCA: 9}]->(kepler)
MERGE (kepler)-[:RUTA_HIPERESPACIAL {costoCA: 14}]->(nova)
MERGE (nova)-[:RUTA_HIPERESPACIAL {costoCA: 14}]->(kepler)
MERGE (orion)-[:RUTA_HIPERESPACIAL {costoCA: 11}]->(nova)
MERGE (nova)-[:RUTA_HIPERESPACIAL {costoCA: 11}]->(orion)
MERGE (orion)-[:RUTA_HIPERESPACIAL {costoCA: 19}]->(citadel)
MERGE (citadel)-[:RUTA_HIPERESPACIAL {costoCA: 19}]->(orion)
MERGE (nova)-[:RUTA_HIPERESPACIAL {costoCA: 7}]->(citadel)
MERGE (citadel)-[:RUTA_HIPERESPACIAL {costoCA: 7}]->(nova)
