package com.devtest.myApp.client;

import com.devtest.myApp.dto.ProductDetailDto;
import com.devtest.myApp.exception.ProductNotFoundException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductClient {
  private final WebClient webClient;

  public Mono<ProductDetailDto> getProductDetailById(String id) {
    return webClient
        .get()
        .uri("/product/{productId}", id)
        .retrieve()
        .bodyToMono(ProductDetailDto.class)
        .doOnError(ex -> log.debug("Could not fetch detail for product {}: {}", id, ex.toString()));
  }

  public Mono<List<String>> getSimilarProductsIds(String id) {
    return webClient
        .get()
        .uri("/product/{productId}/similarids", id)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
        .defaultIfEmpty(Collections.emptyList())
        .onErrorMap(this::isNotFound, ex -> new ProductNotFoundException(id));
  }

  private boolean isNotFound(Throwable ex) {
    return ex instanceof WebClientResponseException wcre
        && wcre.getStatusCode().equals(HttpStatusCode.valueOf(404));
  }
}
