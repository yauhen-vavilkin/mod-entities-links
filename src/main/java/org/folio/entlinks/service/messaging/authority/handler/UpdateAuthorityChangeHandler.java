package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.singletonList;
import static org.folio.entlinks.utils.ObjectUtils.getDifference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.config.properties.InstanceAuthorityChangeProperties;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.InventoryEventType;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.exception.AuthorityBatchProcessingException;
import org.folio.entlinks.integration.dto.AuthoritySourceRecord;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.folio.entlinks.integration.internal.AuthoritySourceRecordService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.AuthorityMappingRulesProcessingService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.FieldChangeHolder;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class UpdateAuthorityChangeHandler extends AbstractAuthorityChangeHandler {

  private final AuthoritySourceFilesService sourceFilesService;
  private final AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private final AuthoritySourceRecordService sourceRecordService;
  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final InstanceAuthorityLinkingService linkingService;

  public UpdateAuthorityChangeHandler(InstanceAuthorityChangeProperties instanceAuthorityChangeProperties,
                                      AuthoritySourceFilesService sourceFilesService,
                                      AuthorityMappingRulesProcessingService mappingRulesProcessingService,
                                      AuthoritySourceRecordService sourceRecordService,
                                      InstanceAuthorityLinkingRulesService linkingRulesService,
                                      InstanceAuthorityLinkingService linkingService) {
    super(instanceAuthorityChangeProperties, linkingService);
    this.sourceFilesService = sourceFilesService;
    this.mappingRulesProcessingService = mappingRulesProcessingService;
    this.sourceRecordService = sourceRecordService;
    this.linkingRulesService = linkingRulesService;
    this.linkingService = linkingService;
  }

  @Override
  public List<LinksChangeEvent> handle(List<InventoryEvent> events) {
    if (events == null || events.isEmpty()) {
      return Collections.emptyList();
    }

    List<LinksChangeEvent> linksEvents = new ArrayList<>();
    for (InventoryEvent event : events) {
      try {
        linksEvents.addAll(handle0(event));
      } catch (AuthorityBatchProcessingException e) {
        log.warn("Skipping authority change processing.", e);
      }
    }

    return linksEvents;
  }

  @Override
  public LinksChangeEvent.TypeEnum getReplyEventType() {
    return LinksChangeEvent.TypeEnum.UPDATE;
  }

  @Override
  public InventoryEventType supportedInventoryEventType() {
    return InventoryEventType.UPDATE;
  }

  private List<FieldChange> getFieldChangesForNaturalId(SubfieldChange subfield0Change,
                                                        List<InstanceAuthorityLink> instanceLinks) {
    return instanceLinks.stream()
      .map(InstanceAuthorityLink::getBibRecordTag)
      .distinct()
      .map(tag -> new FieldChange().field(tag).subfields(singletonList(subfield0Change)))
      .toList();
  }

  private List<LinksChangeEvent> handle0(InventoryEvent event) throws AuthorityBatchProcessingException {
    var changeHolder = toAuthorityChangeHolder(event);
    if (changeHolder.isOnlyNaturalIdChanged()) {
      return handleNaturalIdChange(changeHolder);
    } else {
      return handleFieldChange(changeHolder);
    }
  }

  private AuthorityChangeHolder toAuthorityChangeHolder(InventoryEvent event) throws AuthorityBatchProcessingException {
    var difference = getAuthorityChanges(event.getNew(), event.getOld());
    if (difference.size() > 2 || difference.size() == 2 && difference.contains(AuthorityChange.NATURAL_ID)) {
      throw new AuthorityBatchProcessingException(event.getId(),
        "Unsupported authority change [authorityId: " + event.getId() + ", changed fields: " + difference + "]");
    }
    return new AuthorityChangeHolder(event, difference);
  }

  private List<LinksChangeEvent> handleNaturalIdChange(AuthorityChangeHolder changeHolder) {
    var authorityId = changeHolder.getAuthorityId();
    var naturalId = changeHolder.getNewNaturalId();
    linkingService.updateNaturalId(naturalId, authorityId);

    var subfield0Change = getSubfield0Value(naturalId, changeHolder.getNewSourceFileId());

    return handleLinksByPartitions(authorityId,
      instanceLinks -> {
        var fieldChanges = getFieldChangesForNaturalId(subfield0Change, instanceLinks);
        return constructEvent(UUID.randomUUID(), authorityId, instanceLinks, fieldChanges);
      }
    );

  }

  private List<LinksChangeEvent> handleFieldChange(AuthorityChangeHolder changeHolder)
    throws AuthorityBatchProcessingException {
    var authorityId = changeHolder.getAuthorityId();

    var sourceRecord = sourceRecordService.getAuthoritySourceRecordById(authorityId);
    var changedTag = mappingRulesProcessingService.getTagByAuthorityChange(changeHolder.getFieldChange());
    var linkingRules = linkingRulesService.getLinkingRulesByAuthorityField(changedTag);

    var fieldChangeHolders = getFieldChangeHolders(authorityId, sourceRecord, changedTag, linkingRules);
    getSubfield0Change(changeHolder)
      .ifPresent(subfield0Change -> fieldChangeHolders
        .forEach(fieldChangeHolder -> fieldChangeHolder.addExtraSubfieldChange(subfield0Change)));

    updateLinksAccordingToChange(changeHolder, authorityId, fieldChangeHolders);

    var fieldChanges = fieldChangeHolders.stream()
      .map(FieldChangeHolder::toFieldChange)
      .toList();

    return handleLinksByPartitions(authorityId,
      instanceLinks -> constructEvent(UUID.randomUUID(), authorityId, instanceLinks, fieldChanges)
    );
  }

  private void updateLinksAccordingToChange(AuthorityChangeHolder changeHolder, UUID authorityId,
                                            List<FieldChangeHolder> fieldChangeHolders) {
    fieldChangeHolders.forEach(fieldChangeHolder -> {
      var subfieldCodes = fieldChangeHolder.getBibSubfieldCodes();
      var naturalId = changeHolder.getNewNaturalId();
      linkingService.updateSubfieldsAndNaturalId(subfieldCodes, naturalId, authorityId,
        fieldChangeHolder.getBibField());
    });
  }

  private List<FieldChangeHolder> getFieldChangeHolders(UUID authorityId,
                                                        AuthoritySourceRecord authoritySourceRecord,
                                                        String changedTag,
                                                        List<InstanceAuthorityLinkingRule> linkingRuleForField)
    throws AuthorityBatchProcessingException {
    var dataField = authoritySourceRecord.content().getDataFields().stream()
      .filter(field -> field.getTag().equals(changedTag))
      .findFirst()
      .orElseThrow(() -> new AuthorityBatchProcessingException(authorityId,
        "Source record don't contains [authorityId: " + authorityId + ", tag: " + changedTag + "]"));

    return linkingRuleForField.stream()
      .map(linkingRule -> new FieldChangeHolder(dataField, linkingRule))
      .toList();
  }

  private Optional<SubfieldChange> getSubfield0Change(AuthorityChangeHolder changeHolder) {
    if (changeHolder.isNaturalIdChanged()) {
      return Optional.of(getSubfield0Value(changeHolder.getNewNaturalId(), changeHolder.getNewSourceFileId()));
    }
    return Optional.empty();
  }

  private SubfieldChange getSubfield0Value(String naturalId, UUID sourceFileId) {
    String subfield0Value = "";
    if (sourceFileId != null) {
      var baseUrl = sourceFilesService.fetchAuthoritySourceUrls().get(sourceFileId);
      subfield0Value = StringUtils.appendIfMissing(baseUrl, "/");
    }
    return new SubfieldChange().code("0").value(subfield0Value + naturalId);
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

}
