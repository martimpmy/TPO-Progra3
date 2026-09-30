package uade.prog3.tpo.dto;

import uade.prog3.tpo.model.Mineral;

public record MineralDTO(
        String nombre,
        double peso,
        double valor,
        Double ratio
) {
    public static MineralDTO fromModel(Mineral mineral) {
        double r = Math.round(mineral.getRatio() * 100.0) / 100.0;
        return new MineralDTO(mineral.getNombre(), mineral.getPeso(), mineral.getValor(), r);
    }

    public Mineral toModel() {
        return new Mineral(nombre, peso, valor);
    }
}
