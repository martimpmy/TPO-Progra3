package basic;
//n = lineal
//1 = constante

public class ejerciciosClase3 {
    public static void main(String[] args) {
        int matriz1[][] = {{2,3,4},{3,5,2},{7,2,4}}; // 1
        int vector1[] = {3,5,2}; // 1
        System.out.println(verificarCoincidencia(matriz1, vector1, 3, 3)); // verdadero
        //f(n) = 2n**3 + 4n**2 + 5n + 3

        int matriz2[][] = {{2,3,4},{3,5,2},{7,2,4}}; // 1
        int vector2[] = {3,5,1}; //  1
        System.out.println(verificarCoincidencia(matriz2, vector2, 3, 3)); // falso
        //f(n) = 2n**3 + 4n**2 + 5n + 3 
    }//1 + 1 + 2n**3 + 4n**2 + 5n + 3 +1 + 1 + 2n**3 + 4n**2 +5n + 3 = 4n**3 + 8n**2 + 10n + 10

    // Función principal: controla filas y columnas
    public static boolean verificarCoincidencia(int[][] matriz, int[] vector, int filas, int columnas) {
        //1+n+n = 1+2n
        for (int i = 0; i < filas; i++) {
            if (!verificarFila(matriz, vector, i, columnas)) { // n*r(n) = 2n**3 + 4n**2 + 3n
                return false; // 1
            }
        }
        return true; //1
        //f(n) = 1+2n + 2n**3 + 4n**2 + 3n + 1 + 1 = 2n**3 + 4n**2 + 5n + 3
    }


    // Verifica una fila completa
    private static boolean verificarFila(int[][] matriz, int[] vector, int fila, int columnas) {
        for (int j = 0; j < columnas; j++) {
            //1+n+n = 1+2n
            if (!compararCeldaVariasVeces(matriz[fila][j], vector[j], columnas)) { // n*t(n)= 3n**2 + 3n
                return false; //1
            }
        }
        return true; //1
        //r(n) = 1 + 2n + 3n**2 + 3n + 1 + 1 = 
   }

    // Comparación repetida artificialmente (cubo)
    private static boolean compararCeldaVariasVeces(int valorCelda, int valorVector, int repeticiones) {
        for (int k = 0; k < repeticiones; k++) { // bucle extra para complejidad cúbica
            //1+n+n = 1+2n
            if (valorCelda != valorVector) { //n
                return false; //1
            }
        }
        return true; //1
        //t(n) = 1 + 2n + n + 1 = 3n +3 
    }
    //2-	Calcular complejidad asintótica
    public static int calcular(int[] arr, int i, int f) { //caso division
        if (i == f) return arr[i];
        int n = (i + f) / 2;
        int da = calcular(arr, i, n);
        int ha = calcular(arr, n + 1, f);
        return da + ha;
    }
    // a = 2, la canidad de veces que se llama la recursividad
    // b = 2, 
    // k = 0
    // caso a>b**k
    //0(n log b(a)) => 0(n log 2(2)) => 0(n)
}
  