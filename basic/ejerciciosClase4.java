package basic;

public class ejerciciosClase4 {
    public static void main(String[] args) {
        int matriz1[][] = {{2,3,4},{3,5,2},{2,5,4}};
        int vector1[] = {2,5,4};
        System.out.println(coincidenDiagonalYUnaFila(matriz1, vector1)); // verdadero

        int matriz2[][] = {{2,3,4},{3,5,2},{7,2,4}};
        int vector2[] = {3,5,1};
        System.out.println(coincidenDiagonalYUnaFila(matriz2, vector2)); // falso
    }

    private static boolean coincidenDiagonalYUnaFila(int[][] matriz, int[] vector) {
        return coincideDiagonal(matriz,vector) && coincideUnaFila(matriz,vector);    
    } 

    private static boolean coincideUnaFila(int[][] matriz, int[] vector) {
        boolean rta = false; //1
        for(int i=0; i < matriz.length; i++) { //3n + 3
            rta = rta || coincidenFilaVector(matriz[i], vector); // 2n+n(v(n)) = 2n + n (7n + 5) = 2n + 7n^2 + 5n
        }
        return rta; // 1
    } //m(n) = 

    private static boolean coincidenFilaVector(int[] fila, int[] vector) {
        boolean rta = true; // 1
        for (int i = 0; i < vector.length; i++) { // 3 + 3n
            rta = rta && fila[i] == vector[i]; // 3n
        }
        return rta; // 1
    } //v(n) = 7n + 5

    private static boolean coincideDiagonal(int[][] matriz, int[] vector) {
        boolean rta = true; //1
        for (int i = 0; i < vector.length; i++) {  // 3 + 3n = 1+2n+n
            rta = rta && matriz[i][i]==vector[i]; //5n
        }
        return rta; //1
    } //f(n) = 1 + 3 + 3n + 5n + 1 = 8n + 5 

//2. Calcular y justificar complejidad asintótica 
    public static int sumaCuadratica(int[] arr, int i, int f) {
        if (i == f) return arr[i];
        int m = (i + f) / 2;
        int suma = 0;
        // Bucle doble dentro de cada llamada -> O(n^2)
        for (int x = i; x <= f; x++) {
            for (int y = i; y <= f; y++) {
                suma += arr[x] * arr[y];
            }
        }
        return sumaCuadratica(arr, i, m) + sumaCuadratica(arr, m + 1, f) + suma;
    } 

//3. Calcular y justificar complejidad asintótica 
    public static int contarSubconjuntos(int[] arr, int i, int suma) {
        if (i == arr.length) {
            return suma == 0 ? 1 : 0;
        }
        int incluir = contarSubconjuntos(arr, i + 1, suma - arr[i]);
        int excluir = contarSubconjuntos(arr, i + 1, suma);
        return incluir + excluir; 
    }
}
