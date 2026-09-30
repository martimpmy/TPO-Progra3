package uade.prog3.tpo.config;

import java.time.LocalDateTime;

import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uade.prog3.tpo.dto.ErrorResponseDTO;

/**
 * Interceptor global: ningun error llega al cliente como stack trace,
 * siempre se responde un ErrorResponseDTO.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> argumentoInvalido(IllegalArgumentException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class, Neo4jException.class})
    public ResponseEntity<ErrorResponseDTO> baseNoDisponible(RuntimeException e) {
        log.error("Error accediendo a Neo4j", e);
        return responder(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo acceder a la base de datos Neo4j. Verificar NEO4J_URI y credenciales.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> errorInesperado(Exception e) {
        log.error("Error inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");
    }

    private ResponseEntity<ErrorResponseDTO> responder(HttpStatus status, String mensaje) {
        ErrorResponseDTO body = new ErrorResponseDTO(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
