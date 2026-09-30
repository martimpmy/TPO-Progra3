package uade.prog3.tpo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import uade.prog3.tpo.model.Mineral;

/**
 * Mineral en la API. En la entrada solo se usan nombre, peso y valor;
 * id y ratio los completa el servidor (id solo si el mineral esta persistido).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MineralDTO(
        String id,
        String nombre,
        Double peso,
        Double valor,
        Double ratio
) {
    public static MineralDTO fromModel(Mineral mineral) {
        double r = Math.round(mineral.getRatio() * 100.0) / 100.0;
        return new MineralDTO(mineral.getId(), mineral.getNombre(), mineral.getPeso(), mineral.getValor(), r);
    }

    public Mineral toModel() {
        if (peso == null || valor == null) {
            throw new IllegalArgumentException("Cada mineral debe tener 'nombre', 'peso' y 'valor'");
        }
        return new Mineral(nombre, peso, valor);
    }
}
