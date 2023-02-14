package org.folio.entlinks.integration.kafka;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.entlinks.integration.dto.BaseEvent;
import org.folio.entlinks.utils.DateUtils;
import org.folio.entlinks.utils.KafkaUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

@Log4j2
@RequiredArgsConstructor
public class EventProducer<T extends BaseEvent> {

  private final KafkaTemplate<String, T> template;
  private final String topicName;

  @Autowired
  private FolioExecutionContext context;

  public void sendMessages(List<T> msgBodies) {
    log.info("Sending events to Kafka [topic: {}, number: {}]", topicName, msgBodies.size());
    log.trace("Sending events to Kafka [topic: {}, bodies: {}]", topicName, msgBodies);
    msgBodies.stream()
      .map(this::toProducerRecord)
      .forEach(template::send);
  }

  private ProducerRecord<String, T> toProducerRecord(T linksEvent) {
    linksEvent.setTenant(context.getTenantId());
    linksEvent.setTs(DateUtils.currentTsInString());

    var producerRecord = new ProducerRecord<String, T>(topicName(), linksEvent);

    KafkaUtils.toKafkaHeaders(context.getOkapiHeaders())
      .forEach(header -> producerRecord.headers().add(header));
    return producerRecord;
  }

  private String topicName() {
    return KafkaUtils.getTenantTopicName(topicName, context.getTenantId());
  }
}
