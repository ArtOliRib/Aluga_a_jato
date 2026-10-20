package org.arthur.genericapi.exception;

/**
 * Recurso solicitado nao existe. Traduzida em HTTP 404 pelo {@code GlobalExceptionHandler}.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(recurso + " nao encontrado com id " + id);
    }
}