package com.devtest.myApp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.devtest.myApp.exception.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    ReflectionTestUtils.setField(handler, "waitDurationInOpenState", "10s");
  }

  @Test
  void mapsProductNotFoundTo404() {
    ResponseEntity<Void> response =
        handler.handleProductNotFound(new ProductNotFoundException("42"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void mapsCircuitOpenTo503WithRetryAfterHeader() {
    CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
    CallNotPermittedException ex =
        CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

    ResponseEntity<Void> response = handler.handleCircuitOpen(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("10");
  }

  @Test
  void parsesDifferentWaitDurationFormatsCorrectly() {
    ReflectionTestUtils.setField(handler, "waitDurationInOpenState", "500ms");

    CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
    CallNotPermittedException ex =
        CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

    ResponseEntity<Void> response = handler.handleCircuitOpen(ex);

    assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("0");
  }

  @Test
  void mapsUnexpectedExceptionTo500() {
    ResponseEntity<Void> response = handler.handleUnexpected(new RuntimeException());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
