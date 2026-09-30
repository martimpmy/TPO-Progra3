package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Divide y Vencerás (Hito 2) - PUNTAJE: 1 punto
 *
 * Implementaciones algorítmicas puras de QuickSort y MergeSort.
 *
 * POLÍTICAS ARQUITECTÓNICAS (Sección 3.2 del TPO):
 * - Código Java puro y agnóstico: Sin anotaciones de frameworks (@Component, etc.).
 * - Prohibido el uso de Collections.sort() y Arrays.sort().
 * - No modifica la lista original de entrada (devuelve una nueva lista ordenada).
 * - Partición y mezcla implementadas completamente a mano.
 */
public class Ordenamiento {

    /**
     * QuickSort sobre una lista de elementos según un comparador.
     *
     * ESTRATEGIA DE PARTICIONADO Y PIVOTE:
     * 1. Mediana de Tres: Se evalúan los elementos en los índices bajo, medio y alto
     *    para seleccionar el pivote, evitando degradación en arreglos previamente ordenados.
     * 2. Partición de Tres Vías (Dijkstra / Dutch National Flag):
     *    Divide el arreglo en tres segmentos contiguos:
     *      [bajo .. lt-1]  -> estrictamente MENORES al pivote
     *      [lt .. gt]      -> estrictamente IGUALES al pivote
     *      [gt+1 .. alto]  -> estrictamente MAYORES al pivote
     *    Los elementos con clave idéntica quedan en su posición definitiva en O(N)
     *    y NO se vuelven a procesar recursivamente, resolviendo el peor caso O(N^2)
     *    provocado por claves repetidas.
     * 3. Eliminación de Recursión por la Cola (Tail-Call Optimization):
     *    Se procesa recursivamente siempre la partición de menor tamaño y se itera sobre
     *    la de mayor tamaño en un bucle while. Esto garantiza matemáticamente que la
     *    profundidad máxima de la pila de llamadas (call stack) sea estrictamente O(log N),
     *    inmune a StackOverflowError incluso con cientos de miles de elementos.
     *
     * ANÁLISIS DE COMPLEJIDAD:
     * - Recurrencia promedio: T(N) = 2T(N/2) + O(N) -> O(N log N) por Teorema Maestro.
     * - Claves repetidas masivas: O(N) lineal.
     * - Complejidad espacial auxiliar: O(log N) garantizado en pila.
     *
     * @param items    Colección de elementos a ordenar (no modificada).
     * @param criterio Comparador que define la relación de orden.
     * @param <T>      Tipo del elemento.
     * @return Nueva lista ordenada según el criterio provisto.
     */
    public <T> List<T> quickSort(List<T> items, Comparator<T> criterio) {
        if (items == null) {
            return new ArrayList<>();
        }
        List<T> copia = new ArrayList<>(items);
        if (copia.size() <= 1) {
            return copia;
        }
        quickSortRecursivo(copia, 0, copia.size() - 1, criterio);
        return copia;
    }

    private <T> void quickSortRecursivo(List<T> lista, int bajo, int alto, Comparator<T> criterio) {
        while (bajo < alto) {
            // Selección del pivote por Mediana de Tres
            int medio = bajo + (alto - bajo) / 2;
            int indicePivote = medianaDeTres(lista, bajo, medio, alto, criterio);
            intercambiar(lista, bajo, indicePivote); // Ubicamos el pivote al inicio del rango

            T pivote = lista.get(bajo);
            int lt = bajo;
            int gt = alto;
            int i = bajo + 1;

            // Partición de 3 vías (Dijkstra)
            while (i <= gt) {
                int cmp = criterio.compare(lista.get(i), pivote);
                if (cmp < 0) {
                    intercambiar(lista, lt++, i++);
                } else if (cmp > 0) {
                    intercambiar(lista, i, gt--);
                } else {
                    i++;
                }
            }

            // Subarreglos resultantes: [bajo .. lt - 1] y [gt + 1 .. alto]
            // Optimización de llamada de cola: recurrir en la mitad más chica
            int tamanoIzq = lt - 1 - bajo;
            int tamanoDer = alto - (gt + 1);

            if (tamanoIzq < tamanoDer) {
                if (tamanoIzq > 0) {
                    quickSortRecursivo(lista, bajo, lt - 1, criterio);
                }
                bajo = gt + 1; // Iterar sobre la parte derecha más grande
            } else {
                if (tamanoDer > 0) {
                    quickSortRecursivo(lista, gt + 1, alto, criterio);
                }
                alto = lt - 1; // Iterar sobre la parte izquierda más grande
            }
        }
    }

    private <T> int medianaDeTres(List<T> lista, int a, int b, int c, Comparator<T> criterio) {
        T valA = lista.get(a);
        T valB = lista.get(b);
        T valC = lista.get(c);

        if (criterio.compare(valA, valB) <= 0) {
            if (criterio.compare(valB, valC) <= 0) return b;
            return criterio.compare(valA, valC) <= 0 ? c : a;
        } else {
            if (criterio.compare(valA, valC) <= 0) return a;
            return criterio.compare(valB, valC) <= 0 ? c : b;
        }
    }

    private <T> void intercambiar(List<T> lista, int i, int j) {
        if (i != j) {
            T temp = lista.get(i);
            lista.set(i, lista.get(j));
            lista.set(j, temp);
        }
    }

    /**
     * MergeSort sobre una lista de elementos según un comparador.
     *
     * ESTABILIDAD:
     * El algoritmo es ESTABLE: en el paso de mezcla (merge), ante dos elementos equivalentes
     * (comparator.compare(elemIzq, elemDer) <= 0), se prioriza siempre el elemento de la mitad
     * izquierda, garantizando que elementos con igual clave preserven su orden relativo original.
     *
     * ANÁLISIS DE COMPLEJIDAD:
     * - Recurrencia: T(N) = 2T(N/2) + O(N).
     * - Por Teorema Maestro (Caso 2: a=2, b=2, k=1 -> log_b(a) = log_2(2) = 1 = k):
     *   Complejidad temporal garantizada: O(N log N) en el mejor, peor y promedio caso.
     * - Complejidad espacial auxiliar: O(N) por las sublistas y estructuras temporales de mezcla.
     *
     * @param items    Colección de elementos a ordenar (no es modificada).
     * @param criterio Comparador que define la relación de orden.
     * @param <T>      Tipo del elemento.
     * @return Nueva lista ordenada según el criterio provisto.
     */
    public <T> List<T> mergeSort(List<T> items, Comparator<T> criterio) {
        if (items == null) {
            return new ArrayList<>();
        }
        List<T> copia = new ArrayList<>(items);
        if (copia.size() <= 1) {
            return copia;
        }
        return mergeSortRecursivo(copia, criterio);
    }

    private <T> List<T> mergeSortRecursivo(List<T> lista, Comparator<T> criterio) {
        if (lista.size() <= 1) {
            return lista;
        }
        int medio = lista.size() / 2;
        List<T> izquierda = new ArrayList<>(lista.subList(0, medio));
        List<T> derecha = new ArrayList<>(lista.subList(medio, lista.size()));

        izquierda = mergeSortRecursivo(izquierda, criterio);
        derecha = mergeSortRecursivo(derecha, criterio);

        return mezclar(izquierda, derecha, criterio);
    }

    private <T> List<T> mezclar(List<T> izquierda, List<T> derecha, Comparator<T> criterio) {
        List<T> resultado = new ArrayList<>(izquierda.size() + derecha.size());
        int i = 0;
        int j = 0;

        while (i < izquierda.size() && j < derecha.size()) {
            // El operador <= 0 asegura la preservación del orden relativo (estabilidad)
            if (criterio.compare(izquierda.get(i), derecha.get(j)) <= 0) {
                resultado.add(izquierda.get(i));
                i++;
            } else {
                resultado.add(derecha.get(j));
                j++;
            }
        }

        while (i < izquierda.size()) {
            resultado.add(izquierda.get(i));
            i++;
        }

        while (j < derecha.size()) {
            resultado.add(derecha.get(j));
            j++;
        }

        return resultado;
    }
}
