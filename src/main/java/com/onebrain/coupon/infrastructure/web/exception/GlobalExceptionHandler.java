package com.onebrain.coupon.infrastructure.web.exception;

import com.onebrain.coupon.domain.exception.CouponJaApagadoException;
import com.onebrain.coupon.domain.exception.CouponNotFoundException;
import com.onebrain.coupon.domain.exception.RegraNegocioException;
import com.onebrain.coupon.infrastructure.exception.ApagarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.exception.BuscarCouponRepositoryException;
import com.onebrain.coupon.infrastructure.exception.PersistenceCouponException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorMessage> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String fieldName = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";

        String message = String.format("Valor '%s' inválido para o campo %s", value, fieldName);

        log.warn("m=handleTypeMismatch, msg=Parâmetro inválido: campo={}", fieldName);
        ApiErrorMessage error = new ApiErrorMessage(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> {
                    if ("NotNull".equals(err.getCode()) || "NotEmpty".equals(err.getCode()) || "NotBlank".equals(err.getCode())) {
                        return "Campo obrigatório ausente: " + err.getField();
                    } else {
                        return "Erro no campo " + err.getField() + ": " + err.getDefaultMessage();
                    }
                })
                .toList();

        log.warn("m=handleMethodArgumentNotValid, msg=Requisição inválida: {}", errors);
        ApiErrorMessage apiErrorMessage = new ApiErrorMessage(HttpStatus.BAD_REQUEST, errors);
        return new ResponseEntity<>(apiErrorMessage, headers, apiErrorMessage.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("m=handleHttpMessageNotReadable, msg=Corpo da requisição inválido ou mal formatado");
        ApiErrorMessage apiErrorMessage = new ApiErrorMessage(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado");
        return new ResponseEntity<>(apiErrorMessage, headers, apiErrorMessage.getStatus());
    }

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<?> handleCouponNotFoundException(CouponNotFoundException ex) {
        log.warn("m=handleCouponNotFoundException, msg={}", ex.getMessage());
        ApiErrorMessage apiErrorMessage = new ApiErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage());
        return new ResponseEntity<>(apiErrorMessage, new HttpHeaders(), apiErrorMessage.getStatus());
    }

    @ExceptionHandler(CouponJaApagadoException.class)
    public ResponseEntity<?> handleCouponJaApagadoException(CouponJaApagadoException ex) {
        log.warn("m=handleCouponJaApagadoException, msg={}", ex.getMessage());
        ApiErrorMessage apiErrorMessage = new ApiErrorMessage(HttpStatus.CONFLICT, ex.getMessage());
        return new ResponseEntity<>(apiErrorMessage, new HttpHeaders(), apiErrorMessage.getStatus());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<?> handleRegraNegocioException(RegraNegocioException ex) {
        log.warn("m=handleRegraNegocioException, msg=Regra de negócio violada: {}", ex.getMessage());
        ApiErrorMessage apiErrorMessage = new ApiErrorMessage(HttpStatus.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(apiErrorMessage, new HttpHeaders(), apiErrorMessage.getStatus());
    }

    @ExceptionHandler({
            PersistenceCouponException.class,
            BuscarCouponRepositoryException.class,
            ApagarCouponRepositoryException.class
    })
    public ResponseEntity<?> handleRepositoryException(RuntimeException ex) {
        log.error("m=handleRepositoryException, msg=Falha de persistência", ex);
        ApiErrorMessage apiErrorMessage = new ApiErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return new ResponseEntity<>(apiErrorMessage, new HttpHeaders(), apiErrorMessage.getStatus());
    }
}
