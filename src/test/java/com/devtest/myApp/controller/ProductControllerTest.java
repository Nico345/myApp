package com.devtest.myApp.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.devtest.myApp.dto.ProductDetailDto;
import com.devtest.myApp.exception.ProductNotFoundException;
import com.devtest.myApp.service.ProductService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ProductService productService;

  @Test
  void returns200WithSimilarProductDetails() throws Exception {
    when(productService.getSimilarProducts(eq("1")))
        .thenReturn(
            Mono.just(
                List.of(
                    new ProductDetailDto("2", "Product 2", 10.0, true),
                    new ProductDetailDto("3", "Product 3", 20.0, false))));

    MvcResult mvcResult =
        mockMvc.perform(get("/product/1/similar")).andExpect(request().asyncStarted()).andReturn();

    mockMvc
        .perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("2"))
        .andExpect(jsonPath("$[1].id").value("3"));
  }

  @Test
  void returns200WithEmptyListWhenNoSimilarProducts() throws Exception {
    when(productService.getSimilarProducts(eq("1"))).thenReturn(Mono.just(List.of()));

    MvcResult mvcResult =
        mockMvc.perform(get("/product/1/similar")).andExpect(request().asyncStarted()).andReturn();

    mockMvc
        .perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void returns404WhenBaseProductDoesNotExist() throws Exception {
    when(productService.getSimilarProducts(eq("missing")))
        .thenReturn(Mono.error(new ProductNotFoundException("missing")));

    MvcResult mvcResult =
        mockMvc
            .perform(get("/product/missing/similar"))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isNotFound());
  }

  @Test
  void returns503WithRetryAfterWhenCircuitIsOpen() throws Exception {
    CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
    CallNotPermittedException circuitOpenException =
        CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

    when(productService.getSimilarProducts(eq("1"))).thenReturn(Mono.error(circuitOpenException));

    MvcResult mvcResult =
        mockMvc.perform(get("/product/1/similar")).andExpect(request().asyncStarted()).andReturn();

    mockMvc
        .perform(asyncDispatch(mvcResult))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void returns500OnUnexpectedError() throws Exception {
    when(productService.getSimilarProducts(eq("1"))).thenReturn(Mono.error(new RuntimeException()));

    MvcResult mvcResult =
        mockMvc.perform(get("/product/1/similar")).andExpect(request().asyncStarted()).andReturn();

    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isInternalServerError());
  }
}
