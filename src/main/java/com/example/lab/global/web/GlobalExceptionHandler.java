package com.example.lab.global.web;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.global.error.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(ApplicationException exception) {
        var errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorResponse> errors = exception.getConstraintViolations().stream()
                .map(violation ->
                        new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();
        var errorCode = CommonErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode, errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        var errorCode = CommonErrorCode.INVALID_REQUEST;
        List<FieldErrorResponse> errors = List.of(new FieldErrorResponse(exception.getName(), "허용되지 않는 값입니다."));
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("unexpected_error", exception);
        var errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = exception.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String field = error.getObjectName();
                    if (error instanceof FieldError fieldError) {
                        field = fieldError.getField();
                    }
                    String message = error.getDefaultMessage();
                    if (message == null) {
                        message = "유효하지 않은 값입니다.";
                    }
                    return new FieldErrorResponse(field, message);
                })
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();
        var errorCode = CommonErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode, errors));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldErrorResponse> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String parameterName = result.getMethodParameter().getParameterName();
                    String field = parameterName;
                    if (field == null) {
                        field = "request";
                    }
                    String resolvedField = field;
                    return result.getResolvableErrors().stream().map(error -> {
                        String message = error.getDefaultMessage();
                        if (message == null) {
                            message = "유효하지 않은 값입니다.";
                        }
                        return new FieldErrorResponse(resolvedField, message);
                    });
                })
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();
        var errorCode = CommonErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode, errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errorCode = CommonErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var errorCode = CommonErrorCode.INVALID_REQUEST;
        List<FieldErrorResponse> errors = List.of(new FieldErrorResponse(exception.getParameterName(), "필수 값입니다."));
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode, errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var errorCode = CommonErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var errorCode = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errorCode = CommonErrorCode.NOT_FOUND;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errorCode = CommonErrorCode.NOT_FOUND;
        return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
    }
}
