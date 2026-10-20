package org.arthur.genericapi.exception;

/**
 * Violacao de uma regra de negocio do dominio.
 * Traduzida em HTTP 422 pelo {@code GlobalExceptionHandler}.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}