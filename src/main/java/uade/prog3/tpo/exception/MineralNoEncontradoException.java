package uade.prog3.tpo.exception;

public class MineralNoEncontradoException extends RuntimeException {

    public MineralNoEncontradoException(String id) {
        super("No existe el mineral con id '" + id + "'");
    }
}
