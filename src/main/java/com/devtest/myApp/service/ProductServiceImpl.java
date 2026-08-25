package com.devtest.myApp.service;

import com.devtest.myApp.client.ProductClient;
import com.devtest.myApp.dto.ProductDetailDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
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
        .flatMapSequential(productClient::getProductDetailById, concurrency)
        .collectList();
  }
}
