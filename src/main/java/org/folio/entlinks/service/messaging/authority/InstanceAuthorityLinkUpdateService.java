package org.folio.entlinks.service.messaging.authority;

import static org.folio.entlinks.utils.ObjectUtils.getDifference;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.handler.AuthorityChangeHandler;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class InstanceAuthorityLinkUpdateService {

  private final AuthorityDataStatService authorityDataStatService;
  private final Map<AuthorityChangeType, AuthorityChangeHandler> changeHandlers;
  private final AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private final InstanceAuthorityLinkingService linkingService;
  private final EventProducer<LinksChangeEvent> eventProducer;

  public InstanceAuthorityLinkUpdateService(AuthorityDataStatService authorityDataStatService,
                                            AuthorityMappingRulesProcessingService mappingRulesProcessingService,
                                            InstanceAuthorityLinkingService linkingService,
                                            EventProducer<LinksChangeEvent> eventProducer,
                                            List<AuthorityChangeHandler> changeHandlers) {
    this.authorityDataStatService = authorityDataStatService;
    this.mappingRulesProcessingService = mappingRulesProcessingService;
    this.linkingService = linkingService;
    this.eventProducer = eventProducer;
    this.changeHandlers = changeHandlers.stream()
      .collect(Collectors.toMap(AuthorityChangeHandler::supportedAuthorityChangeType, handler -> handler));
  }

  public void handleAuthoritiesChanges(List<InventoryEvent> events) {
    var incomingAuthorityIds = events.stream()
      .map(InventoryEvent::getId)
      .collect(Collectors.toSet());
    var linksNumberByAuthorityId = linkingService.countLinksByAuthorityIds(incomingAuthorityIds);

    var fieldTagRelation = mappingRulesProcessingService.getFieldTagRelations();
    var changeHolders = events.stream()
      .map(event -> toAuthorityChangeHolder(event, fieldTagRelation, linksNumberByAuthorityId))
      .toList();

    prepareAndSaveAuthorityDataStats(changeHolders);

    processEventsByChangeType(changeHolders);
  }

  private void processEventsByChangeType(List<AuthorityChangeHolder> changeHolders) {
    var changesByType = changeHolders.stream()
      .filter(changeHolder -> {
        if (changeHolder.getNumberOfLinks() > 0) {
          return true;
        } else {
          log.info("Skip message. Authority record [id: {}] doesn't have links", changeHolder.getAuthorityId());
          return false;
        }
      })
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

  private void prepareAndSaveAuthorityDataStats(List<AuthorityChangeHolder> changeHolders) {
    var authorityDataStats = changeHolders.stream()
      .map(AuthorityChangeHolder::toAuthorityDataStat)
      .toList();

    var dataStats = authorityDataStatService.createInBatch(authorityDataStats);
    for (AuthorityChangeHolder changeHolder : changeHolders) {
      for (AuthorityDataStat authorityDataStat : dataStats) {
        if (authorityDataStat.getAuthorityData().getId() == changeHolder.getAuthorityId()) {
          changeHolder.setAuthorityDataStatId(authorityDataStat.getId());
          break;
        }
      }
    }
  }

  private AuthorityChangeHolder toAuthorityChangeHolder(InventoryEvent event,
                                                        Map<AuthorityChangeField, String> fieldTagRelation,
                                                        Map<UUID, Integer> linksNumberByAuthorityId) {
    var difference = getAuthorityChanges(event.getNew(), event.getOld());
    return new AuthorityChangeHolder(event, difference, fieldTagRelation,
      linksNumberByAuthorityId.getOrDefault(event.getId(), 0));
  }

  @SneakyThrows
  private Map<AuthorityChangeField, AuthorityChange> getAuthorityChanges(AuthorityInventoryRecord s1,
                                                                         AuthorityInventoryRecord s2) {
    return getDifference(s1, s2).stream()
      .map(difference -> {
        try {
          var authorityChangeField = AuthorityChangeField.fromValue(difference.fieldName());
          return new AuthorityChange(authorityChangeField, difference.val1(), difference.val2());
        } catch (IllegalArgumentException e) {
          log.debug("Not supported authority change [fieldName: {}]", difference);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(AuthorityChange::changeField, ac -> ac));
  }

  private void sendEvents(List<LinksChangeEvent> events, AuthorityChangeType type) {
    log.info("Sending {} {} events to Kafka", events.size(), type);
    eventProducer.sendMessages(events);
  }

}
