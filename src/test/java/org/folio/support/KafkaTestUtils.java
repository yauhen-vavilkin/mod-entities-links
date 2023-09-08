package org.folio.support;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.folio.spring.config.properties.FolioEnvironment.getFolioEnvName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import lombok.experimental.UtilityClass;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@UtilityClass
public class KafkaTestUtils {

  public static String fullTopicName(String topicName, String tenantId) {
    return String.format("%s.%s.%s", getFolioEnvName(), tenantId, topicName);
  }

  public static List<ConsumerRecord<String, LinkUpdateReport>> consumerRecords(List<LinkUpdateReport> reports) {
    return reports.stream()
      .map(report -> new ConsumerRecord<>(EMPTY, 0, 0, EMPTY, report))
      .toList();
  }

  public static <T> KafkaMessageListenerContainer<String, T> createAndStartTestConsumer(
    String topicName,
    BlockingQueue<ConsumerRecord<String, T>> queue,
    KafkaProperties properties,
    Class<T> eventClass) {
    var deserializer = new JsonDeserializer<>(eventClass);
    properties.getConsumer().setGroupId("test-group");
    Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties());
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);

    var consumer = new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);

    var containerProperties = new ContainerProperties(topicName);
    var container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
    container.setupMessageListener((MessageListener<String, T>) queue::add);
    container.start();
    return container;
  }
}
