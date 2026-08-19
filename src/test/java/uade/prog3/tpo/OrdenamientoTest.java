package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.model.Item;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de QuickSort y MergeSort sobre Item.
 *
 * Criterio de orden: por valor ascendente.
 */
class OrdenamientoTest {

    private final Ordenamiento ordenamiento = new Ordenamiento();
    private final Comparator<Item> porValor = Comparator.comparingDouble(Item::getValor);

    private Item item(String id, double valor) {
        return new Item(id, "item-" + id, 1, valor, null);
    }

    @Test
    void quickSortOrdenaListaDesordenada() {
        List<Item> items = List.of(item("A", 5), item("B", 1), item("C", 4), item("D", 2), item("E", 3));

        List<Item> resultado = ordenamiento.quickSort(items, porValor);

        assertEquals(List.of("B", "D", "E", "C", "A"), idsDe(resultado));
    }

    @Test
    void quickSortOrdenaListaYaOrdenada() {
        // Peor caso para la particion de Lomuto (pivote = ultimo elemento).
        List<Item> items = List.of(item("A", 1), item("B", 2), item("C", 3), item("D", 4));

        List<Item> resultado = ordenamiento.quickSort(items, porValor);

        assertEquals(List.of("A", "B", "C", "D"), idsDe(resultado));
    }

    @Test
    void quickSortNoModificaLaListaOriginal() {
        List<Item> items = List.of(item("A", 3), item("B", 1), item("C", 2));

        ordenamiento.quickSort(items, porValor);

        assertEquals(List.of("A", "B", "C"), idsDe(items), "la lista de entrada no debe modificarse");
    }

    @Test
    void mergeSortOrdenaListaDesordenada() {
        List<Item> items = List.of(item("A", 5), item("B", 1), item("C", 4), item("D", 2), item("E", 3));

        List<Item> resultado = ordenamiento.mergeSort(items, porValor);

        assertEquals(List.of("B", "D", "E", "C", "A"), idsDe(resultado));
    }

    @Test
    void mergeSortEsEstable() {
        // Dos items con el mismo valor: el que estaba primero debe seguir primero.
        List<Item> items = List.of(item("A", 1), item("B", 1), item("C", 0));

        List<Item> resultado = ordenamiento.mergeSort(items, porValor);

        assertEquals(List.of("C", "A", "B"), idsDe(resultado));
    }

    private List<String> idsDe(List<Item> items) {
        return items.stream().map(Item::getId).toList();
    }
}
