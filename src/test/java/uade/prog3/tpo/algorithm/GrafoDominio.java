package uade.prog3.tpo.algorithm;

/**
 * Grafo semilla del dominio (8 estaciones, 12 rutas) armado en memoria para los tests,
 * con el mismo orden que produce GrafoService al leer de Neo4j: vértices y vecinos
 * en orden alfabético de id.
 */
public final class GrafoDominio {

    private GrafoDominio() {
    }

    public static Grafo crear() {
        Grafo g = new Grafo();
        g.agregarVertice("ALPHA", "Alpha Centauri");
        g.agregarVertice("CITADEL", "Ciudadela Omega");
        g.agregarVertice("KEPLER", "Colonia Kepler");
        g.agregarVertice("NOVA", "Puesto Nova");
        g.agregarVertice("ORION", "Nebulosa Orion");
        g.agregarVertice("SIRIUS", "Puerto Sirio");
        g.agregarVertice("SOL", "Base Solar");
        g.agregarVertice("VEGA", "Minas de Vega");

        g.agregarArista("ALPHA", "SIRIUS", 8);
        g.agregarArista("ALPHA", "SOL", 12);
        g.agregarArista("ALPHA", "VEGA", 18);
        g.agregarArista("CITADEL", "NOVA", 7);
        g.agregarArista("CITADEL", "ORION", 19);
        g.agregarArista("KEPLER", "NOVA", 14);
        g.agregarArista("KEPLER", "ORION", 9);
        g.agregarArista("KEPLER", "SIRIUS", 15);
        g.agregarArista("KEPLER", "VEGA", 10);
        g.agregarArista("NOVA", "ORION", 11);
        g.agregarArista("ORION", "VEGA", 22);
        g.agregarArista("SIRIUS", "SOL", 25);
        return g;
    }
}
