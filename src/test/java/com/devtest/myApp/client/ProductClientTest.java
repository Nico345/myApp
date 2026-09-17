package com.devtest.myApp.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.devtest.myApp.exception.ProductNotFoundException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ProductClientTest {

  private WireMockServer wireMockServer;
  private ProductClient productClient;

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();

    WebClient webClient =
        WebClient.builder().baseUrl("http://localhost:" + wireMockServer.port()).build();

    productClient = new ProductClient(webClient, CircuitBreakerRegistry.ofDefaults());
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void getSimilarProductsIdsReturnsListOnSuccess() {
    wireMockServer.stubFor(
        get(urlEqualTo("/product/1/similarids")).willReturn(okJson("[\"2\",\"3\"]")));

    StepVerifier.create(productClient.getSimilarProductsIds("1"))
        .assertNext(ids -> assertThat(ids).containsExactly("2", "3"))
        .verifyComplete();
  }

  @Test
  void getSimilarProductsIdsReturnsEmptyListWhenUpstreamReturnsEmptyArray() {
    wireMockServer.stubFor(get(urlEqualTo("/product/1/similarids")).willReturn(okJson("[]")));

    StepVerifier.create(productClient.getSimilarProductsIds("1"))
        .assertNext(ids -> assertThat(ids).isEmpty())
        .verifyComplete();
  }

  @Test
  void getSimilarProductsIdsMapsUpstream404ToProductNotFoundException() {
    wireMockServer.stubFor(
        get(urlEqualTo("/product/1/similarids")).willReturn(aResponse().withStatus(404)));

    StepVerifier.create(productClient.getSimilarProductsIds("1"))
        .expectErrorMatches(
            ex -> ex instanceof ProductNotFoundException && ex.getMessage().contains("1"))
        .verify();
  }

  @Test
  void getProductDetailByIdReturnsDetailOnSuccess() {
    wireMockServer.stubFor(
        get(urlEqualTo("/product/2"))
            .willReturn(
                okJson(
                    "{\"id\":\"2\",\"name\":\"Product 2\",\"price\":10.5,\"availability\":true}")));

    StepVerifier.create(productClient.getProductDetailById("2"))
        .assertNext(
            detail -> {
              assertThat(detail.id()).isEqualTo("2");
              assertThat(detail.name()).isEqualTo("Product 2");
              assertThat(detail.price()).isEqualTo(10.5);
              assertThat(detail.availability()).isTrue();
            })
        .verifyComplete();
  }

  @Test
  void getProductDetailByIdMapsUpstream404ToProductNotFoundException() {
    wireMockServer.stubFor(get(urlEqualTo("/product/99")).willReturn(aResponse().withStatus(404)));

    StepVerifier.create(productClient.getProductDetailById("99"))
        .expectErrorMatches(
            ex -> ex instanceof ProductNotFoundException && ex.getMessage().contains("99"))
        .verify();
  }

  @Test
  void getProductDetailByIdPropagatesNonNotFoundErrorsAsIs() {
    wireMockServer.stubFor(get(urlEqualTo("/product/5")).willReturn(aResponse().withStatus(500)));

    StepVerifier.create(productClient.getProductDetailById("5"))
        .expectErrorMatches(ex -> !(ex instanceof ProductNotFoundException))
        .verify();
  }

  @Test
  void stopsCallingUpstreamOnceCircuitOpens() {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .slidingWindowSize(4)
            .minimumNumberOfCalls(4)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .build();
    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

    WebClient webClient =
        WebClient.builder().baseUrl("http://localhost:" + wireMockServer.port()).build();
    ProductClient clientWithLowThreshold = new ProductClient(webClient, registry);

    wireMockServer.stubFor(get(urlEqualTo("/product/1")).willReturn(aResponse().withStatus(500)));

    for (int i = 0; i < 4; i++) {
      StepVerifier.create(clientWithLowThreshold.getProductDetailById("1")).expectError().verify();
    }

    int requestsBeforeAssert =
        wireMockServer
            .countRequestsMatching(getRequestedFor(urlEqualTo("/product/1")).build())
            .getCount();

    StepVerifier.create(clientWithLowThreshold.getProductDetailById("1"))
        .expectError(CallNotPermittedException.class)
        .verify();

    int requestsAfterAssert =
        wireMockServer
            .countRequestsMatching(getRequestedFor(urlEqualTo("/product/1")).build())
            .getCount();

    assertThat(requestsAfterAssert).isEqualTo(requestsBeforeAssert);
  }
}
