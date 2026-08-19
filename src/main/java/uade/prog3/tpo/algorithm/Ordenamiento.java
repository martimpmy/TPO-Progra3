package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Divide y Conquista (clase 2)     PUNTAJE: 1 punto
 *
 * IMPORTANTE: no vale llamar a Collections.sort ni a Arrays.sort.
 * El punto se asigna por implementar el algoritmo, no por usarlo.
 */
@Component
public class Ordenamiento {

    /**
     * QuickSort sobre una lista de items.
     *
     * Requisitos:
     *   - implementar la particion (partition) a mano
     *   - documentar que estrategia de pivote usaron y por que
     *   - NO modificar la lista de entrada: devolver una lista nueva
     *
     * Complejidad esperada: O(n log n) promedio, O(n^2) peor caso.
     * Hay que poder explicar cuando ocurre el peor caso.
     */
    public List<Item> quickSort(List<Item> items, Comparator<Item> criterio) {
        List<Item> copia = new ArrayList<>(items);
        quickSort(copia, 0, copia.size() - 1, criterio);
        return copia;
    }

    /**
     * Estrategia de pivote: se usa siempre el ultimo elemento del rango
     * (particion de Lomuto). Es la mas simple de implementar y de explicar,
     * pero es tambien la que peor se comporta si la lista ya viene ordenada
     * (o casi ordenada): en ese caso cada particion deja un lado vacio y la
     * recursion degenera en O(n^2) (peor caso). En promedio, con datos en
     * orden aleatorio, las particiones quedan balanceadas y se logra
     * O(n log n).
     */
    private void quickSort(List<Item> items, int desde, int hasta, Comparator<Item> criterio) {
        if (desde >= hasta) {
            return;
        }
        int posPivote = partition(items, desde, hasta, criterio);
        quickSort(items, desde, posPivote - 1, criterio);
        quickSort(items, posPivote + 1, hasta, criterio);
    }

    /**
     * Particion de Lomuto: usa el ultimo elemento del rango [desde, hasta]
     * como pivote y reubica los elementos menores a su izquierda.
     * Devuelve la posicion final del pivote.
     */
    private int partition(List<Item> items, int desde, int hasta, Comparator<Item> criterio) {
        Item pivote = items.get(hasta);
        int i = desde - 1;
        for (int j = desde; j < hasta; j++) {
            if (criterio.compare(items.get(j), pivote) <= 0) {
                i++;
                swap(items, i, j);
            }
        }
        swap(items, i + 1, hasta);
        return i + 1;
    }

    private void swap(List<Item> items, int i, int j) {
        Item aux = items.get(i);
        items.set(i, items.get(j));
        items.set(j, aux);
    }

    /**
     * MergeSort sobre una lista de items.
     *
     * Requisitos:
     *   - implementar la mezcla (merge) a mano
     *   - respetar la estabilidad del algoritmo
     *
     * Complejidad esperada: O(n log n) siempre.
     *
     * Para el coloquio: escribir la recurrencia T(n) = 2T(n/2) + O(n)
     * y resolverla con la regla practica (a, b, k).
     */
    public List<Item> mergeSort(List<Item> items, Comparator<Item> criterio) {
        List<Item> copia = new ArrayList<>(items);
        if (copia.size() <= 1) {
            return copia;
        }
        return mergeSortRec(copia, criterio);
    }

    private List<Item> mergeSortRec(List<Item> items, Comparator<Item> criterio) {
        if (items.size() <= 1) {
            return items;
        }
        int medio = items.size() / 2;
        List<Item> izquierda = mergeSortRec(new ArrayList<>(items.subList(0, medio)), criterio);
        List<Item> derecha = mergeSortRec(new ArrayList<>(items.subList(medio, items.size())), criterio);
        return merge(izquierda, derecha, criterio);
    }

    /**
     * Mezcla dos sublistas ya ordenadas en una sola ordenada.
     * Para mantener la estabilidad, ante un empate (compare == 0) se toma
     * primero el elemento de la lista izquierda (<=), preservando el orden
     * relativo original entre elementos equivalentes.
     */
    private List<Item> merge(List<Item> izquierda, List<Item> derecha, Comparator<Item> criterio) {
        List<Item> resultado = new ArrayList<>(izquierda.size() + derecha.size());
        int i = 0;
        int j = 0;
        while (i < izquierda.size() && j < derecha.size()) {
            if (criterio.compare(izquierda.get(i), derecha.get(j)) <= 0) {
                resultado.add(izquierda.get(i++));
            } else {
                resultado.add(derecha.get(j++));
            }
        }
        while (i < izquierda.size()) {
            resultado.add(izquierda.get(i++));
        }
        while (j < derecha.size()) {
            resultado.add(derecha.get(j++));
        }
        return resultado;
    }
}
