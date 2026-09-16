package com.devtest.myApp.controller;

import com.devtest.myApp.exception.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @Value("${resilience4j.circuitbreaker.instances.externalApi.wait-duration-in-open-state}")
  private String waitDurationInOpenState;

  @ExceptionHandler(ProductNotFoundException.class)
  public ResponseEntity<Void> handleProductNotFound(ProductNotFoundException ex) {
    log.debug(ex.getMessage());
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(CallNotPermittedException.class)
  public ResponseEntity<Void> handleCircuitOpen(CallNotPermittedException ex) {
    log.warn("Circuit breaker is open for the external API: {}", ex.getMessage());
    long retryAfterSeconds = DurationStyle.detectAndParse(waitDurationInOpenState).toSeconds();
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("Retry-After", String.valueOf(retryAfterSeconds))
        .build();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Void> handleUnexpected(Exception ex) {
    log.error("Unexpected error handling request", ex);
    return ResponseEntity.internalServerError().build();
  }
}
