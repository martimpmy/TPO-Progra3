# Catálogo de Endpoints — Odisea Galáctica

Todas las respuestas son JSON. Los errores devuelven un `ErrorResponseDTO`
(`fecha`, `codigo`, `error`, `mensaje`) sin stack trace.

---

### GET /api/grafo

Estaciones y rutas del grafo completo. Lo usa la consola web para dibujar el mapa estelar.
Cada ruta no dirigida aparece una sola vez.

**Respuesta (200 OK):**
```json
{
  "estaciones": [ { "id": "ALPHA", "nombre": "Alpha Centauri" }, { "id": "SOL", "nombre": "Base Solar" } ],
  "rutas": [ { "origen": "ALPHA", "destino": "SOL", "costoCA": 12 } ]
}
```

**Errores:** `503` si no se puede conectar a Neo4j.

**Complejidad:** O(V + E) — una consulta que lee todo el grafo y una pasada por la lista de adyacencia.

---

### GET /api/grafo/resumen  (Hito 1)

Cantidad de estaciones (vértices) y rutas hiperespaciales (aristas) cargadas en Neo4j AuraDB.

**Parámetros:** ninguno

**Ejemplo:** `GET /api/grafo/resumen`

**Respuesta (200 OK):**
```json
{ "vertices": 8, "aristas": 12, "estado": "Grafo cargado y conectado a AuraDB" }
```

**Errores:** `503 Service Unavailable` si no se puede conectar a Neo4j.

**Nota:** cada ruta se persiste en ambos sentidos (a→b y b→a); el conteo toma
pares distintos de estaciones, así que una ruta bidireccional cuenta como 1 arista.

**Complejidad:** O(V + E) — un `count` de nodos y un recorrido de las relaciones en Neo4j.
**Estructura usada:** ninguna en memoria; se resuelve con dos consultas Cypher de agregación.

---

### POST /api/minerales/ordenar  (Hito 2)

Ordenamiento propio de lotes de minerales aplicando **Divide y Vencerás** (sin `Collections.sort()` ni `Arrays.sort()`).

**Parámetros de Consulta (Query Params):**
- `algoritmo` *(opcional, default `quicksort`)*: `quicksort` o `mergesort`.
- `criterio` *(opcional, default `ratio`)*: `ratio`, `valor` o `peso`.
- `direccion` *(opcional, default `desc`)*: `desc` o `asc`.

**Cuerpo de Entrada (opcional):**
- **Sin body:** ordena los minerales **persistidos en Neo4j** (ver `POST /api/minerales`).
- **Con body:** ordena la lista enviada, sin guardarla. `[]` devuelve un resultado vacío.
- `peso` (> 0) y `valor` (≥ 0) admiten decimales.
```json
[
  { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66 },
  { "nombre": "Núcleo de Plasma", "peso": 5, "valor": 50 },
  { "nombre": "Aleación de Titanio", "peso": 5, "valor": 50 }
]
```

**Ejemplo:** `POST /api/minerales/ordenar?algoritmo=quicksort&criterio=ratio&direccion=desc`

**Respuesta (200 OK):**
```json
{
  "algoritmoUtilizado": "QuickSort (Propio)",
  "criterio": "ratio",
  "resultado": [
    { "nombre": "Cristal de Taquiones", "peso": 6.0, "valor": 66.0, "ratio": 11.0 },
    { "nombre": "Núcleo de Plasma", "peso": 5.0, "valor": 50.0, "ratio": 10.0 },
    { "nombre": "Aleación de Titanio", "peso": 5.0, "valor": 50.0, "ratio": 10.0 }
  ]
}
```
Los minerales persistidos incluyen además su `id`. QuickSort no es estable: dos minerales
con la misma clave (ej. ratio 10.0) pueden salir en cualquier orden; MergeSort conserva el orden original.

**Errores (400 Bad Request):** `algoritmo`, `criterio` o `direccion` inválidos; mineral sin
`nombre`, sin `valor`, con `peso` ≤ 0 o `valor` < 0; JSON mal formado.

**Justificación de Complejidad y Análisis Teórico:**
1. **QuickSort:**
   - *Estrategia del Pivote y Particionado:* Mediana de Tres (bajo, medio, alto) + **Partición de Tres Vías (Dijkstra / Dutch National Flag)**. Los elementos iguales al pivote se agrupan en su posición definitiva en una sola pasada $O(N)$, eliminando la degradación a $O(N^2)$ ante claves repetidas masivas.
   - *Optimización de Llamada de Cola (Tail-Call Optimization):* La recursión se ejecuta sobre la partición menor y se itera sobre la mayor, acotando la profundidad de pila a **$O(\log N)$ garantizado** e impidiendo cualquier `StackOverflowError`.
   - *Recurrencia promedio:* $T(N) = 2T(N/2) + O(N)$.
   - *Teorema Maestro:* $a=2, b=2, k=1 \implies \log_2(2) = 1 = k \implies$ Caso 2: $O(N \log N)$ promedio.
   - *Peor caso teórico:* $O(N^2)$ si todos los elementos son distintos y el pivote resulta ser sistemáticamente el extremo.
2. **MergeSort:**
   - *Estabilidad:* Garantizada mediante comparación inclusiva (`<=`) al fusionar las mitades ordenadas.
   - *Recurrencia:* $T(N) = 2T(N/2) + O(N)$.
   - *Complejidad temporal:* $O(N \log N)$ garantizada en el mejor, peor y promedio caso.
   - *Espacio auxiliar:* $O(N)$ para las sublistas de mezcla en memoria.

---

### GET /api/minerales

Lista los minerales persistidos en Neo4j (`(:Mineral {id, nombre, peso, valor})`).

**Respuesta (200 OK):**
```json
[ { "id": "3f2a…", "nombre": "Cristal de Taquiones", "peso": 6.0, "valor": 66.0, "ratio": 11.0 } ]
```

**Complejidad:** O(N) — lectura de los N nodos `:Mineral`.

---

### POST /api/minerales

Persiste uno o más minerales en Neo4j. Si alguno es inválido no se guarda ninguno.

**Cuerpo:** `[ { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66 } ]`

**Respuesta (201 Created):** los minerales guardados, con su `id` generado.

**Errores:** `400` si la lista está vacía o algún mineral es inválido.

**Complejidad:** O(N) — una escritura por mineral en una sola transacción.

---

### DELETE /api/minerales/{id}

Elimina un mineral persistido.

**Respuesta:** `204 No Content`. **Errores:** `404` si no existe el `id`.

---

### POST /api/bodega/cargar-greedy  (Hito 3)

Carga rápida de la bodega con criterio **voraz**: toma primero los minerales de mayor
ratio valor/peso mientras entren en la capacidad. **No garantiza el óptimo** de Mochila 0/1.

**Cuerpo de Entrada:**
```json
{
  "capacidadBodega": 10,
  "itemsDisponibles": [
    { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66 },
    { "nombre": "Núcleo de Plasma", "peso": 5, "valor": 50 },
    { "nombre": "Aleación de Titanio", "peso": 5, "valor": 50 }
  ]
}
```

**Respuesta (200 OK):**
```json
{
  "metodo": "Greedy (Selección por Ratio Valor/Peso)",
  "capacidadBodega": 10.0,
  "pesoOcupado": 6.0,
  "valorTotalObtenido": 66.0,
  "mineralesCargados": [
    { "nombre": "Cristal de Taquiones", "peso": 6.0, "valor": 66.0, "ratio": 11.0 }
  ]
}
```
Este es el contraejemplo del informe (sección 5.6): Greedy obtiene **66 CG**, mientras que
el óptimo (Núcleo + Titanio) es **100 CG**. Si un mineral no entra, se saltea y se prueba el siguiente.

**Errores (400 Bad Request):** falta `capacidadBodega` o es negativa; `itemsDisponibles`
falta o está vacía; algún mineral inválido (sin nombre, sin valor, peso ≤ 0); JSON mal formado.

**Componentes de la técnica voraz:**
- *Candidatos:* los N minerales recibidos.
- *Selección:* mayor ratio `valor / peso`.
- *Factibilidad:* `pesoActual + peso ≤ capacidad` (con tolerancia 1e-9 para pesos decimales).
- *Objetivo:* maximizar el valor en Créditos Galácticos.
- *Solución:* termina al evaluar todos los minerales.

**Complejidad:** O(N log N) — dominada por el ordenamiento por ratio; el recorrido posterior es O(N).
**Espacio auxiliar:** O(N) — copia ordenada y lista de seleccionados.
**Estructura usada:** lista ordenada con el **MergeSort propio** (estable: ante ratios iguales
respeta el orden de entrada, así el resultado es determinístico).

---

### GET /api/grafo/recorrer  (Hito 4)

Recorre la red de estaciones desde un origen con **BFS** (por niveles) o **DFS** (en profundidad)
y devuelve el orden en que se visitan las estaciones alcanzables.

**Parámetros:**
- `origen` *(obligatorio)*: id de la estación de partida (`SOL`, `ALPHA`, `SIRIUS`, `VEGA`,
  `KEPLER`, `ORION`, `NOVA`, `CITADEL`).
- `tipo` *(opcional, default `BFS`)*: `BFS` o `DFS`.

**Ejemplo:** `GET /api/grafo/recorrer?origen=SOL&tipo=BFS`

**Respuesta (200 OK):**
```json
{
  "nodoInicial": "Base Solar (SOL)",
  "recorrido": "BFS",
  "ordenExploracion": [
    "Base Solar", "Alpha Centauri", "Puerto Sirio", "Minas de Vega",
    "Colonia Kepler", "Nebulosa Orion", "Puesto Nova", "Ciudadela Omega"
  ]
}
```
Con `tipo=DFS` desde `SOL` el orden es: Base Solar, Alpha Centauri, Puerto Sirio, Colonia Kepler,
Puesto Nova, Ciudadela Omega, Nebulosa Orion, Minas de Vega.

Los vecinos de cada estación se exploran en **orden alfabético de id**, por eso el resultado
es siempre el mismo. BFS visita las estaciones por cantidad de saltos desde el origen
(nivel 1: Alpha y Sirio; nivel 2: Vega y Kepler; nivel 3: Orion y Nova; nivel 4: Ciudadela).

**Errores:** `400` si falta `origen` o `tipo` no es `BFS`/`DFS`; `404` si la estación no existe.

**Complejidad:** O(V + E) — cada estación se visita una vez y cada ruta se examina dos veces
(una por extremo). **Espacio auxiliar:** O(V).
**Estructura usada:** lista de adyacencia en memoria + arreglo `boolean[] visitado`;
BFS usa una **cola FIFO** (se marca visitado al encolar) y DFS la **pila de llamadas recursivas**.
El grafo se lee de Neo4j en **una sola consulta** y el algoritmo trabaja únicamente en memoria.

---

### GET /api/navegacion/dijkstra  (Hito 5)

Calcula la **trayectoria óptima de consumo energético** (Celdas de Antimateria - CA) desde una estación de origen a una de destino mediante el algoritmo de **Dijkstra con Min-Heap**, reconstruyendo el camino completo paso a paso.

**Parámetros de Consulta:**
- `origen` *(obligatorio)*: identificador de la estación de partida (ej. `SOL`).
- `destino` *(obligatorio)*: identificador de la estación de destino (ej. `CITADEL`).

**Ejemplo:** `GET /api/navegacion/dijkstra?origen=SOL&destino=CITADEL`

**Respuesta Exitosa (200 OK):**
```json
{
  "origen": "Base Solar",
  "destino": "Ciudadela Omega",
  "consumoTotal": 56,
  "unidad": "Celdas de Antimateria (CA)",
  "caminoReconstruido": [
    "Base Solar",
    "Alpha Centauri",
    "Puerto Sirio",
    "Colonia Kepler",
    "Puesto Nova",
    "Ciudadela Omega"
  ],
  "caminoIds": [
    "SOL",
    "ALPHA",
    "SIRIUS",
    "KEPLER",
    "NOVA",
    "CITADEL"
  ]
}
```

**Verificación contra Cálculo Manual del Informe (Sección 5.5):**
Trayectoria: `SOL` (0) → `ALPHA` (+12=12) → `SIRIUS` (+8=20) → `KEPLER` (+15=35) → `NOVA` (+14=49) → `CITADEL` (+7=56 CA).

**Errores Manejados:**
- `400 Bad Request`: Si falta el parámetro `origen` o `destino`.
- `404 Not Found`: Si alguna de las estaciones especificadas no existe en el grafo.
- `500 Internal Server Error`: Si el grafo fuese disconexo y no existiera ruta transitable.

**Justificación de Complejidad y Análisis Teórico:**
- **Complejidad Temporal:** $O((V + E) \log V)$ sostenida mediante `PriorityQueue` (Min-Heap binario). Si se empleara búsqueda lineal en arreglo caería a $O(V^2)$, degradando la calificación según la rúbrica.
- **Complejidad Espacial:** $O(V)$ para los vectores auxiliares `dist[]`, `predecesor[]` y `visitado[]`.
- **Estructuras Usadas:** Min-Heap binario + arreglo de predecesores para reconstrucción inversa en $O(L)$ donde $L \le V$.
- **Lectura Anti-Penalización:** El grafo se carga una sola vez a memoria RAM; el algoritmo opera en memoria pura sin consultas Cypher en sus bucles.

---

### GET /api/red/mst  (Hito 5)

Diseña la **Red Troncal de Balizas Subespaciales** interconectando todas las estaciones al menor costo global posible mediante un Árbol Generador Mínimo (MST), utilizando **Kruskal** (con estructura propia Union-Find DSU) o **Prim** (árbol continuo con PriorityQueue).

**Parámetros de Consulta:**
- `metodo` *(opcional, default: `kruskal`)*: `kruskal` o `prim`.

**Ejemplo:** `GET /api/red/mst?metodo=kruskal`

**Respuesta Exitosa (200 OK):**
```json
{
  "algoritmo": "Kruskal (con Union-Find propio)",
  "costoTotalMST": 72,
  "unidad": "Celdas de Antimateria (CA)",
  "aristasSeleccionadas": [
    { "origen": "Puesto Nova", "destino": "Ciudadela Omega", "costo": 7 },
    { "origen": "Alpha Centauri", "destino": "Puerto Sirio", "costo": 8 },
    { "origen": "Colonia Kepler", "destino": "Nebulosa Orion", "costo": 9 },
    { "origen": "Minas de Vega", "destino": "Colonia Kepler", "costo": 10 },
    { "origen": "Puesto Nova", "destino": "Nebulosa Orion", "costo": 11 },
    { "origen": "Base Solar", "destino": "Alpha Centauri", "costo": 12 },
    { "origen": "Puerto Sirio", "destino": "Colonia Kepler", "costo": 15 }
  ]
}
```

**Verificación:** El MST conecta los 8 vértices mediante exactamente $V - 1 = 7$ aristas al menor costo acumulado global de **72 CA** ($7 + 8 + 9 + 10 + 11 + 12 + 15$). Ambos algoritmos (Prim y Kruskal) obtienen exactamente el mismo costo óptimo.

**Errores:** `400 Bad Request` si el método especificado no es `prim` ni `kruskal`.

**Justificación de Complejidad y Estructuras:**
1. **Kruskal con Union-Find propio:**
   - *Complejidad Temporal:* $O(E \log E) = O(E \log V)$, dominada por el ordenamiento de aristas (resuelto reutilizando el **MergeSort propio** del Hito 2).
   - *Union-Find Propio (DSU):* Implementado con **Compresión de Caminos (Path Compression)** en `find()` y **Unión por Rango (Union by Rank)** en `union()`, operando en tiempo casi lineal $O(E \cdot \alpha(V))$.
   - *Espacio:* $O(V)$ para los arreglos `parent[]` y `rank[]` de DSU + $O(E)$ para la lista de aristas.
2. **Prim con Cola de Prioridad:**
   - *Complejidad Temporal:* $O(E \log V)$ mediante `PriorityQueue` de aristas frontera.
   - *Espacio:* $O(V + E)$ en memoria auxiliar.

**Compatibilidad con Scaffold:**
- `GET /api/grafo/dijkstra?origen=SOL&destino=CITADEL`
- `GET /api/grafo/mst?metodo=kruskal`
- `GET /api/grafo/prim?origen=SOL`
- `GET /api/grafo/kruskal`

