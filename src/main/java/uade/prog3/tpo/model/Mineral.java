package uade.prog3.tpo.model;

import java.util.Objects;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * Lote de mineral o recurso comercial transportable en la bodega de la nave.
 * Se persiste en Neo4j como (:Mineral {id, nombre, peso, valor}).
 *
 * peso:  toneladas metricas (> 0).
 * valor: Creditos Galacticos (>= 0).
 */
@Node("Mineral")
public class Mineral {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;
    private String nombre;
    private double peso;
    private double valor;

    /** Requerido por Spring Data Neo4j para reconstruir el nodo. */
    public Mineral() {
    }

    public Mineral(String nombre, double peso, double valor) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del mineral es obligatorio");
        }
        if (!(peso > 0) || Double.isInfinite(peso)) {
            throw new IllegalArgumentException("El peso del mineral debe ser mayor a 0");
        }
        if (!(valor >= 0) || Double.isInfinite(valor)) {
            throw new IllegalArgumentException("El valor del mineral no puede ser negativo");
        }
        this.nombre = nombre.trim();
        this.peso = peso;
        this.valor = valor;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPeso() {
        return peso;
    }

    public double getValor() {
        return valor;
    }

    /** Rentabilidad = valor / peso (Creditos Galacticos por tonelada). */
    public double getRatio() {
        return peso > 0 ? valor / peso : 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mineral mineral = (Mineral) o;
        return Double.compare(mineral.peso, peso) == 0
                && Double.compare(mineral.valor, valor) == 0
                && Objects.equals(nombre, mineral.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, peso, valor);
    }

    @Override
    public String toString() {
        return "Mineral{nombre='" + nombre + "', peso=" + peso + ", valor=" + valor + ", ratio=" + getRatio() + '}';
    }
}
