package org.arthur.genericapi.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * Content-Type que a API nao sabe ler (ex.: {@code text/plain}).
     *
     * <p>Sem este handler, a excecao caia no {@code Exception.class} generico e o
     * cliente recebia 500 - sugerindo que a API quebrou, quando na verdade a
     * requisicao estava malformada. 415 diz exatamente o que corrigir.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleTipoDeMidiaNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteudo nao suportado",
                "Envie o corpo com Content-Type: application/json");
    }

    /**
     * Rota que nao existe.
     *
     * <p>No Spring Boot 4 isso chega como {@code NoResourceFoundException} (antes
     * era {@code NoHandlerFoundException}). Sem tratar, tambem virava 500: um
     * front que digita a URL errada recebia "erro interno" em vez de 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleRotaInexistente(NoResourceFoundException ex) {
        return resposta(HttpStatus.NOT_FOUND, "Rota nao encontrada",
                "O caminho '" + ex.getResourcePath() + "' nao existe nesta API");
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