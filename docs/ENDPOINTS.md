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

Ordenamiento propio de lotes de minerales y recursos comerciales aplicando la técnica de **Divide y Vencerás** (sin utilizar `Collections.sort()` ni `Arrays.sort()`).

**Parámetros de Consulta (Query Params):**
- `algoritmo` *(opcional, default: `quicksort`)*: `quicksort` o `mergesort`.
- `criterio` *(opcional, default: `ratio`)*: `ratio`, `valor` o `peso`.
- `direccion` *(opcional, default: `desc`)*: `desc` (descendente) o `asc` (ascendente).

**Cuerpo de Entrada (Request Body - Opcional):**
Si se omite (`null`), se utiliza el catálogo canónico de minerales por defecto. Si se envía un arreglo vacío (`[]`), se procesa y devuelve una lista vacía.
```json
[
  { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66 },
  { "nombre": "Núcleo de Plasma", "peso": 5, "valor": 50 },
  { "nombre": "Aleación de Titanio", "peso": 5, "valor": 50 }
]
```

**Ejemplo de Petición:**
`POST /api/minerales/ordenar?algoritmo=quicksort&criterio=ratio&direccion=desc`

**Respuesta Exitosa (200 OK):**
```json
{
  "algoritmoUtilizado": "QuickSort (Propio)",
  "criterio": "ratio",
  "resultado": [
    { "nombre": "Cristal de Taquiones", "peso": 6, "valor": 66, "ratio": 11.0 },
    { "nombre": "Núcleo de Plasma", "peso": 5, "valor": 50, "ratio": 10.0 },
    { "nombre": "Aleación de Titanio", "peso": 5, "valor": 50, "ratio": 10.0 }
  ]
}
```

**Errores Manejados (400 Bad Request):**
- Si se envía un algoritmo no soportado (ej. `algoritmo=bogo`):
```json
{
  "fecha": "2026-09-30T10:00:00",
  "codigo": 400,
  "error": "Bad Request",
  "mensaje": "Algoritmo desconocido: 'bogo'. Opciones válidas: 'quicksort', 'mergesort'."
}
```
- Si se envía un criterio inválido (ej. `criterio=color`):
```json
{
  "fecha": "2026-09-30T10:00:00",
  "codigo": 400,
  "error": "Bad Request",
  "mensaje": "Criterio inválido: 'color'. Opciones válidas: 'valor', 'peso', 'ratio'."
}
```
- Si se envía una dirección inválida (ej. `direccion=aleatoria`):
```json
{
  "fecha": "2026-09-30T10:00:00",
  "codigo": 400,
  "error": "Bad Request",
  "mensaje": "Dirección inválida: 'aleatoria'. Opciones válidas: 'desc', 'asc'."
}
```

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

**Compatibilidad con Scaffold:**
- `GET /api/seleccion/quicksort?criterio=ratio`
- `GET /api/seleccion/mergesort?criterio=peso`
