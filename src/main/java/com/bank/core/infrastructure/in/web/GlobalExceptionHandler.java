package com.bank.core.infrastructure.in.web;

import com.bank.core.domain.exception.AccountBlockedException;
import com.bank.core.domain.exception.AccountNotFoundException;
import com.bank.core.domain.exception.DuplicateTransactionException;
import com.bank.core.domain.exception.InsufficientFundsException;
import com.bank.core.infrastructure.in.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Global exception handler that maps domain and infrastructure exceptions to
 * standardised HTTP error responses.
 *
 * <p>All error responses share the {@link ApiErrorResponse} envelope, ensuring
 * API consumers can parse failures uniformly. Exception-to-status mappings:</p>
 *
 * <ul>
 *   <li>{@link AccountNotFoundException} → 404 Not Found</li>
 *   <li>{@link InsufficientFundsException} → 422 Unprocessable Entity</li>
 *   <li>{@link AccountBlockedException} → 422 Unprocessable Entity</li>
 *   <li>{@link DuplicateTransactionException} → 409 Conflict</li>
 *   <li>{@link OptimisticLockingFailureException} → 409 Conflict</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request (with field errors)</li>
 *   <li>{@link Exception} (catch-all) → 500 Internal Server Error</li>
 * </ul>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link AccountNotFoundException} (HTTP 404).
     *
     * @param ex      the exception
     * @param request the originating HTTP request
     * @return a standardised error response
     */
    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleAccountNotFound(AccountNotFoundException ex,
                                                   HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Handles {@link InsufficientFundsException} (HTTP 422).
     *
     * @param ex      the exception
     * @param request the originating HTTP request
     * @return a standardised error response
     */
    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleInsufficientFunds(InsufficientFundsException ex,
                                                     HttpServletRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /**
     * Handles {@link AccountBlockedException} (HTTP 422).
     *
     * @param ex      the exception
     * @param request the originating HTTP request
     * @return a standardised error response
     */
    @ExceptionHandler(AccountBlockedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleAccountBlocked(AccountBlockedException ex,
                                                  HttpServletRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /**
     * Handles {@link DuplicateTransactionException} (HTTP 409).
     *
     * @param ex      the exception
     * @param request the originating HTTP request
     * @return a standardised error response
     */
    @ExceptionHandler(DuplicateTransactionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicateTransaction(DuplicateTransactionException ex,
                                                        HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Handles {@link OptimisticLockingFailureException} (HTTP 409).
     *
     * <p>This is thrown when two concurrent transfer requests target the same account
     * and one of them loses the optimistic lock race. The losing request should be
     * retried by the client.</p>
     *
     * @param ex      the exception
     * @param request the originating HTTP request
     * @return a standardised error response
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleOptimisticLock(OptimisticLockingFailureException ex,
                                                  HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT,
                "Concurrent modification conflict, please retry the request", request);
    }

    /**
     * Handles {@link MethodArgumentNotValidException} (HTTP 400).
     *
     * <p>Returns a list of field-level validation errors in the {@code errors} field
     * of the response envelope.</p>
     *
     * @param ex      the exception
     * @param request the originating HTTP request
     * @return a standardised error response with field-level details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex,
                                              HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }

    /**
     * Catch-all handler for unexpected exceptions (HTTP 500).
     *
     * @param ex      the exception
     * @param request the originating HTTP request
     * @return a standardised error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred", request);
    }

    /**
     * Builds a standard {@link ApiErrorResponse} with an empty field-error list.
     *
     * @param status  the HTTP status to encode in the response body
     * @param message the developer-friendly error message
     * @param request the originating HTTP request
     * @return the assembled error response
     */
    private ApiErrorResponse buildError(HttpStatus status, String message,
                                         HttpServletRequest request) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                List.of()
        );
    }
}
