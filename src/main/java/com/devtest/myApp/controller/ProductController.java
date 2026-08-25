package com.devtest.myApp.controller;

import com.devtest.myApp.dto.ProductDetailDto;
import com.devtest.myApp.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
public class ProductController {

  private final ProductService productService;

  @GetMapping("/{id}/similar")
  public Mono<ResponseEntity<List<ProductDetailDto>>> getSimilarProducts(@PathVariable String id) {
    return productService.getSimilarProducts(id).map(ResponseEntity::ok);
  }
}
