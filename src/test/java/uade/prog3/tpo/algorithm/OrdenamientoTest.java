package uade.prog3.tpo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
                new Mineral("Cristal de Taquiones", 6, 66),    // ratio = 11.0
                new Mineral("Núcleo de Plasma", 5, 50),       // ratio = 10.0
                new Mineral("Aleación de Titanio", 5, 50),     // ratio = 10.0
                new Mineral("Fragmento de Antimateria", 2, 30) // ratio = 15.0
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
    @DisplayName("QuickSort con 20.000 elementos iguales corre en tiempo lineal O(N) sin StackOverflowError")
    void quickSortCon20000ElementosRepetidos() {
        Mineral repetido = new Mineral("Titanio", 5, 50); // ratio 10.0
        List<Mineral> masiva = new ArrayList<>(20_000);
        for (int i = 0; i < 20_000; i++) {
            masiva.add(repetido);
        }

        Comparator<Mineral> comp = Comparator.comparingDouble(Mineral::getRatio);

        long tInicio = System.currentTimeMillis();
        List<Mineral> resultado = ordenamiento.quickSort(masiva, comp);
        long tFin = System.currentTimeMillis();

        assertThat(resultado).hasSize(20_000);
        // Gracias a la partición de 3 vías y eliminación de llamadas de cola,
        // no hay StackOverflowError y corre en menos de 300 ms.
        assertThat(tFin - tInicio).isLessThan(500);
    }

    @Test
    @DisplayName("QuickSort con 20.000 elementos aleatorios valida corrección general")
    void quickSortCon20000ElementosAleatorios() {
        Random rnd = new Random(42);
        List<Mineral> listaGrande = new ArrayList<>(20_000);
        for (int i = 0; i < 20_000; i++) {
            listaGrande.add(new Mineral("M-" + i, rnd.nextInt(100) + 1, rnd.nextInt(1000) + 1));
        }

        Comparator<Mineral> comp = Comparator.comparingInt(Mineral::getPeso);

        List<Mineral> resultado = ordenamiento.quickSort(listaGrande, comp);

        assertThat(resultado).hasSize(20_000);
        for (int i = 0; i < resultado.size() - 1; i++) {
            assertThat(resultado.get(i).getPeso()).isLessThanOrEqualTo(resultado.get(i + 1).getPeso());
        }
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

        Mineral unico = new Mineral("Oro Estelar", 1, 100);
        List<Mineral> resUnico = ordenamiento.quickSort(List.of(unico), comp);
        assertThat(resUnico).containsExactly(unico);
    }

    @Test
    @DisplayName("MergeSort ordena correctamente por peso de forma ascendente")
    void mergeSortOrdenaPorPesoAscendente() {
        Comparator<Mineral> comp = Comparator.comparingInt(Mineral::getPeso);

        List<Mineral> ordenados = ordenamiento.mergeSort(mineralesEjemplo, comp);

        assertThat(ordenados).hasSize(4);
        assertThat(ordenados.get(0).getPeso()).isEqualTo(2); // Antimateria
        assertThat(ordenados.get(1).getPeso()).isEqualTo(5);
        assertThat(ordenados.get(2).getPeso()).isEqualTo(5);
        assertThat(ordenados.get(3).getPeso()).isEqualTo(6); // Taquiones
    }

    @Test
    @DisplayName("MergeSort es ESTABLE: mantiene el orden relativo original para claves iguales")
    void mergeSortPreservaEstabilidad() {
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

        assertThat(nombresConClave2).containsExactly("A1", "A2", "A3");
    }

    @Test
    @DisplayName("MergeSort no modifica la lista original de entrada")
    void mergeSortNoModificaListaOriginal() {
        List<Mineral> original = new ArrayList<>(mineralesEjemplo);
        Comparator<Mineral> comp = Comparator.comparingInt(Mineral::getValor);

        ordenamiento.mergeSort(original, comp);

        assertThat(original).containsExactlyElementsOf(mineralesEjemplo);
    }

    @Test
    @DisplayName("MergeSort maneja lista vacía y lista de un único elemento")
    void mergeSortCasosBorde() {
        Comparator<Mineral> comp = Comparator.comparingInt(Mineral::getValor);

        assertThat(ordenamiento.mergeSort(List.of(), comp)).isEmpty();

        Mineral unico = new Mineral("Platino", 3, 50);
        assertThat(ordenamiento.mergeSort(List.of(unico), comp)).containsExactly(unico);
    }
}
