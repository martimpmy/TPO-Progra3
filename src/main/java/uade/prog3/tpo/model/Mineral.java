package uade.prog3.tpo.model;

import java.util.Objects;

/**
 * Entidad de dominio que representa un lote de minerales o recursos comerciales
 * disponibles para ser transportados en la bodega de carga de la nave espacial.
 *
 * Cada mineral posee un peso físico (en toneladas métricas enteras w_i ∈ Z+)
 * y un valor comercial (en Créditos Galácticos enteros v_i ∈ Z+).
 */
public class Mineral {

    private String id;
    private String nombre;
    private int peso;
    private int valor;

    public Mineral() {
    }

    public Mineral(String nombre, int peso, int valor) {
        this(nombre != null ? nombre.toLowerCase().replace(" ", "_") : null, nombre, peso, valor);
    }

    public Mineral(String id, String nombre, int peso, int valor) {
        if (peso <= 0) {
            throw new IllegalArgumentException("El peso del mineral debe ser mayor a 0");
        }
        if (valor < 0) {
            throw new IllegalArgumentException("El valor del mineral no puede ser negativo");
        }
        this.id = id;
        this.nombre = nombre;
        this.peso = peso;
        this.valor = valor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getPeso() {
        return peso;
    }

    public void setPeso(int peso) {
        if (peso <= 0) {
            throw new IllegalArgumentException("El peso del mineral debe ser mayor a 0");
        }
        this.peso = peso;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("El valor del mineral no puede ser negativo");
        }
        this.valor = valor;
    }

    /**
     * Ratio comercial rentabilidad = valor / peso (Créditos Galácticos por tonelada).
     */
    public double getRatio() {
        return peso > 0 ? (double) valor / peso : 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mineral mineral = (Mineral) o;
        return peso == mineral.peso &&
                valor == mineral.valor &&
                Objects.equals(nombre, mineral.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, peso, valor);
    }

    @Override
    public String toString() {
        return "Mineral{" +
                "nombre='" + nombre + '\'' +
                ", peso=" + peso +
                ", valor=" + valor +
                ", ratio=" + getRatio() +
                '}';
    }
}
