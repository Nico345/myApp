package com.devtest.myApp.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient externalApiWebClient(
      @Value("${external-api.base-url}") String baseUrl,
      @Value("${external-api.timeout-ms}") long timeoutMs) {
    HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMs));

    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
