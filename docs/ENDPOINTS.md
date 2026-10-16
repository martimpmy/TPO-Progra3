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
**Estructura usada:** lista de adyacencia `Grafo` en memoria; cada ruta se emite una sola vez (índice u < v).

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

**Estructura usada:** `ArrayList` copiada de la entrada (no se modifica la original). QuickSort particiona
in-place sobre la copia con mediana de tres y partición de 3 vías; MergeSort divide en sublistas y las mezcla en una lista nueva.

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

### POST /api/bodega/cargar-optimo  (Hito 6)

Carga **óptima** de la bodega resolviendo la Mochila 0/1 con **Programación Dinámica**.
Recibe el mismo cuerpo que `cargar-greedy`, así se pueden comparar los dos resultados.

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
  "metodo": "Programación Dinámica (Mochila 0/1)",
  "capacidadBodega": 10.0,
  "pesoOcupado": 10.0,
  "valorTotalObtenido": 100.0,
  "mineralesCargados": [
    { "nombre": "Núcleo de Plasma", "peso": 5.0, "valor": 50.0, "ratio": 10.0 },
    { "nombre": "Aleación de Titanio", "peso": 5.0, "valor": 50.0, "ratio": 10.0 }
  ],
  "matrizDP": [
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 66, 66, 66, 66, 66],
    [0, 0, 0, 0, 0, 50, 66, 66, 66, 66, 66],
    [0, 0, 0, 0, 0, 50, 66, 66, 66, 66, 100]
  ],
  "factorEscala": 1
}
```
Sobre estos mismos datos Greedy obtiene **66 CG**: el paso a paso está en
[CONTRAEJEMPLO.md](CONTRAEJEMPLO.md).

`matrizDP[i][c]` es el máximo valor usando los primeros `i` minerales con capacidad `c`.
Si hay pesos decimales, se escalan a enteros: la columna `c` equivale a `c / factorEscala` toneladas
(por ejemplo, con pesos de 0.1 t el factor es 10).

**Errores (400 Bad Request):** falta `capacidadBodega` o es negativa; `itemsDisponibles`
falta o está vacía; algún mineral inválido; pesos con más de 6 decimales; matriz de más de
5.000.000 de celdas (capacidad demasiado grande); JSON mal formado.

**Recurrencia:**
`dp[i][c] = max(dp[i-1][c], dp[i-1][c - peso_i] + valor_i)` si `peso_i ≤ c`; si no, `dp[i-1][c]`.
Caso base: `dp[0][c] = 0`.

**Complejidad:** O(N · W) — se completa una celda por cada mineral y cada capacidad de 0 a W
(W = capacidad escalada a enteros). La recuperación de ítems agrega O(N).
**Espacio:** O(N · W) — se conserva la matriz completa para poder recuperar los minerales elegidos.
**Estructura usada:** matriz `double[N+1][W+1]`.

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
- `409 Conflict`: si no existe ruta transitable entre las dos estaciones (grafo no conexo).

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
    { "origen": "Ciudadela Omega", "destino": "Puesto Nova", "costo": 7 },
    { "origen": "Alpha Centauri", "destino": "Puerto Sirio", "costo": 8 },
    { "origen": "Colonia Kepler", "destino": "Nebulosa Orion", "costo": 9 },
    { "origen": "Colonia Kepler", "destino": "Minas de Vega", "costo": 10 },
    { "origen": "Puesto Nova", "destino": "Nebulosa Orion", "costo": 11 },
    { "origen": "Alpha Centauri", "destino": "Base Solar", "costo": 12 },
    { "origen": "Colonia Kepler", "destino": "Puerto Sirio", "costo": 15 }
  ]
}
```

**Verificación:** El MST conecta los 8 vértices mediante exactamente $V - 1 = 7$ aristas al menor costo acumulado global de **72 CA** ($7 + 8 + 9 + 10 + 11 + 12 + 15$). Ambos algoritmos (Prim y Kruskal) obtienen exactamente el mismo costo óptimo.

**Errores:** `400 Bad Request` si el método especificado no es `prim` ni `kruskal`; `409 Conflict` si el grafo
no es conexo y no existe un árbol que conecte todas las estaciones.

**Justificación de Complejidad y Estructuras:**
1. **Kruskal con Union-Find propio:**
   - *Complejidad Temporal:* $O(E \log E) = O(E \log V)$, dominada por el ordenamiento de aristas (resuelto reutilizando el **MergeSort propio** del Hito 2).
   - *Union-Find Propio (DSU):* Implementado con **Compresión de Caminos (Path Compression)** en `find()` y **Unión por Rango (Union by Rank)** en `union()`, operando en tiempo casi lineal $O(E \cdot \alpha(V))$.
   - *Espacio:* $O(V)$ para los arreglos `parent[]` y `rank[]` de DSU + $O(E)$ para la lista de aristas.
2. **Prim con Cola de Prioridad:**
   - *Complejidad Temporal:* $O(E \log V)$ mediante `PriorityQueue` de aristas frontera.
   - *Espacio:* $O(V + E)$ en memoria auxiliar.

---

### GET /api/navegacion/todos-contra-todos  (Hito 7)

Calcula el **consumo mínimo entre todos los pares de estaciones** con **Floyd-Warshall**,
detecta **ciclos de costo negativo** y compara los estados expandidos contra correr Dijkstra
una vez desde cada estación.

**Parámetros de Consulta (opcionales, van los tres juntos o ninguno):**
- `simularOrigen`, `simularDestino`: ids de dos estaciones distintas.
- `simularCosto`: costo en CA de una ruta **dirigida** `simularOrigen → simularDestino`; puede ser negativo.

La ruta simulada se agrega solo a la matriz en memoria de esa consulta y **no se guarda en Neo4j**.
Existe para probar la detección: las rutas persistidas tienen costo positivo, así que el grafo
real nunca tiene ciclos negativos.

**Ejemplo:** `GET /api/navegacion/todos-contra-todos`

**Respuesta (200 OK):**
```json
{
  "algoritmo": "Floyd-Warshall (Programación Dinámica)",
  "unidad": "Celdas de Antimateria (CA)",
  "estaciones": [
    { "id": "ALPHA", "nombre": "Alpha Centauri" },
    { "id": "CITADEL", "nombre": "Ciudadela Omega" },
    { "id": "KEPLER", "nombre": "Colonia Kepler" },
    { "id": "NOVA", "nombre": "Puesto Nova" },
    { "id": "ORION", "nombre": "Nebulosa Orion" },
    { "id": "SIRIUS", "nombre": "Puerto Sirio" },
    { "id": "SOL", "nombre": "Base Solar" },
    { "id": "VEGA", "nombre": "Minas de Vega" }
  ],
  "distancias": [
    [0, 44, 23, 37, 32, 8, 12, 18],
    [44, 0, 21, 7, 18, 36, 56, 31],
    [23, 21, 0, 14, 9, 15, 35, 10],
    [37, 7, 14, 0, 11, 29, 49, 24],
    [32, 18, 9, 11, 0, 24, 44, 19],
    [8, 36, 15, 29, 24, 0, 20, 25],
    [12, 56, 35, 49, 44, 20, 0, 30],
    [18, 31, 10, 24, 19, 25, 30, 0]
  ],
  "cicloNegativo": false,
  "estacionesEnCicloNegativo": [],
  "alerta": null,
  "rutaSimulada": null,
  "comparativa": {
    "vertices": 8,
    "aristas": 12,
    "estadosFloydWarshall": 512,
    "estadosDijkstraVVeces": 256,
    "nodosExpandidosDijkstra": 64,
    "aristasExaminadasDijkstra": 192
  }
}
```
`distancias[i][j]` es el costo de ir de `estaciones[i]` a `estaciones[j]`; una celda `null` indica
que no hay camino. La fila de `SOL` coincide con el Dijkstra del Hito 5 (`SOL → CITADEL` = **56 CA**).

**Con ciclo negativo:** `GET /api/navegacion/todos-contra-todos?simularOrigen=NOVA&simularDestino=ORION&simularCosto=-30`
responde `200 OK` con `"cicloNegativo": true`, `"distancias": null`, las estaciones afectadas en
`estacionesEnCicloNegativo` y el mensaje en `alerta`. El ciclo es `ORION → NOVA` (11) + `NOVA → ORION` (−30) = −19 CA:
cada vuelta abarata el viaje, así que el mínimo no existe y no se devuelve la matriz.

**Errores:** `400` si faltan parámetros de la simulación, `simularCosto` no es un número o las dos
estaciones son la misma; `404` si alguna estación no existe; `503` si no se puede conectar a Neo4j.

**Recurrencia:** `D_k[i][j] = min(D_{k-1}[i][j], D_{k-1}[i][k] + D_{k-1}[k][j])`, con `D` inicial igual a la
matriz de adyacencia (0 en la diagonal, ∞ donde no hay ruta).

**Detección de ciclos negativos:** al terminar se revisa la diagonal; si `D[i][i] < 0`, la estación `i`
puede volver a sí misma con costo negativo.

**Comparativa de estados expandidos (V = 8, E = 12):**

| Estrategia | Qué se cuenta | Estados |
|---|---|:---:|
| Floyd-Warshall | ternas `(k, i, j)` evaluadas: V³ | **512** |
| Dijkstra × V | nodos extraídos del heap (V · V = 64) + aristas examinadas (V · 2E = 192) | **256** |

Los dos valores se **miden** con contadores en el código, no se calculan con la fórmula. En un grafo
ralo como este conviene Dijkstra × V, cuya cota es O(V · (V + E) · log V); Floyd-Warshall conviene en
grafos densos (E cercano a V²) y es el único de los dos que admite costos negativos.

**Complejidad:** O(V³) — tres bucles anidados sobre los V vértices; armar la matriz desde la lista de
adyacencia suma O(V² + E) y la revisión de la diagonal O(V).
**Espacio:** O(V²) — una única matriz, que se actualiza en el lugar.
**Estructura usada:** matriz de adyacencia `double[V][V]` con `Double.POSITIVE_INFINITY` como "sin ruta".
El grafo se lee de Neo4j en **una sola consulta** y el algoritmo trabaja únicamente en memoria.

---

### GET /api/navegacion/rutas-seguras  (Hito 8)

Exploración de **rutas simples** (sin ciclos) entre una estación de origen y una de destino mediante **Backtracking con Doble Poda** (restricción simultánea de consumo en CA y de saltos hiperespaciales).

Instrumenta contadores para comparar los **nodos explorados con poda versus sin poda** y reportar el porcentaje real de reducción del espacio de estados (`podaEfectiva`).

**Parámetros de Consulta (Query Params):**
- `origen`: id de la estación inicial (ej. `SOL`).
- `destino`: id de la estación final (ej. `CITADEL`).
- `maxCosto`: entero $\ge 0$, cota superior de consumo energético en Celdas de Antimateria (CA).
- `maxSaltos`: entero $\ge 0$, cota superior de saltos hiperespaciales (cantidad de tramos).

**Ejemplo:** `GET /api/navegacion/rutas-seguras?origen=SOL&destino=CITADEL&maxCosto=70&maxSaltos=5`

**Respuesta (200 OK):**
```json
{
  "origen": "Base Solar",
  "destino": "Ciudadela Omega",
  "maxCosto": 70,
  "maxSaltos": 5,
  "rutasValidas": [
    {
      "camino": ["Base Solar", "Alpha Centauri", "Puerto Sirio", "Colonia Kepler", "Puesto Nova", "Ciudadela Omega"],
      "caminoIds": ["SOL", "ALPHA", "SIRIUS", "KEPLER", "NOVA", "CITADEL"],
      "costoTotal": 56,
      "saltos": 5
    },
    {
      "camino": ["Base Solar", "Puerto Sirio", "Colonia Kepler", "Puesto Nova", "Ciudadela Omega"],
      "caminoIds": ["SOL", "SIRIUS", "KEPLER", "NOVA", "CITADEL"],
      "costoTotal": 61,
      "saltos": 4
    },
    {
      "camino": ["Base Solar", "Alpha Centauri", "Minas de Vega", "Colonia Kepler", "Puesto Nova", "Ciudadela Omega"],
      "caminoIds": ["SOL", "ALPHA", "VEGA", "KEPLER", "NOVA", "CITADEL"],
      "costoTotal": 61,
      "saltos": 5
    },
    {
      "camino": ["Base Solar", "Alpha Centauri", "Puerto Sirio", "Colonia Kepler", "Nebulosa Orion", "Ciudadela Omega"],
      "caminoIds": ["SOL", "ALPHA", "SIRIUS", "KEPLER", "ORION", "CITADEL"],
      "costoTotal": 63,
      "saltos": 5
    },
    {
      "camino": ["Base Solar", "Puerto Sirio", "Colonia Kepler", "Nebulosa Orion", "Puesto Nova", "Ciudadela Omega"],
      "caminoIds": ["SOL", "SIRIUS", "KEPLER", "ORION", "NOVA", "CITADEL"],
      "costoTotal": 67,
      "saltos": 5
    },
    {
      "camino": ["Base Solar", "Puerto Sirio", "Colonia Kepler", "Nebulosa Orion", "Ciudadela Omega"],
      "caminoIds": ["SOL", "SIRIUS", "KEPLER", "ORION", "CITADEL"],
      "costoTotal": 68,
      "saltos": 4
    },
    {
      "camino": ["Base Solar", "Alpha Centauri", "Minas de Vega", "Colonia Kepler", "Nebulosa Orion", "Ciudadela Omega"],
      "caminoIds": ["SOL", "ALPHA", "VEGA", "KEPLER", "ORION", "CITADEL"],
      "costoTotal": 68,
      "saltos": 5
    },
    {
      "camino": ["Base Solar", "Alpha Centauri", "Minas de Vega", "Nebulosa Orion", "Puesto Nova", "Ciudadela Omega"],
      "caminoIds": ["SOL", "ALPHA", "VEGA", "ORION", "NOVA", "CITADEL"],
      "costoTotal": 70,
      "saltos": 5
    }
  ],
  "metricasExploracion": { "nodosExploradosConPoda": 41, "nodosExploradosSinPoda": 79, "ramasPodadas": 19, "podaEfectiva": "48.1%", "porcentajePoda": 48.10126582278481 }
}
```

**Errores:**
- `400 Bad Request` si falta algún parámetro obligatorio, o si `maxCosto < 0` o `maxSaltos < 0`.
- `404 Not Found` si `origen` o `destino` no existen en la red de estaciones.

**Fundamentación de la Poda y Monotonía:**
Dado que los pesos de las rutas son estrictamente positivos ($CA > 0$) y cada salto suma $+1$, las funciones acumuladas de costo y saltos son **estrictamente monótonas crecientes**:
$$\text{costo}(P \cdot (u, v)) = \text{costo}(P) + w(u, v) > \text{costo}(P)$$
$$\text{saltos}(P \cdot (u, v)) = \text{saltos}(P) + 1 > \text{saltos}(P)$$
Si en un vértice intermedio $\text{costoActual} + w(u, v) > C_{\max}$ o $\text{saltosActuales} + 1 > K_{\max}$, es matemáticamente imposible que cualquier descendiente vuelva a satisfacer las cotas. La rama se poda inmediatamente sin evaluar ningún camino derivado.

**Complejidad:**
- *Temporal (Peor Caso):* $O(V!)$ en un grafo completo sin poda (permutaciones de caminos simples). Las cotas reducen el árbol explorado: en el ejemplo (70 CA, 5 saltos) se exploran 41 de 79 nodos (48,1 % de poda); con `maxCosto=56` sobrevive una sola ruta y se exploran 22 de 79 nodos (72,2 % de poda).
- *Espacio Auxiliar:* $O(V)$ por la pila de llamadas de recursión (profundidad máxima $V$) y el arreglo booleano de estaciones visitadas `visitado[]`.

**Estructura usada:** lista de adyacencia en memoria, arreglo `boolean[] visitado` (se marca al entrar y se desmarca al
volver: eso es el retroceso), lista con el camino actual y la pila de llamadas recursivas. Las rutas válidas se ordenan
por costo y saltos con el **MergeSort propio**.


---

### POST /api/logistica/reparto-optimo  (Hito 11)

Ruta de reparto de **menor consumo** que sale de una estación base, visita una vez cada estación pedida y
vuelve a la base, resuelta con **Ramificación y Poda (Branch & Bound)**.

**Parámetros:** `origen` *(opcional, default `SOL`)*: estación base.

**Cuerpo:** ids de las estaciones a aprovisionar. Se ignoran los repetidos y el propio origen.
```json
["ALPHA", "VEGA", "KEPLER"]
```

**Respuesta (200 OK):**
```json
{
  "algoritmo": "Branch & Bound (Ramificación y Poda)",
  "origen": "Base Solar",
  "estacionesRequeridas": ["Alpha Centauri", "Minas de Vega", "Colonia Kepler"],
  "recorridoOptimo": ["Base Solar", "Alpha Centauri", "Minas de Vega", "Colonia Kepler", "Base Solar"],
  "recorridoCompleto": ["Base Solar", "Alpha Centauri", "Minas de Vega", "Colonia Kepler", "Puerto Sirio", "Alpha Centauri", "Base Solar"],
  "tramos": [
    { "desde": "Base Solar", "hasta": "Alpha Centauri", "costo": 12, "camino": ["Base Solar", "Alpha Centauri"] },
    { "desde": "Alpha Centauri", "hasta": "Minas de Vega", "costo": 18, "camino": ["Alpha Centauri", "Minas de Vega"] },
    { "desde": "Minas de Vega", "hasta": "Colonia Kepler", "costo": 10, "camino": ["Minas de Vega", "Colonia Kepler"] },
    {
      "desde": "Colonia Kepler",
      "hasta": "Base Solar",
      "costo": 35,
      "camino": ["Colonia Kepler", "Puerto Sirio", "Alpha Centauri", "Base Solar"]
    }
  ],
  "costoMinimoTotal": 75,
  "unidad": "Celdas de Antimateria (CA)",
  "cotaUtilizada": "Costo acumulado + suma de la arista saliente mínima de cada estación no visitada (optimista/admisible)",
  "metricas": {
    "cotaInferiorRaiz": 32,
    "cotaSuperiorInicial": 75,
    "nodosGenerados": 16,
    "nodosExpandidos": 10,
    "nodosArbolPodados": 0,
    "nodosArbolCompleto": 16,
    "podaEfectiva": "0.0%"
  }
}
```

Dos estaciones pedidas pueden no estar conectadas directamente: el service calcula con **Dijkstra** el costo
mínimo entre cada par de puntos y el Branch & Bound decide el orden de visita sobre esa matriz. En el ejemplo
no existe ruta Kepler–Solar, así que la vuelta cuesta 35 CA pasando por Sirio y Alpha (`recorridoCompleto`
muestra las estaciones intermedias). El óptimo es **75 CA**.

Con 3 estaciones el árbol completo tiene solo 16 nodos y no hay nada que podar. La poda se ve con las
**7 estaciones** (`["ALPHA","SIRIUS","VEGA","KEPLER","ORION","NOVA","CITADEL"]`): recorrido óptimo de **123 CA**,
se generan **3.995 de 13.700** nodos (70,8 % de poda), 1.895 expandidos y 1.465 podados.

**Errores:** `400` si la lista falta, está vacía, tiene ids vacíos o solo contiene el origen (o si se piden
más de 12 estaciones); `404` si alguna estación no existe; `409` si alguna estación pedida es inalcanzable.

**Cota inferior (optimista):**
$$\text{cota}(nodo) = \text{costoAcumulado} + \sum_{v \in \text{noVisitados}} \min_{u \ne v} \text{costo}(v, u)$$
Es **admisible**: lo que falta del recorrido sale exactamente una vez de cada estación no visitada (hacia otra
estación o hacia la base), y cada salida cuesta al menos la mínima de esa estación. Por eso la cota nunca supera
el costo real y podar con ella no pierde el óptimo. Se poda cuando `cota >= mejor costo encontrado`.
La primera cota superior es el recorrido del **vecino más cercano**.

**Métricas:** `nodosGenerados` (hijos creados al ramificar, más la raíz), `nodosExpandidos` (sacados de la cola
y ramificados), `nodosArbolPodados` (descartados por la cota) y `nodosArbolCompleto`
($\sum_{i=0}^{k} k!/(k-i)!$, el árbol sin poda). `podaEfectiva` = 1 − generados / árbol completo.

**Complejidad:** $O(k! \cdot k)$ en el peor caso, con *k* estaciones pedidas (árbol de permutaciones; la cota de
cada hijo se calcula en $O(k)$), más $O(k^2 (V+E)\log V)$ para la matriz de Dijkstra.
**Espacio auxiliar:** $O(k! \cdot k)$ en el peor caso (nodos pendientes en la cola, cada uno con su camino).
**Estructura usada:** cola de prioridad (`PriorityQueue`) ordenada por cota inferior (exploración *best-first*),
máscara de bits con las estaciones visitadas y matriz `int[][]` de costos mínimos entre los puntos.
