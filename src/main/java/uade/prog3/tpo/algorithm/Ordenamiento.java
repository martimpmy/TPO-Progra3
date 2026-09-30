package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * UNIDAD: Divide y Vencerás (Hito 2) - PUNTAJE: 1 punto
 *
 * Implementaciones algorítmicas puras de QuickSort y MergeSort.
 *
 * REGLAS DE CÁTEDRA:
 * - Prohibido el uso de Collections.sort() y Arrays.sort().
 * - No modifica la lista original de entrada (devuelve una nueva lista ordenada).
 * - Partición y mezcla implementadas completamente a mano.
 */
@Component
public class Ordenamiento {

    /**
     * QuickSort sobre una lista de elementos según un criterio de comparación.
     *
     * ESTRATEGIA DE PIVOTE:
     * Se implementa la técnica de "Mediana de Tres" (evaluando los elementos en los índices
     * bajo, medio y alto del subarreglo actual). Esta técnica evita que el algoritmo caiga
     * en el peor caso O(N^2) cuando los datos de entrada ya están ordenados o en orden inverso,
     * garantizando un balance estadísticamente óptimo de particionado.
     *
     * ANÁLISIS DE COMPLEJIDAD:
     * - Recurrencia (caso promedio): T(N) = 2T(N/2) + O(N).
     * - Por Teorema Maestro (Caso 2: a=2, b=2, k=1 -> log_2(2) = 1 = k):
     *   Complejidad temporal promedio: O(N log N).
     * - Peor caso: O(N^2) si sistemáticamente el pivote seleccionado resulta ser el mínimo
     *   o el máximo absoluto en cada paso (desbalance extremo de partición).
     * - Complejidad espacial auxiliar: O(log N) por el stack de llamadas recursivas.
     *
     * @param items    Colección de elementos a ordenar (no es modificada).
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
        if (bajo < alto) {
            int indicePivote = particionar(lista, bajo, alto, criterio);
            quickSortRecursivo(lista, bajo, indicePivote - 1, criterio);
            quickSortRecursivo(lista, indicePivote + 1, alto, criterio);
        }
    }

    private <T> int particionar(List<T> lista, int bajo, int alto, Comparator<T> criterio) {
        int medio = bajo + (alto - bajo) / 2;
        int indiceMediana = medianaDeTres(lista, bajo, medio, alto, criterio);
        intercambiar(lista, indiceMediana, alto);

        T pivote = lista.get(alto);
        int i = bajo - 1;

        for (int j = bajo; j < alto; j++) {
            if (criterio.compare(lista.get(j), pivote) <= 0) {
                i++;
                intercambiar(lista, i, j);
            }
        }
        intercambiar(lista, i + 1, alto);
        return i + 1;
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
     * MergeSort sobre una lista de elementos según un criterio de comparación.
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
