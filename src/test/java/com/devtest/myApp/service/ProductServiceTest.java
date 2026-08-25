package com.devtest.myApp.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devtest.myApp.client.ProductClient;
import com.devtest.myApp.dto.ProductDetailDto;
import com.devtest.myApp.exception.ProductNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ProductServiceTest {

  private ProductClient productClient;
  private ProductService service;

  @BeforeEach
  void setUp() {
    productClient = mock(ProductClient.class);
    service = new ProductServiceImpl(productClient);
  }

  @Test
  void returnsDetailsInSimilarityOrder() {
    when(productClient.getSimilarProductsIds("1")).thenReturn(Mono.just(List.of("2", "3")));
    when(productClient.getProductDetailById("2"))
        .thenReturn(Mono.just(new ProductDetailDto("2", "Product 2", 10.0, true)));
    when(productClient.getProductDetailById("3"))
        .thenReturn(Mono.just(new ProductDetailDto("3", "Product 3", 20.0, false)));

    StepVerifier.create(service.getSimilarProducts("1"))
        .assertNext(
            list -> {
              org.assertj.core.api.Assertions.assertThat(list)
                  .extracting(ProductDetailDto::id)
                  .containsExactly("2", "3");
            })
        .verifyComplete();
  }

  @Test
  void failsWholeRequestWhenAnySimilarProductCannotBeFetched() {
    when(productClient.getSimilarProductsIds("1")).thenReturn(Mono.just(List.of("2", "3", "4")));
    when(productClient.getProductDetailById("2"))
        .thenReturn(Mono.just(new ProductDetailDto("2", "Product 2", 10.0, true)));
    when(productClient.getProductDetailById("3"))
        .thenReturn(Mono.error(new RuntimeException("missing")));
    when(productClient.getProductDetailById("4"))
        .thenReturn(Mono.just(new ProductDetailDto("4", "Product 4", 30.0, true)));

    StepVerifier.create(service.getSimilarProducts("1"))
        .expectErrorMatches(ex -> ex.getMessage().equals("missing"))
        .verify();
  }

  @Test
  void failsWithNotFoundWhenASimilarProductNoLongerExists() {
    when(productClient.getSimilarProductsIds("1")).thenReturn(Mono.just(List.of("2", "3")));
    when(productClient.getProductDetailById("2"))
        .thenReturn(Mono.just(new ProductDetailDto("2", "Product 2", 10.0, true)));
    when(productClient.getProductDetailById("3"))
        .thenReturn(Mono.error(new ProductNotFoundException("3")));

    StepVerifier.create(service.getSimilarProducts("1"))
        .expectError(ProductNotFoundException.class)
        .verify();
  }

  @Test
  void propagatesNotFoundWhenBaseProductDoesNotExist() {
    when(productClient.getSimilarProductsIds("missing"))
        .thenReturn(Mono.error(new ProductNotFoundException("missing")));

    StepVerifier.create(service.getSimilarProducts("missing"))
        .expectError(ProductNotFoundException.class)
        .verify();
  }

  @Test
  void returnsEmptyListWhenThereAreNoSimilarProducts() {
    when(productClient.getSimilarProductsIds("1")).thenReturn(Mono.just(List.of()));

    StepVerifier.create(service.getSimilarProducts("1"))
        .assertNext(list -> org.assertj.core.api.Assertions.assertThat(list).isEmpty())
        .verifyComplete();
  }
}
