package org.folio.entlinks.service.messaging.authority;

import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType.DELETE;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType.UPDATE;
import static org.folio.entlinks.utils.ObjectUtils.getDifference;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.entlinks.integration.internal.AuthoritySourceRecordService;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.handler.AuthorityChangeHandler;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class InstanceAuthorityLinkUpdateService {

  private final AuthorityDataStatService authorityDataStatService;
  private final Map<AuthorityChangeType, AuthorityChangeHandler> changeHandlers;
  private final AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private final InstanceAuthorityLinkingService linkingService;
  private final EventProducer<LinksChangeEvent> eventProducer;
  private final AuthoritySourceRecordService sourceRecordService;
  private final ConsortiumTenantsService consortiumTenantsService;
  private final FolioExecutionContext folioExecutionContext;
  private final SystemUserScopedExecutionService executionService;

  public InstanceAuthorityLinkUpdateService(AuthorityDataStatService authorityDataStatService,
                                            AuthorityMappingRulesProcessingService mappingRulesProcessingService,
                                            InstanceAuthorityLinkingService linkingService,
                                            EventProducer<LinksChangeEvent> eventProducer,
                                            List<AuthorityChangeHandler> changeHandlers,
                                            AuthoritySourceRecordService sourceRecordService,
                                            ConsortiumTenantsService consortiumTenantsService,
                                            FolioExecutionContext folioExecutionContext,
                                            SystemUserScopedExecutionService executionService) {
    this.authorityDataStatService = authorityDataStatService;
    this.mappingRulesProcessingService = mappingRulesProcessingService;
    this.linkingService = linkingService;
    this.eventProducer = eventProducer;
    this.changeHandlers = changeHandlers.stream()
      .collect(Collectors.toMap(AuthorityChangeHandler::supportedAuthorityChangeType, handler -> handler));
    this.sourceRecordService = sourceRecordService;
    this.consortiumTenantsService = consortiumTenantsService;
    this.folioExecutionContext = folioExecutionContext;
    this.executionService = executionService;
  }

  public void handleAuthoritiesChanges(List<AuthorityDomainEvent> events) {
    var incomingAuthorityIds = events.stream()
      .map(AuthorityDomainEvent::getId)
      .collect(Collectors.toSet());
    var linksNumberByAuthorityId = linkingService.countLinksByAuthorityIds(incomingAuthorityIds);

    var fieldTagRelation = mappingRulesProcessingService.getFieldTagRelations();
    var changeHolders = events.stream()
      .map(event -> toAuthorityChangeHolder(event, fieldTagRelation, linksNumberByAuthorityId))
      .filter(AuthorityChangeHolder::changesExist)
      .toList();
    fillChangeHoldersWithSourceRecord(changeHolders);

    prepareAndSaveAuthorityDataStats(changeHolders);
    processEventsByChangeType(changeHolders);
    processChangesForShadowAuthorities(incomingAuthorityIds, changeHolders);
  }

  private void fillChangeHoldersWithSourceRecord(List<AuthorityChangeHolder> changeHolders) {
    var changeHoldersForSourceRecord = changeHolders.stream()
        .filter(holder -> holder.getChangeType().equals(UPDATE) && !holder.isOnlyNaturalIdChanged())
        .collect(Collectors.groupingBy(AuthorityChangeHolder::getAuthorityId));
    if (!changeHoldersForSourceRecord.isEmpty()) {
      changeHoldersForSourceRecord.forEach((authorityId, authorityChangeHolders) -> {
        var sourceRecord = sourceRecordService.getAuthoritySourceRecordById(authorityId);
        authorityChangeHolders.forEach(changeHolder -> changeHolder.setSourceRecord(sourceRecord));
      });
    }
  }

  private void processEventsByChangeType(List<AuthorityChangeHolder> changeHolders) {
    var changesByType = changeHolders.stream()
      .filter(this::isProcessableChange)
      .collect(Collectors.groupingBy(AuthorityChangeHolder::getChangeType));

    for (var eventsByTypeEntry : changesByType.entrySet()) {
      var type = eventsByTypeEntry.getKey();
      var handler = changeHandlers.get(type);
      if (handler == null) {
        log.warn("No suitable handler found [tenantId: {}, event type: {}]",
            folioExecutionContext.getTenantId(), type);
        return;
      } else {
        var linksEvents = handler.handle(eventsByTypeEntry.getValue());
        sendEvents(linksEvents, type);
      }
    }
  }

  private boolean isProcessableChange(AuthorityChangeHolder changeHolder) {
    if (changeHolder.getChangeType() == DELETE) {
      return true;
    }

    if (changeHolder.getChangeType() == UPDATE && changeHolder.getNumberOfLinks() > 0) {
      return true;
    }

    log.info("Skip message for {} event. Authority record [tenantId: {}, id: {}] doesn't have links",
        changeHolder.getChangeType(), folioExecutionContext.getTenantId(), changeHolder.getAuthorityId());
    return false;

  }

  private void prepareAndSaveAuthorityDataStats(List<AuthorityChangeHolder> changeHolders) {
    var authorityDataStats = changeHolders.stream()
      .map(AuthorityChangeHolder::toAuthorityDataStat)
      .toList();

    var dataStats = authorityDataStatService.createInBatch(authorityDataStats);
    for (AuthorityChangeHolder changeHolder : changeHolders) {
      for (AuthorityDataStat authorityDataStat : dataStats) {
        if (Objects.equals(authorityDataStat.getAuthority().getId(), changeHolder.getAuthorityId())) {
          changeHolder.setAuthorityDataStatId(authorityDataStat.getId());
          break;
        }
      }
    }
  }

  private void processChangesForShadowAuthorities(Set<UUID> authorityIds, List<AuthorityChangeHolder> changeHolders) {
    var consortiumTenants = consortiumTenantsService.getConsortiumTenants(folioExecutionContext.getTenantId());
    if (consortiumTenants.isEmpty()) {
      return;
    }

    log.debug("Processing authority changes for shadow copies of authorities: [{}]", authorityIds);
    consortiumTenants.forEach(memberTenant -> {
      var changeHolderCopies = changeHolders.stream().map(AuthorityChangeHolder::copy).toList();
      executionService.executeSystemUserScoped(memberTenant, () -> {
        var linksNumberByAuthorityId = linkingService.countLinksByAuthorityIds(authorityIds);
        changeHolderCopies.forEach(changeHolder ->
            changeHolder.setNumberOfLinks(linksNumberByAuthorityId.getOrDefault(changeHolder.getAuthorityId(), 0)));
        prepareAndSaveAuthorityDataStats(changeHolderCopies);
        processEventsByChangeType(changeHolderCopies);
        return null;
      });
    });
    log.debug("Finished processing authority changes for shadow copies of authorities: [{}]", authorityIds);
  }

  private AuthorityChangeHolder toAuthorityChangeHolder(AuthorityDomainEvent event,
                                                        Map<AuthorityChangeField, String> fieldTagRelation,
                                                        Map<UUID, Integer> linksNumberByAuthorityId) {
    var difference = getAuthorityChanges(event.getNewEntity(), event.getOldEntity());
    return new AuthorityChangeHolder(event, difference, fieldTagRelation,
      linksNumberByAuthorityId.getOrDefault(event.getId(), 0));
  }

  @SneakyThrows
  private Map<AuthorityChangeField, AuthorityChange> getAuthorityChanges(AuthorityDto s1, AuthorityDto s2) {
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
    log.info("Sending {} {} events to Kafka for tenant {}", events.size(), type,
        folioExecutionContext.getTenantId());
    eventProducer.sendMessages(events);
  }

}
