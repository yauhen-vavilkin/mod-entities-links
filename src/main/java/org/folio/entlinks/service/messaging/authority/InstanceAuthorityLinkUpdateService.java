package org.folio.entlinks.service.messaging.authority;

import static org.folio.entlinks.utils.ObjectUtils.getDifference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.service.messaging.authority.handler.AuthorityChangeHandler;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.entlinks.utils.KafkaUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class InstanceAuthorityLinkUpdateService {

  private static final String REPLY_TOPIC_NAME = "links.instance-authority";

  private final FolioExecutionContext context;
  private final KafkaTemplate<String, LinksChangeEvent> kafkaTemplate;
  private final Map<AuthorityChangeType, AuthorityChangeHandler> changeHandlers;

  public InstanceAuthorityLinkUpdateService(FolioExecutionContext context,
                                            KafkaTemplate<String, LinksChangeEvent> kafkaTemplate,
                                            List<AuthorityChangeHandler> changeHandlers) {
    this.context = context;
    this.kafkaTemplate = kafkaTemplate;
    this.changeHandlers = changeHandlers.stream()
      .collect(Collectors.toMap(AuthorityChangeHandler::supportedAuthorityChangeType, handler -> handler));
  }

  public void handleAuthoritiesChanges(List<InventoryEvent> events) {
    var changeHolders = events.stream()
      .map(this::toAuthorityChangeHolder)
      .toList();

    var changesByType = changeHolders.stream()
      .collect(Collectors.groupingBy(AuthorityChangeHolder::getChangeType));

    for (var eventsByTypeEntry : changesByType.entrySet()) {
      var type = eventsByTypeEntry.getKey();
      var handler = changeHandlers.get(type);
      if (handler == null) {
        log.warn("No suitable handler found [event type: {}]", type);
        return;
      } else {
        var linksEvents = handler.handle(eventsByTypeEntry.getValue());
        sendEvents(linksEvents, type);
      }
    }
  }

  private AuthorityChangeHolder toAuthorityChangeHolder(InventoryEvent event) {
    if (event.getNew() == null) {
      return new AuthorityChangeHolder(event, Collections.emptyList());
    }
    var difference = getAuthorityChanges(event.getNew(), event.getOld());
    return new AuthorityChangeHolder(event, difference);
  }

  @SneakyThrows
  private List<AuthorityChange> getAuthorityChanges(AuthorityInventoryRecord s1, AuthorityInventoryRecord s2) {
    return getDifference(s1, s2).stream()
      .map(fieldName -> {
        try {
          return AuthorityChange.fromValue(fieldName);
        } catch (IllegalArgumentException e) {
          log.debug("Not supported authority change [fieldName: {}]", fieldName);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();
  }

  private void sendEvents(List<LinksChangeEvent> events, AuthorityChangeType type) {
    log.info("Sending {} {} events to Kafka", events.size(), type);
    events.stream()
      .map(this::toProducerRecord)
      .forEach(kafkaTemplate::send);
  }

  private ProducerRecord<String, LinksChangeEvent> toProducerRecord(LinksChangeEvent linksEvent) {
    linksEvent.tenant(context.getTenantId());
    var producerRecord = new ProducerRecord<String, LinksChangeEvent>(topicName(), linksEvent);
    KafkaUtils.toKafkaHeaders(context.getOkapiHeaders())
      .forEach(header -> producerRecord.headers().add(header));
    return producerRecord;
  }

  private String topicName() {
    return KafkaUtils.getTenantTopicName(REPLY_TOPIC_NAME, context.getTenantId());
  }

}
