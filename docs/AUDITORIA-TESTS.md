# Auditoría de tests — Hito 9

Revisión de la suite de tests unitarios contra la estrategia de QA del informe (sección 8)
y del catálogo `docs/ENDPOINTS.md` contra las respuestas reales de la API.

## Cómo correrlo

```bash
./mvnw test
```

- Los tests **no necesitan Neo4j**: los algoritmos se prueban con grafos armados en memoria
  (`GrafoDominio`) y los controllers y services con el repositorio simulado (Mockito).
- El mismo comando genera el informe de cobertura en `target/site/jacoco/index.html`
  (abrirlo con el navegador).

## Resultado

**167 tests, 0 fallas.**

| Paquete | Líneas cubiertas | Ramas cubiertas |
|---|---|---|
| `algorithm` | 99 % | 95 % |
| `service` | 98 % | 85 % |
| `controller` | 97 % | 100 % |
| `config` (manejo de errores) | 100 % | 100 % |
| **Total** | **94 %** | **90 %** |

### Tests por clase

| Clase de test | Tests | Qué verifica |
|---|---|---|
| `OrdenamientoTest` | 9 | QuickSort y MergeSort; estabilidad de MergeSort; 20.000 elementos repetidos sin desbordar la pila |
| `GreedyCargaTest` | 9 | Contraejemplo (66 CG), capacidad 0, mineral que no entra, pesos decimales, empates |
| `Mochila01Test` | 11 | Óptimo 100 CG, matriz DP, recuperación de ítems, decimales, capacidad 0, mineral que no entra |
| `ContraejemploGreedyVsDPTest` | 3 | Greedy = 66 CG vs. DP = 100 CG sobre los mismos 3 minerales |
| `RecorridosTest` | 10 | Orden BFS y DFS desde SOL, un solo vértice, componente inalcanzable, 5.000 vértices |
| `CaminosMinimosTest` | 8 | Dijkstra SOL→CITADEL = 56 CA y todas las distancias del cálculo manual; relajación; inalcanzable |
| `ArbolRecubrimientoTest` | 9 | Prim y Kruskal = 72 CA con las mismas 7 aristas; grafo no conexo; grafo vacío |
| `UnionFindTest` | 8 | Unión por rango en ambos sentidos, compresión de caminos, detección de ciclos |
| `FloydWarshallTest` | 11 | Matriz vs. Dijkstra en los 64 pares, 512 estados, ciclos negativos, vértice aislado |
| `RepartoBranchAndBoundTest` | 6 | Óptimo igual a la fuerza bruta en 300 matrices al azar; cota de la raíz ≤ óptimo; 75 CA en el ejemplo; 71 % de poda con 7 estaciones |
| `RutasSegurasBacktrackingTest` | 10 | 8 rutas válidas, métricas exactas (41/79/19), orden, 200 búsquedas simultáneas |
| `MineralTest` | 4 | Validaciones del modelo (nombre, peso > 0, valor ≥ 0, NaN e infinitos) |
| `GrafoServiceTest` | 20 | Armado del grafo desde Neo4j, una sola lectura por pedido, validaciones y 404 |
| `LogisticaServiceTest` | 6 | Reparto de 75 CA, recorrido completo sobre rutas reales, repetidas, validaciones, 404 y 409 |
| `LogisticaControllerTest` | 3 | Endpoint de reparto: 200, 400 y 404 |
| `MineralControllerTest` | 12 | Alta, baja, listado y ordenamiento de minerales; errores 400/404 |
| `CargaControllerTest` | 5 | Endpoints de carga Greedy y DP |
| `GrafoControllerTest` | 7 | Resumen, recorridos y errores |
| `NavegacionControllerTest` | 9 | Dijkstra, MST, Floyd-Warshall y rutas seguras |
| `ManejadorDeErroresTest` | 7 | 400, 404, 405, 409, 415 y 500, siempre sin stack trace |

## Casos exigidos por el informe (sección 8)

| Caso | Dónde se prueba |
|---|---|
| Grafo con un solo nodo (V = 1, E = 0) | `RecorridosTest.unSoloVertice`, `ArbolRecubrimientoTest` (un solo vértice), `CaminosMinimosTest.dijkstraMismoOrigenYDestino` |
| Nodos desconectados o inalcanzables | `RecorridosTest.componenteInalcanzable`, `CaminosMinimosTest.dijkstraDestinoInalcanzable`, `FloydWarshallTest` (vértice aislado a distancia infinita), `GrafoServiceTest.dijkstraDestinoInalcanzable`, `ManejadorDeErroresTest` (responde 409) |
| Bodega con capacidad W = 0 | `GreedyCargaTest.capacidadCero`, `Mochila01Test` (capacidad cero) |
| Ítems que superan la bodega | `GreedyCargaTest.mineralQueSuperaLaBodega`, `Mochila01Test.mineralQueSuperaLaBodega` |
| Contraejemplo Greedy vs. PD (66 vs. 100 CG) | `ContraejemploGreedyVsDPTest`, `GreedyCargaTest.contraejemploDelPdf`, `Mochila01Test` |

## Hallazgos y correcciones

1. **Destino inalcanzable o grafo no conexo respondía 500.** Dijkstra, Prim y Kruskal lo
   señalan con `IllegalStateException`, que caía en el error genérico. Ahora `ManejadorDeErrores`
   responde **409 Conflict** con el mensaje del algoritmo. Tests: `ManejadorDeErroresTest`.
2. **La unión por rango del Union-Find nunca se ejecutaba en los tests.** Es la optimización que
   el informe dice implementar. Se agregó `UnionFindTest.unionPorRango`.
3. **Casos de error de los services sin probar:** estación de destino inexistente en Dijkstra y en
   rutas seguras, método de MST inválido, parámetros vacíos o negativos. Se agregaron en `GrafoServiceTest`.
4. **Manejo de errores HTTP:** 404 de ruta, 405, 415 y 500 no tenían test. Se agregó `ManejadorDeErroresTest`.
5. **Validaciones de `Mineral`** (NaN, infinitos, valor negativo, nombre vacío) cubiertas al 38 %.
   Se agregó `MineralTest`.
6. **Mochila: faltaba el caso "mineral más pesado que la bodega"**, exigido por el informe. Agregado.
7. **Código muerto de "compatibilidad con el scaffold"** (los adaptadores `dijkstraScaffold`, `primScaffold`,
   `kruskalScaffold` y los endpoints duplicados `/api/grafo/dijkstra`, `/mst`, `/prim` y `/kruskal`): eliminado.
   Nadie lo usaba y `/api/grafo/prim?origen=X` ignoraba el origen. Los endpoints oficiales son
   `/api/navegacion/dijkstra` y `/api/red/mst`.

### Catálogo `docs/ENDPOINTS.md`

- Los 13 endpoints de la API están documentados con parámetros, ejemplo, errores y complejidad.
- Se contrastó cada ejemplo con la respuesta real de la API: coinciden. Se corrigieron dos
  ejemplos desactualizados (MST y rutas seguras).
- Se agregó la **estructura usada** donde faltaba (ordenamiento, backtracking y `GET /api/grafo`).
- Dijkstra y MST documentan el nuevo `409 Conflict`; se quitó el `500` que figuraba como respuesta esperada.
- Rutas seguras: se reemplazó "más de 70 % de poda" por los valores medidos (48,1 % en el ejemplo
  y 72,2 % con `maxCosto=56`).

## Lo que queda sin cubrir, y por qué

| Código | Motivo |
|---|---|
| `GrafoRepository`, `CargaInicial` | Necesitan una base Neo4j real; quedan fuera de los tests unitarios. Se verificaron a mano contra AuraDB. |
| `Estacion.rutas` y `RutaHiperespacial` | Mapeo de Spring Data Neo4j que ningún algoritmo usa (el grafo se lee con una consulta Cypher propia). |
