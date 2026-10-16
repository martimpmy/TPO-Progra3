# Informe de complejidades asintóticas — Hito 10

Complejidad temporal y espacial de cada algoritmo **tal como está implementado** en el paquete
`algorithm`, con la recurrencia o el razonamiento que la justifica y mediciones sobre el grafo del dominio.

**Notación.** *N*: cantidad de minerales. *W*: capacidad de la bodega. *V*, *E*: estaciones y rutas del grafo
(en el dominio, V = 8 y E = 12). *k*: estaciones pedidas en el reparto. "Espacio" es el **auxiliar**, sin contar la entrada.

## Resumen

| Algoritmo | Técnica | Recurrencia / razonamiento | Tiempo (peor caso) | Espacio auxiliar | Estructura de soporte |
|---|---|---|---|---|---|
| QuickSort | Divide y vencerás | T(n) = 2T(n/2) + O(n) promedio; T(n) = T(n−1) + O(n) peor | O(n²) peor · O(n log n) promedio | O(n) copia + O(log n) pila | `ArrayList`, partición de 3 vías |
| MergeSort | Divide y vencerás | T(n) = 2T(n/2) + O(n) | O(n log n) en todos los casos | O(n) | Sublistas de mezcla |
| Greedy (carga rápida) | Voraz | Ordenar + una pasada | O(N log N) | O(N) | Lista ordenada por ratio (MergeSort) |
| Mochila 0/1 | Programación dinámica | V[i][w] = máx(V[i−1][w], vᵢ + V[i−1][w−wᵢ]) | O(N · W′) | O(N · W′) | Matriz `double[N+1][W′+1]` |
| BFS | Grafos | Cada vértice se encola una vez | O(V + E) | O(V) | Cola FIFO + `boolean[]` |
| DFS | Grafos | T(V) = Σ T(hijos) + O(grado) | O(V + E) | O(V) | Pila de recursión + `boolean[]` |
| Dijkstra | Voraz | Relajación con cola de prioridad | O((V + E) log V) | O(V + E) | `PriorityQueue` + `dist[]`, `predecesor[]` |
| Prim | Voraz | Aristas frontera en cola de prioridad | O(E log V) | O(V + E) | `PriorityQueue` + `boolean[]` |
| Kruskal | Voraz | Ordenar aristas + Union-Find | O(E log E) | O(V + E) | Lista de aristas + Union-Find propio |
| Union-Find | Estructura auxiliar | Compresión de caminos + unión por rango | O(α(V)) amortizado por operación | O(V) | `parent[]`, `rank[]` |
| Floyd-Warshall | Programación dinámica | D⁽ᵏ⁾[i][j] = mín(D⁽ᵏ⁻¹⁾[i][j], D⁽ᵏ⁻¹⁾[i][k] + D⁽ᵏ⁻¹⁾[k][j]) | Θ(V³) | O(V²) | Matriz `double[V][V]` |
| Backtracking (rutas seguras) | Búsqueda con retroceso | Árbol de caminos simples | O(V!) | O(V) | Pila de recursión + `boolean[]` |
| Branch & Bound (reparto) | Ramificación y poda | Árbol de permutaciones con cota | O(k! · k) | O(k! · k) | `PriorityQueue` por cota + máscara de bits |

W′ = W · 10ᵈ, donde *d* es la cantidad de decimales de los pesos (como máximo 6): la mochila escala los pesos a enteros
para poder usarlos como índices de la matriz.

## Divide y vencerás (Hito 2)

### QuickSort

- **Caso promedio:** cada partición recorre el rango una vez, O(n), y lo divide en dos mitades de tamaño similar:
  T(n) = 2T(n/2) + O(n). Por el **Teorema Maestro** con a = 2, b = 2, k = 1: log_b(a) = log₂(2) = 1 = k → caso 2 →
  **O(n log n)**.
- **Peor caso:** si el pivote queda siempre en un extremo, T(n) = T(n−1) + O(n) = **O(n²)**. La mediana de tres
  (bajo, medio, alto) evita que pase con listas ya ordenadas o invertidas.
- **Claves repetidas:** la partición de 3 vías agrupa los iguales al pivote y no los vuelve a procesar. Con todos los
  elementos iguales termina en una sola pasada: **O(n)**. Sin esa partición el caso sería O(n²).
- **Espacio:** la recursión siempre entra en la parte más chica y sigue con la más grande en un bucle; cada llamada
  recibe a lo sumo la mitad del rango, así que la pila es **O(log n) garantizado**, aun en el peor caso de tiempo.
  Además la implementación copia la lista para no modificar la entrada: O(n).
- **No es estable.**

### MergeSort

- T(n) = 2T(n/2) + O(n): divide a la mitad sin importar los datos y la mezcla es lineal. Teorema Maestro, caso 2 →
  **Θ(n log n)** en el mejor, peor y caso promedio.
- **Espacio:** O(n) para las sublistas de la mezcla. **Es estable** (ante claves iguales toma primero la de la izquierda);
  por eso lo reutilizan el Greedy, Kruskal y el backtracking.

## Algoritmos voraces

### Greedy de carga rápida (Hito 3)

Ordenar por ratio valor/peso con MergeSort, O(N log N), y una pasada que carga cada mineral si entra, O(N).
Total **O(N log N)**. Espacio **O(N)** por la copia ordenada y la lista de cargados.
No es óptimo para la mochila 0/1: con capacidad 10 y lotes (6 t, 66), (5 t, 50), (5 t, 50) obtiene 66 en lugar de 100.

### Dijkstra (Hito 5)

Cada relajación exitosa inserta en la cola de prioridad (montículo binario), O(log V); hay a lo sumo 2E relajaciones
(cada ruta se mira desde sus dos extremos) y V extracciones útiles. Las entradas viejas se descartan al sacarlas (`visitado[]`). Total
**O((V + E) log V)** (log E ≤ 2 log V). Con búsqueda lineal del mínimo sería O(V²).
Espacio **O(V + E)**: los arreglos `dist`, `predecesor` y `visitado` son O(V), y la cola puede llegar a E entradas.

### Prim (Hito 5)

Crece un único árbol desde una estación; las aristas que salen del árbol van a una cola de prioridad. Cada arista entra a
la cola a lo sumo dos veces (una por extremo): **O(E log E) = O(E log V)**. Espacio **O(V + E)**.

### Kruskal y Union-Find (Hito 5)

Ordenar las E aristas con MergeSort cuesta **O(E log E)** y domina. Después, por cada arista, un `find` y quizás un
`union`: con **compresión de caminos** y **unión por rango** cada operación cuesta O(α(V)) amortizado, donde α es la
inversa de Ackermann (α(V) ≤ 4 para cualquier tamaño práctico). Total **O(E log E + E · α(V)) = O(E log E)**.
Espacio **O(V + E)**: `parent[]` y `rank[]` más la lista de aristas.

## Recorridos (Hito 4)

- **BFS:** cada estación entra una vez a la cola (se marca al encolar) y cada ruta se examina dos veces, una por extremo:
  **O(V + E)**. Espacio **O(V)**: cola y arreglo de visitados.
- **DFS:** cada estación se visita una vez y se recorre su lista de adyacencia: **O(V + E)**. Espacio **O(V)** por la
  profundidad máxima de la recursión.

## Programación dinámica

### Mochila 0/1 (Hito 6)

N · W′ subproblemas, cada uno resuelto en O(1) con la recurrencia: **O(N · W′)** de tiempo y de espacio (se guarda la
matriz completa para recuperar qué minerales se eligieron, en O(N)). Es **pseudo-polinomial**: depende del *valor* de W,
no de su tamaño en bits. Por eso el algoritmo rechaza matrices de más de 5 millones de celdas.

### Floyd-Warshall (Hito 7)

Tres bucles anidados sobre los V vértices, con **k en el bucle externo**: **Θ(V³)**. Espacio **O(V²)** por la matriz.
Un ciclo negativo se detecta al final si algún D[i][i] < 0.

**Comparación medida con Dijkstra V veces** (grafo del dominio, respuesta de `/api/navegacion/todos-contra-todos`):

| | Floyd-Warshall | Dijkstra desde cada estación |
|---|---|---|
| Estados | V³ = **512** | **256** (64 nodos expandidos y 192 aristas examinadas) |
| Complejidad | Θ(V³) | O(V · (V + E) log V) |

En un grafo **disperso** como el del dominio (E = 12, lejos de V² = 64), Dijkstra V veces hace menos trabajo; Floyd-Warshall
es más simple, resuelve todos los pares de una vez y admite pesos negativos (sin ciclos negativos).

## Búsqueda exhaustiva

### Backtracking con doble poda (Hito 8)

Sin cotas, explora todos los caminos simples desde el origen: en un grafo completo hay del orden de (V−1)! →
**O(V!)**. Espacio **O(V)**: profundidad de la recursión, `visitado[]` y el camino actual.
Como los costos son positivos y cada salto suma 1, costo y saltos solo crecen a lo largo de un camino: si una rama
supera una cota, ningún descendiente puede volver a cumplirla, y se poda.

| SOL → CITADEL | Nodos explorados | Poda |
|---|---|---|
| Sin poda | 79 | — |
| maxCosto 70, maxSaltos 5 (8 rutas válidas) | 41 | 48,1 % |
| maxCosto 56, maxSaltos 5 (1 ruta válida) | 22 | 72,2 % |

### Branch & Bound del reparto (Hito 11)

El árbol de permutaciones de *k* estaciones tiene Σᵢ₌₀ᵏ k!/(k−i)! nodos, del orden de **k!**; calcular la cota de cada
hijo cuesta O(k). Peor caso **O(k! · k)** de tiempo y **O(k! · k)** de espacio (nodos pendientes en la cola, cada uno con
su camino). Antes se arma la matriz de costos mínimos entre los k + 1 puntos con Dijkstra:
O(k² · (V + E) log V).

Cota inferior: costo acumulado + Σ de la arista saliente mínima de cada estación no visitada. Es **optimista**
(nunca supera el costo real de completar el recorrido), así que podar cuando `cota ≥ mejor` no pierde el óptimo.

| Pedido desde SOL | Árbol sin poda | Generados | Expandidos | Podados | Poda | Óptimo |
|---|---|---|---|---|---|---|
| ALPHA, VEGA, KEPLER | 16 | 16 | 10 | 0 | 0 % | 75 CA |
| Las 7 estaciones | 13.700 | 3.995 | 1.895 | 1.465 | 70,8 % | 123 CA |

## Diferencias con la tabla del informe (sección 7)

| Algoritmo | Dice el informe | Corresponde | Motivo |
|---|---|---|---|
| Greedy | Espacio O(1) | **O(N)** | Se ordena una copia de la lista y se arma la lista de cargados. |
| Dijkstra | Espacio O(V) | **O(V + E)** | La cola de prioridad puede tener hasta E entradas (se descartan al sacarlas). |
| Prim | Espacio O(V) | **O(V + E)** | Misma razón: la cola guarda aristas frontera. |
| Kruskal | Espacio O(V) | **O(V + E)** | Además de Union-Find, se guarda y ordena la lista de E aristas. |
| Mochila 0/1 | O(N · W) | **O(N · W′)** | Con pesos decimales la capacidad se escala a enteros (W′ = W · 10ᵈ). |
| QuickSort | Espacio O(log N) | **O(log N) de pila**, garantizado | La recursión entra en la parte más chica; se suma O(N) por copiar la lista de entrada. |
| Branch & Bound | O(2^V · V) tiempo, O(2^V) espacio | **O(k! · k)** tiempo y espacio | 2^V · V corresponde a la programación dinámica de Held-Karp; el B&B explora un árbol de permutaciones. |
