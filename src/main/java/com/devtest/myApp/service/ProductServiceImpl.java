package com.devtest.myApp.service;

import com.devtest.myApp.client.ProductClient;
import com.devtest.myApp.dto.ProductDetailDto;
import com.devtest.myApp.exception.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductClient productClient;

  @Value("${external-api.concurrency:5}")
  private int concurrency;

  @Override
  public Mono<List<ProductDetailDto>> getSimilarProducts(String productId) {
    return productClient
        .getSimilarProductsIds(productId)
        .flatMapMany(Flux::fromIterable)
        .distinct()
        .flatMapSequential(
            similarId ->
                productClient
                    .getProductDetailById(similarId)
                    .onErrorResume(
                        ex -> {
                          if (ex instanceof CallNotPermittedException) {
                            return Mono.error(ex);
                          } else if (ex instanceof ProductNotFoundException) {
                            log.debug(
                                "Skipping similar product {} for product {}: not found",
                                similarId,
                                productId);
                          } else {
                            log.warn(
                                "Skipping similar product {} for product {}: {}",
                                similarId,
                                productId,
                                ex.toString());
                          }
                          return Mono.empty();
                        }),
            concurrency)
        .collectList();
  }
}
