package org.arthur.genericapi.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Tradutor unico de excecoes para HTTP. Os controllers nao conhecem status
 * code nem formato de erro: apenas lancam excecoes de dominio.
 * O corpo da resposta segue o formato ProblemDetail (RFC 7807).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI TIPO_VALIDACAO = URI.create("https://problemas.locadora/validacao");
    private static final URI TIPO_CONFLITO = URI.create("https://problemas.locadora/conflito");

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ProblemDetail> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio violada", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidacaoDeBody(MethodArgumentNotValidException ex) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of(
                        "campo", erro.getField(),
                        "mensagem", erro.getDefaultMessage() == null ? "valor invalido" : erro.getDefaultMessage()
                ))
                .toList();

        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Um ou mais campos invalidos");
        detalhe.setType(TIPO_VALIDACAO);
        detalhe.setTitle("Erro de validacao");
        detalhe.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(detalhe);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleValidacaoDeParametro(ConstraintViolationException ex) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detalhe.setType(TIPO_VALIDACAO);
        detalhe.setTitle("Erro de validacao");
        return ResponseEntity.badRequest().body(detalhe);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleParametroObrigatorio(MissingServletRequestParameterException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Parametro obrigatorio ausente",
                "O parametro '" + ex.getParameterName() + "' e obrigatorio");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor invalido",
                "O valor '" + ex.getValue() + "' nao e valido para " + ex.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido",
                "Verifique o JSON enviado e os valores dos campos enumerados");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleConflitoDeIntegridade(DataIntegrityViolationException ex) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Ja existe um registro com um dos valores enviados");
        detalhe.setType(TIPO_CONFLITO);
        detalhe.setTitle("Conflito de dados");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detalhe);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleErroInesperado(Exception ex) {
        log.error("Erro inesperado nao tratado", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    private ResponseEntity<ProblemDetail> resposta(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detalhe);
        body.setTitle(titulo);
        return ResponseEntity.status(status).body(body);
    }
}