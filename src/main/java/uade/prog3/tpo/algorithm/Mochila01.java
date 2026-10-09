
package uade.prog3.tpo.algorithm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import uade.prog3.tpo.model.Mineral;

/**
 * Mochila 0/1 mediante Programación Dinámica.
 *
 * Cada mineral puede seleccionarse como máximo una vez.
 *
 * dp[i][c] representa el máximo valor que puede obtenerse usando los primeros
 * i minerales con una capacidad disponible de c unidades de peso.
 *
 * La matriz se conserva para permitir la recuperación de los ítems elegidos.
 *
 * Complejidad temporal: O(N * W).
 * Complejidad espacial: O(N * W).
 */
 // >>> HITO 6: algoritmo nuevo de mochila 0/1 con DP y recuperación de minerales.
public class Mochila01 {

    private static final double EPSILON = 1e-9;
    private static final int MAX_DECIMALES = 6;

    public ResultadoMochila resolver(List<Mineral> minerales, double capacidad) {

        // >>> HITO 6: validamos la capacidad.
        if (capacidad < 0 || Double.isInfinite(capacidad) || Double.isNaN(capacidad)) {
            throw new IllegalArgumentException(
                    "La capacidad de la bodega debe ser un número no negativo"
            );
        }

        // Si no hay minerales, devolvemos una solución vacía.
        if (minerales == null) {
            return new ResultadoMochila(
                    List.of(), 0, 0, new double[][]{{0}}, 1
            );
        }

        // >>> HITO 6: convertimos los pesos a unidades enteras.
        // Esto permite trabajar con pesos decimales sin usarlos como índices.
        int escala = calcularEscala(minerales, capacidad);
        int capacidadEscalada = escalar(capacidad, escala);

        int n = minerales.size();
        int[] pesos = new int[n];

        for (int i = 0; i < n; i++) {
            Mineral mineral = minerales.get(i);

            if (mineral == null) {
                throw new IllegalArgumentException(
                        "La lista de minerales no puede contener elementos nulos"
                );
            }

            pesos[i] = escalar(mineral.getPeso(), escala);
        }

        // >>> HITO 6 - DP:
        // Filas = cantidad de minerales considerados.
        // Columnas = capacidad disponible.
        double[][] dp = new double[n + 1][capacidadEscalada + 1];

        for (int i = 1; i <= n; i++) {
            Mineral mineral = minerales.get(i - 1);
            int peso = pesos[i - 1];
            double valor = mineral.getValor();

            for (int c = 0; c <= capacidadEscalada; c++) {

                // Caso 1: no tomar el mineral.
                dp[i][c] = dp[i - 1][c];

                // Caso 2: tomarlo si entra en la capacidad.
                if (peso <= c) {
                    double conMineral = dp[i - 1][c - peso] + valor;

                    if (conMineral > dp[i][c]) {
                        dp[i][c] = conMineral;
                    }
                }
            }
        }

        // >>> HITO 6 - RECUPERACIÓN DE ÍTEMS:
        // Recorremos la matriz desde la última fila hacia la primera
        // para identificar qué minerales forman la solución óptima.
        List<Mineral> seleccionados = new ArrayList<>();
        int capacidadRestante = capacidadEscalada;

        for (int i = n; i >= 1; i--) {

            if (Math.abs(
                    dp[i][capacidadRestante]
                    - dp[i - 1][capacidadRestante]
            ) > EPSILON) {

                Mineral mineral = minerales.get(i - 1);
                seleccionados.add(mineral);
                capacidadRestante -= pesos[i - 1];
            }
        }

        // La recuperación se hizo hacia atrás.
        // Invertimos la lista para mantener el orden original.
        for (int izquierda = 0, derecha = seleccionados.size() - 1;
             izquierda < derecha;
             izquierda++, derecha--) {

            Mineral temporal = seleccionados.get(izquierda);
            seleccionados.set(izquierda, seleccionados.get(derecha));
            seleccionados.set(derecha, temporal);
        }

        // >>> HITO 6: calculamos el peso y el valor de los minerales elegidos.
        double pesoOcupado = seleccionados.stream()
                .mapToDouble(Mineral::getPeso)
                .sum();

        double valorTotal = seleccionados.stream()
                .mapToDouble(Mineral::getValor)
                .sum();

        return new ResultadoMochila(
                List.copyOf(seleccionados),
                pesoOcupado,
                valorTotal,
                dp,
                escala
        );
    }

    // >>> HITO 6: determina la escala necesaria para representar
    // los pesos decimales como enteros.
    private int calcularEscala(List<Mineral> minerales, double capacidad) {

        int escalaDecimal = escalaDecimal(BigDecimal.valueOf(capacidad));

        for (Mineral mineral : minerales) {
            if (mineral == null) {
                continue;
            }

            escalaDecimal = Math.max(
                    escalaDecimal,
                    escalaDecimal(BigDecimal.valueOf(mineral.getPeso()))
            );
        }

        if (escalaDecimal > MAX_DECIMALES) {
            throw new IllegalArgumentException(
                    "Los pesos de la mochila admiten como máximo "
                    + MAX_DECIMALES + " decimales"
            );
        }

        return (int) Math.pow(10, escalaDecimal);
    }

    private int escalaDecimal(BigDecimal numero) {
        return Math.max(numero.stripTrailingZeros().scale(), 0);
    }

    // Convierte un peso o una capacidad a unidades enteras.
    private int escalar(double valor, int escala) {

        double escalado = valor * escala;

        if (escalado > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "La capacidad/peso escalado es demasiado grande para la matriz DP"
            );
        }

        return (int) Math.round(escalado);
    }

    // >>> HITO 6: resultado del algoritmo.
    // Incluye los minerales elegidos, el peso, el valor,
    // la matriz completa y el factor de escala.
    public record ResultadoMochila(
            List<Mineral> minerales,
            double pesoOcupado,
            double valorTotal,
            double[][] matrizDP,
            int factorEscala
    ) {
    }
}
