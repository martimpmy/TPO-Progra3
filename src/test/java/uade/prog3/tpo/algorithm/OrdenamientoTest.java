package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Mineral;

import static org.assertj.core.api.Assertions.assertThat;

class OrdenamientoTest {

    private Ordenamiento ordenamiento;
    private List<Mineral> mineralesEjemplo;

    @BeforeEach
    void setUp() {
        ordenamiento = new Ordenamiento();
        mineralesEjemplo = List.of(
                new Mineral("Cristal de Taquiones", 6.0, 66.0),    // ratio = 11.0
                new Mineral("Núcleo de Plasma", 5.0, 50.0),       // ratio = 10.0
                new Mineral("Aleación de Titanio", 5.0, 50.0),     // ratio = 10.0
                new Mineral("Fragmento de Antimateria", 2.0, 30.0) // ratio = 15.0
        );
    }

    @Test
    @DisplayName("QuickSort ordena correctamente por ratio de forma descendente")
    void quickSortOrdenaPorRatioDescendente() {
        Comparator<Mineral> comp = Comparator.comparingDouble(Mineral::getRatio).reversed();

        List<Mineral> ordenados = ordenamiento.quickSort(mineralesEjemplo, comp);

        assertThat(ordenados).hasSize(4);
        assertThat(ordenados.get(0).getNombre()).isEqualTo("Fragmento de Antimateria"); // 15.0
        assertThat(ordenados.get(1).getNombre()).isEqualTo("Cristal de Taquiones");     // 11.0
        assertThat(ordenados.get(2).getRatio()).isEqualTo(10.0);
        assertThat(ordenados.get(3).getRatio()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("QuickSort no modifica la lista original de entrada")
    void quickSortNoModificaListaOriginal() {
        List<Mineral> original = new ArrayList<>(mineralesEjemplo);
        Comparator<Mineral> comp = Comparator.comparingDouble(Mineral::getPeso);

        ordenamiento.quickSort(original, comp);

        assertThat(original).containsExactlyElementsOf(mineralesEjemplo);
    }

    @Test
    @DisplayName("QuickSort maneja lista vacía y lista de un único elemento")
    void quickSortCasosBorde() {
        Comparator<Mineral> comp = Comparator.comparingDouble(Mineral::getPeso);

        assertThat(ordenamiento.quickSort(List.of(), comp)).isEmpty();

        Mineral unico = new Mineral("Oro Estelar", 1.0, 100.0);
        List<Mineral> resUnico = ordenamiento.quickSort(List.of(unico), comp);
        assertThat(resUnico).containsExactly(unico);
    }

    @Test
    @DisplayName("MergeSort ordena correctamente por peso de forma ascendente")
    void mergeSortOrdenaPorPesoAscendente() {
        Comparator<Mineral> comp = Comparator.comparingDouble(Mineral::getPeso);

        List<Mineral> ordenados = ordenamiento.mergeSort(mineralesEjemplo, comp);

        assertThat(ordenados).hasSize(4);
        assertThat(ordenados.get(0).getPeso()).isEqualTo(2.0); // Antimateria
        assertThat(ordenados.get(1).getPeso()).isEqualTo(5.0);
        assertThat(ordenados.get(2).getPeso()).isEqualTo(5.0);
        assertThat(ordenados.get(3).getPeso()).isEqualTo(6.0); // Taquiones
    }

    @Test
    @DisplayName("MergeSort es ESTABLE: mantiene el orden relativo original para claves iguales")
    void mergeSortPreservaEstabilidad() {
        // Objeto simple con clave y orden original para probar estabilidad
        record Elemento(String nombre, int clave) {}

        List<Elemento> lista = List.of(
                new Elemento("A1", 2),
                new Elemento("B1", 1),
                new Elemento("A2", 2),
                new Elemento("C1", 3),
                new Elemento("A3", 2)
        );

        Comparator<Elemento> comp = Comparator.comparingInt(Elemento::clave);
        List<Elemento> resultado = ordenamiento.mergeSort(lista, comp);

        List<String> nombresConClave2 = resultado.stream()
                .filter(e -> e.clave() == 2)
                .map(Elemento::nombre)
                .toList();

        // Debe conservar A1, luego A2, luego A3
        assertThat(nombresConClave2).containsExactly("A1", "A2", "A3");
    }

    @Test
    @DisplayName("MergeSort no modifica la lista original de entrada")
    void mergeSortNoModificaListaOriginal() {
        List<Mineral> original = new ArrayList<>(mineralesEjemplo);
        Comparator<Mineral> comp = Comparator.comparingDouble(Mineral::getValor);

        ordenamiento.mergeSort(original, comp);

        assertThat(original).containsExactlyElementsOf(mineralesEjemplo);
    }

    @Test
    @DisplayName("MergeSort maneja lista vacía y lista de un único elemento")
    void mergeSortCasosBorde() {
        Comparator<Mineral> comp = Comparator.comparingDouble(Mineral::getValor);

        assertThat(ordenamiento.mergeSort(List.of(), comp)).isEmpty();

        Mineral unico = new Mineral("Platino", 3.0, 50.0);
        assertThat(ordenamiento.mergeSort(List.of(unico), comp)).containsExactly(unico);
    }
}
