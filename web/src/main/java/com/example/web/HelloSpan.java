package com.example.web;

import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Observed
public class HelloSpan {

  public void logHello(Hello hello) {
    log.info("Received hello {}", hello);
  }

  public Mono<Hello> logHelloReactive(Hello hello) {
    return Mono.just(hello)
        .doOnNext(it -> log.info("Received hello {}", it));
  }
}
