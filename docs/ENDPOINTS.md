# Catálogo de Endpoints — Odisea Galáctica

Todas las respuestas son JSON. Los errores devuelven un `ErrorResponseDTO`
(`fecha`, `codigo`, `error`, `mensaje`) sin stack trace.

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
