package com.example.kafkaclient;

import io.micrometer.observation.ObservationRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.kafka.receiver.ReceiverOptions;

@Configuration
class Config {

  private static final String BOOTSTRAP_SERVERS = "localhost:29092";
  private static final String TOPIC = "hello.in";

  @Bean
  ReceiverOptions<Integer, String> kafkaTemplateReceiverOptions(ObservationRegistry observationRegistry) {
    Map<String, Object> config = new HashMap<>(commonKafkaConfig());
    config.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-reactive-template-consumer");
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-reactive-template-client");

    ReceiverOptions<Integer, String> basicReceiverOptions = ReceiverOptions.create(config);
    return basicReceiverOptions.withObservation(observationRegistry).subscription(Collections.singletonList(TOPIC));
  }

  @Bean
  ReceiverOptions<Integer, String> kafkaReactorReceiverOptions(ObservationRegistry observationRegistry) {

    Map<String, Object> props = new HashMap<>(commonKafkaConfig());
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-reactor-consumer");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-reactor-client");

    ReceiverOptions<Integer, String> receiverOptions = ReceiverOptions.create(props);
    return receiverOptions.withObservation(observationRegistry).withObservation(observationRegistry)
        .subscription(Collections.singletonList(TOPIC));
  }

  private Map<String, Object> commonKafkaConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return config;
  }

  @Bean
  ReactiveKafkaConsumerTemplate<Integer, String> reactiveKafkaConsumerTemplate(
      ReceiverOptions<Integer, String> kafkaTemplateReceiverOptions) {
    return new ReactiveKafkaConsumerTemplate<>(kafkaTemplateReceiverOptions);
  }


  @Bean
  MongoClientSettingsBuilderCustomizer mongoMetricsSynchronousContextProvider(ObservationRegistry registry) {
    return (clientSettingsBuilder) -> clientSettingsBuilder.contextProvider(ContextProviderFactory.create(registry))
        .addCommandListener(new MongoObservationCommandListener(registry));
  }
}
