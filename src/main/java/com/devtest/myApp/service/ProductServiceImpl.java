package com.devtest.myApp.service;

import com.devtest.myApp.client.ProductClient;
import com.devtest.myApp.dto.ProductDetailDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductClient productClient;

  @Override
  public Mono<List<ProductDetailDto>> getSimilarProducts(String productId) {
    return productClient
        .getSimilarProductsIds(productId)
        .flatMapMany(Flux::fromIterable)
        .flatMapSequential(productClient::getProductDetailById)
        .collectList();
  }
}
