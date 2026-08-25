package com.devtest.myApp.exception;

public class ProductNotFoundException extends RuntimeException {

  public ProductNotFoundException(String productId) {
    super("Product not found: " + productId);
  }
}
