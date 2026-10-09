# Contraejemplo: Greedy vs. Mochila 0/1 con Programación Dinámica (Hito 6)

Juego de datos donde el algoritmo Greedy del Hito 3 **no** obtiene la carga óptima y la
Programación Dinámica sí.

## Juego de datos

Capacidad de la bodega: **W = 10 t**.

| i | Mineral | Peso (t) | Valor (CG) | Ratio (CG/t) |
|:---:|---|:---:|:---:|:---:|
| 1 | Cristal de Taquiones | 6 | 66 | 11 |
| 2 | Núcleo de Plasma | 5 | 50 | 10 |
| 3 | Aleación de Titanio | 5 | 50 | 10 |

## Qué hace Greedy

Greedy ordena por ratio valor/peso de mayor a menor y carga cada mineral si todavía entra.

| Paso | Mineral | Peso acumulado | ¿Entra? | Valor acumulado |
|:---:|---|:---:|:---:|:---:|
| 1 | Cristal de Taquiones (ratio 11) | 0 + 6 = 6 | Sí | 66 |
| 2 | Núcleo de Plasma (ratio 10) | 6 + 5 = 11 > 10 | No | 66 |
| 3 | Aleación de Titanio (ratio 10) | 6 + 5 = 11 > 10 | No | 66 |

**Resultado Greedy: 66 CG**, con 6 t ocupadas y 4 t libres que ningún mineral puede aprovechar.

## Qué hace la Programación Dinámica

`dp[i][c]` es el máximo valor que se puede obtener usando los primeros `i` minerales con una
capacidad disponible de `c` toneladas.

```
dp[0][c] = 0
dp[i][c] = dp[i-1][c]                                        si peso_i > c
dp[i][c] = max(dp[i-1][c], dp[i-1][c - peso_i] + valor_i)    si peso_i ≤ c
```

### Matriz completa

| i \ c | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 0 · sin minerales | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1 · Cristal (6 t, 66) | 0 | 0 | 0 | 0 | 0 | 0 | 66 | 66 | 66 | 66 | 66 |
| 2 · Plasma (5 t, 50) | 0 | 0 | 0 | 0 | 0 | 50 | 66 | 66 | 66 | 66 | 66 |
| 3 · Titanio (5 t, 50) | 0 | 0 | 0 | 0 | 0 | 50 | 66 | 66 | 66 | 66 | **100** |

### Paso a paso

**Fila 1 — Cristal de Taquiones (6 t, 66 CG).**
Para `c` de 0 a 5 no entra: se copia la fila anterior (0).
Para `c` de 6 a 10 entra: `max(0, dp[0][c-6] + 66) = 66`.

**Fila 2 — Núcleo de Plasma (5 t, 50 CG).**
Para `c` de 0 a 4 no entra: 0.
Para `c = 5`: `max(dp[1][5], dp[1][0] + 50) = max(0, 50) = 50`.
Para `c` de 6 a 10: `max(dp[1][c], dp[1][c-5] + 50) = max(66, 0 + 50) = 66`. Conviene seguir con el
Cristal: los dos juntos pesan 11 t y no entran, por eso `dp[1][c-5]` vale 0 (`c - 5 ≤ 5`).

**Fila 3 — Aleación de Titanio (5 t, 50 CG).**
Para `c` de 0 a 4 no entra: 0.
Para `c = 5`: `max(dp[2][5], dp[2][0] + 50) = max(50, 50) = 50`.
Para `c` de 6 a 9: `max(dp[2][c], dp[2][c-5] + 50) = max(66, 0 + 50) = 66`.
Para `c = 10`: `max(dp[2][10], dp[2][5] + 50) = max(66, 50 + 50) = 100`.

**Resultado DP: `dp[3][10]` = 100 CG.**

### Recuperación de los minerales elegidos

Se recorre la matriz desde la última fila con `c = 10`. Si `dp[i][c]` es distinto de
`dp[i-1][c]`, el mineral `i` fue cargado y se descuenta su peso.

| i | c | `dp[i][c]` | `dp[i-1][c]` | ¿Cargado? | c siguiente |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 3 · Titanio | 10 | 100 | 66 | Sí | 10 − 5 = 5 |
| 2 · Plasma | 5 | 50 | 0 | Sí | 5 − 5 = 0 |
| 1 · Cristal | 0 | 0 | 0 | No | 0 |

**Carga óptima: Núcleo de Plasma + Aleación de Titanio**, 10 t y 100 CG.

## Conclusión

| Algoritmo | Minerales cargados | Peso | Valor | Complejidad |
|---|---|:---:|:---:|:---:|
| Greedy (Hito 3) | Cristal de Taquiones | 6 t | 66 CG | O(N log N) |
| DP (Hito 6) | Núcleo de Plasma + Aleación de Titanio | 10 t | **100 CG** | O(N · W) |

Greedy falla porque decide mirando solo el ratio de cada mineral: el Cristal tiene el mejor
ratio, pero deja 4 t vacías. En la Mochila 0/1 los minerales no se pueden fraccionar, así que
la mejor elección local no garantiza el óptimo global. La DP evalúa todas las combinaciones de
"cargar / no cargar" reutilizando subproblemas, y por eso encuentra que llenar la bodega con
dos minerales de ratio 10 vale más.

## Cómo verificarlo

- Test: [`ContraejemploGreedyVsDPTest`](../src/test/java/uade/prog3/tpo/algorithm/ContraejemploGreedyVsDPTest.java)
  comprueba los 66 CG de Greedy, los 100 CG de DP y las cuatro filas de la matriz.
- API: enviar el mismo cuerpo a `POST /api/bodega/cargar-greedy` y a
  `POST /api/bodega/cargar-optimo` (ver [ENDPOINTS.md](ENDPOINTS.md)).
- Consola web: pestaña **Carga óptima**, botón "Probar el contraejemplo del informe".
