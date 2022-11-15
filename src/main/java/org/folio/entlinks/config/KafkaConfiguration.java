package org.folio.entlinks.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.entlinks.integration.AuthorityChangeFilterStrategy;
import org.folio.qm.domain.dto.InventoryEvent;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Responsible for Kafka configuration.
 */
@Configuration
public class KafkaConfiguration {

  /**
   * Creates and configures {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} as
   * Spring bean for consuming authority events from Apache Kafka.
   *
   * @return {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} object as Spring bean.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> authorityListenerFactory(
    ConsumerFactory<String, InventoryEvent> consumerFactory) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, InventoryEvent>();
    factory.setBatchListener(true);
    factory.setConsumerFactory(consumerFactory);
    factory.setRecordFilterStrategy(new AuthorityChangeFilterStrategy());
    factory.setCommonErrorHandler(new CommonLoggingErrorHandler());
    return factory;
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ConsumerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link InventoryEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ConsumerFactory} object as Spring bean.
   */
  @Bean
  public ConsumerFactory<String, InventoryEvent> authorityConsumerFactory(KafkaProperties kafkaProperties) {
    var deserializer = new JsonDeserializer<>(InventoryEvent.class);
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
    return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ProducerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link Object}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ProducerFactory} object as Spring bean.
   */
  @Bean
  public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
    Map<String, Object> configProps = new HashMap<>(kafkaProperties.buildProducerProperties());
    configProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.KafkaTemplate} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link Object}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.KafkaTemplate} object as Spring bean.
   */
  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }
}
