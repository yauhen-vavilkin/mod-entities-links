package org.folio.entlinks.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.entlinks.integration.kafka.AuthorityChangeFilterStrategy;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * Creates and configures {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} as
   * Spring bean for consuming authority events from Apache Kafka.
   *
   * @return {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} object as Spring bean.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, AuthorityDomainEvent> authorityListenerFactory(
    ConsumerFactory<String, AuthorityDomainEvent> consumerFactory) {
    var factory = listenerFactory(consumerFactory);
    factory.setRecordFilterStrategy(new AuthorityChangeFilterStrategy());
    return factory;
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ConsumerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link AuthorityDomainEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ConsumerFactory} object as Spring bean.
   */
  @Bean
  public ConsumerFactory<String, AuthorityDomainEvent> authorityConsumerFactory(KafkaProperties kafkaProperties) {
    return consumerFactoryForEvent(kafkaProperties, AuthorityDomainEvent.class);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} as
   * Spring bean for consuming link update report events from Apache Kafka.
   *
   * @return {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} object as Spring bean.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, LinkUpdateReport> statsListenerFactory(
    ConsumerFactory<String, LinkUpdateReport> consumerFactory) {
    return listenerFactory(consumerFactory);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ConsumerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinkUpdateReport}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ConsumerFactory} object as Spring bean.
   */
  @Bean
  public ConsumerFactory<String, LinkUpdateReport> linkUpdateReportConsumerFactory(KafkaProperties kafkaProperties) {
    return consumerFactoryForEvent(kafkaProperties, LinkUpdateReport.class);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ProducerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinksChangeEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ProducerFactory} object as Spring bean.
   */
  @Bean
  public ProducerFactory<String, LinksChangeEvent> producerFactory(KafkaProperties kafkaProperties) {
    return getProducerConfigProps(kafkaProperties);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ProducerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinkUpdateReport}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ProducerFactory} object as Spring bean.
   */
  @Bean
  public ProducerFactory<String, LinkUpdateReport> linkUpdateProducerFactory(KafkaProperties kafkaProperties) {
    return getProducerConfigProps(kafkaProperties);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.KafkaTemplate} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinksChangeEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.KafkaTemplate} object as Spring bean.
   */
  @Bean
  public KafkaTemplate<String, LinksChangeEvent> linksChangeKafkaTemplate(
    ProducerFactory<String, LinksChangeEvent> factory) {
    return new KafkaTemplate<>(factory);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.KafkaTemplate} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinkUpdateReport}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.KafkaTemplate} object as Spring bean.
   */
  @Bean
  public KafkaTemplate<String, LinkUpdateReport> linksUpdateKafkaTemplate(
    ProducerFactory<String, LinkUpdateReport> linkUpdateProducerFactory) {
    return new KafkaTemplate<>(linkUpdateProducerFactory);
  }

  @Bean
  public EventProducer<LinksChangeEvent> linksChangeEventMessageProducerService(
    KafkaTemplate<String, LinksChangeEvent> template) {
    return new EventProducer<>(template, "links.instance-authority");
  }

  @Bean
  public EventProducer<LinkUpdateReport> linkUpdateReportMessageProducerService(
    KafkaTemplate<String, LinkUpdateReport> template) {
    return new EventProducer<>(template, "links.instance-authority-stats");
  }

  @Bean
  public ProducerFactory<String, DomainEvent<?>> domainProducerFactory(KafkaProperties kafkaProperties) {
    return getProducerConfigProps(kafkaProperties);
  }

  @Bean
  public KafkaTemplate<String, DomainEvent<?>> domainKafkaTemplate(
    ProducerFactory<String, DomainEvent<?>> domainProducerFactory) {
    return new KafkaTemplate<>(domainProducerFactory);
  }

  @Bean
  public EventProducer<DomainEvent<?>> authorityDomainMessageProducerService(
    KafkaTemplate<String, DomainEvent<?>> template) {
    return new EventProducer<>(template, "authorities.authority");
  }

  private <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(
    ConsumerFactory<String, T> consumerFactory) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, T>();
    factory.setBatchListener(true);
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(new CommonLoggingErrorHandler());
    return factory;
  }

  private <T> ConsumerFactory<String, T> consumerFactoryForEvent(KafkaProperties kafkaProperties, Class<T> eventClass) {
    var deserializer = new JsonDeserializer<>(eventClass, objectMapper, false);
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
    return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
  }

  private <T> ProducerFactory<String, T> getProducerConfigProps(KafkaProperties kafkaProperties) {
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(),
        new StringSerializer(), new JsonSerializer<>(objectMapper));
  }
}
