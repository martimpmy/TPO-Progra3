package uade.prog3.tpo.config;

import java.time.LocalDateTime;

import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uade.prog3.tpo.dto.ErrorResponseDTO;
import uade.prog3.tpo.exception.EstacionNoEncontradaException;
import uade.prog3.tpo.exception.MineralNoEncontradoException;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> cuerpoInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido o tiene tipos incorrectos.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> parametroFaltante(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + e.getParameterName() + "'.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> parametroMalTipado(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + e.getName() + "'.");
    }

    @ExceptionHandler({EstacionNoEncontradaException.class, MineralNoEncontradoException.class})
    public ResponseEntity<ErrorResponseDTO> noEncontrado(RuntimeException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "No existe el recurso '/" + e.getResourcePath() + "'.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> metodoNoSoportado(HttpRequestMethodNotSupportedException e) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " no soportado para esta ruta.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> tipoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo debe enviarse como application/json.");
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
