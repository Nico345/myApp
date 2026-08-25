package com.devtest.myApp.service;

import com.devtest.myApp.dto.ProductDetailDto;
import java.util.List;
import reactor.core.publisher.Mono;

public interface ProductService {
  Mono<List<ProductDetailDto>> getSimilarProducts(String productId);
}
