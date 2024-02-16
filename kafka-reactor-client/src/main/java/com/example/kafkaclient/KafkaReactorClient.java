package com.example.kafkaclient;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.observation.KafkaReceiverObservation;
import reactor.kafka.receiver.observation.KafkaRecordReceiverContext;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaReactorClient {

  private final ReceiverOptions<Integer, String> kafkaReactorReceiverOptions;

  private final ObservationRegistry observationRegistry;

  private final HelloReactiveRepo helloReactiveRepo;

  private final HelloSpan helloSpan;

  @EventListener(ApplicationStartedEvent.class)
  public Disposable consumeMessages() {

    ReceiverOptions<Integer, String> options = kafkaReactorReceiverOptions
        .addAssignListener(partitions -> log.debug("onPartitionsAssigned {}", partitions))
        .addRevokeListener(partitions -> log.debug("onPartitionsRevoked {}", partitions));
    return KafkaReceiver.create(options).receive()
        .flatMap(r -> {
          Observation receiverObservation =
              KafkaReceiverObservation.RECEIVER_OBSERVATION.start(null,
                  KafkaReceiverObservation.DefaultKafkaReceiverObservationConvention.INSTANCE,
                  () ->
                      new KafkaRecordReceiverContext(
                          r, "reactor.receiver", kafkaReactorReceiverOptions.bootstrapServers()),
                  observationRegistry);

          return Mono.defer(() ->
              Mono.just(r)
                  .map(record -> {
                    ReceiverOffset offset = record.receiverOffset();
                    Instant timestamp = Instant.ofEpochMilli(record.timestamp());
                    List<String> headers = Arrays.stream(record.headers().toArray())
                        .map(h -> h.key() + " -> " + new String(h.value()))
                        .toList();
                    log.info("Received message: topic-partition={} offset={} timestamp={} key={} headers={} value={}",
                        offset.topicPartition(),
                        offset.offset(),
                        timestamp,
                        record.key(),
                        headers,
                        record.value());
                    offset.acknowledge();
                    Hello hello = new Hello();
                    hello.setText(record.value());
                    return hello;
                  })
                  .flatMap(helloSpan::logHelloBefore)
                  .flatMap(helloReactiveRepo::save)
                  .flatMap(helloSpan::logHelloAfter)
                  .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, receiverObservation)));
        })
        .subscribe();
  }
}
