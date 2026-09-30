## Dominio galáctico

El sistema representa la computadora de navegación y logística de una nave carguera
que opera dentro del sector estelar "Vía Láctea Austral".

La nave se desplaza entre distintas estaciones espaciales conectadas mediante rutas
hiperespaciales. Cada estación representa un punto de escala, comercio o
reabastecimiento, mientras que las rutas representan los posibles desplazamientos
entre estaciones.

Cada ruta posee un costo energético expresado en Celdas de Antimateria (CA), que
representa el consumo necesario para realizar el salto hiperespacial.

Además, las estaciones disponen de distintos cargamentos de minerales que pueden
ser transportados por la nave. Cada mineral posee:

- un nombre;
- un peso, expresado en toneladas;
- un valor comercial, expresado en Créditos Galácticos.

La nave cuenta con una capacidad máxima de carga, por lo que no siempre es posible
transportar todos los minerales disponibles.

### Aplicación del algoritmo Greedy

Para realizar una carga rápida de la bodega se utiliza una estrategia Greedy.

El criterio de selección consiste en priorizar los minerales que posean la mayor
relación entre valor comercial y peso:

ratio = valor / peso

En cada iteración se selecciona el mineral con mayor ratio que todavía pueda ser
incorporado sin superar la capacidad máxima de la bodega.

La estrategia continúa hasta evaluar todos los minerales disponibles o hasta que
ningún mineral restante pueda ser cargado.

El algoritmo Greedy busca obtener una buena solución de manera rápida, aunque no
garantiza encontrar siempre la solución óptima global para el problema de Mochila
0/1.