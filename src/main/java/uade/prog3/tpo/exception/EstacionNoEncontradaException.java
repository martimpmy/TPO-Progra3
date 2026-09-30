package uade.prog3.tpo.exception;

public class EstacionNoEncontradaException extends RuntimeException {

    public EstacionNoEncontradaException(String id) {
        super("No existe la estacion con id '" + id + "'");
    }
}
