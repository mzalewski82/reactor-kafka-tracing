package com.example.kafkaclient;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.observation.KafkaReceiverObservation;
import reactor.kafka.receiver.observation.KafkaRecordReceiverContext;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTemplateClient {

  private final ReactiveKafkaConsumerTemplate<Integer, String> reactiveKafkaConsumerTemplate;

  private final HelloReactiveRepo helloReactiveRepo;

  private final HelloSpan helloSpan;

  private final ObservationRegistry observationRegistry;

  @EventListener(ApplicationStartedEvent.class)
  public Flux<Hello> startKafkaConsumer() {
    return reactiveKafkaConsumerTemplate
        .receiveAutoAck()
        .flatMap(cr -> {
          Observation receiverObservation =
              KafkaReceiverObservation.RECEIVER_OBSERVATION.start(null,
                  KafkaReceiverObservation.DefaultKafkaReceiverObservationConvention.INSTANCE,
                  () ->
                      new KafkaRecordReceiverContext(
                          cr, "template.receiver", null),
                  observationRegistry);

          log.info("Observation {}", receiverObservation);

          return Mono.defer(() -> Mono.just(cr))
              .doOnNext(consumerRecord -> log.info("received key={}, value={} from topic={}, offset={}",
                  consumerRecord.key(),
                  consumerRecord.value(),
                  consumerRecord.topic(),
                  consumerRecord.offset())
              )
              .map(ConsumerRecord::value)
              .map(msg -> {
                    Hello hello = new Hello();
                    hello.setText(msg);
                    return hello;
                  }
              )
              .flatMap(helloSpan::logHelloBefore)
              .flatMap(helloReactiveRepo::save)
              .flatMap(helloSpan::logHelloAfter)
              .doOnNext(hello -> log.info("successfully consumed {}={}", String.class.getSimpleName(), hello))
              .doOnError(throwable -> log.error("something bad happened while consuming : {}", throwable.getMessage()))
              .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, receiverObservation));
        });
  }
}
