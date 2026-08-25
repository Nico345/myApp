package com.devtest.myApp.controller;

import com.devtest.myApp.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  public ResponseEntity<Void> handleProductNotFound(ProductNotFoundException ex) {
    log.debug(ex.getMessage());
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Void> handleUnexpected(Exception ex) {
    log.error("Unexpected error handling request", ex);
    return ResponseEntity.internalServerError().build();
  }
}
