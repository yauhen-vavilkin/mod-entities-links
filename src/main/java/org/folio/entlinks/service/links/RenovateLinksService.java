package org.folio.entlinks.service.links;

import static java.util.Collections.emptyList;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum.DELETE;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum.UPDATE;
import static org.folio.entlinks.utils.FieldUtils.getSubfield0Value;
import static org.folio.entlinks.utils.LinkEventsUtils.constructEvent;
import static org.folio.entlinks.utils.LinkEventsUtils.groupLinksByAuthorityId;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.exception.MarcAuthorityNotFoundException;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.folio.entlinks.service.links.model.AuthorityRuleValidationResult;
import org.folio.entlinks.service.messaging.authority.model.FieldChangeHolder;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class RenovateLinksService {

  private final AuthoritySourceFilesService sourceFilesService;

  public List<LinksChangeEvent> renovateBibs(UUID instanceId,
                                             List<StrippedParsedRecord> authoritySources,
                                             AuthorityRuleValidationResult validationResult) {
    return ListUtils.union(
      renovateBibsForValidLinks(instanceId, validationResult.validLinks(), authoritySources),
      renovateBibsForInvalidLinks(validationResult.invalidLinks())
    );
  }

  public List<LinksChangeEvent> renovateBibsForInvalidLinks(List<InstanceAuthorityLink> links) {
    var eventId = UUID.randomUUID();
    var linksByAuthorityId = groupLinksByAuthorityId(links);

    return linksByAuthorityId.entrySet().stream().map(linksById ->
        constructEvent(eventId, linksById.getKey(), DELETE, linksById.getValue(), emptyList()))
      .toList();
  }

  public List<LinksChangeEvent> renovateBibsForValidLinks(UUID instanceId,
                                                          List<InstanceAuthorityLink> links,
                                                          List<StrippedParsedRecord> authoritySources) {
    var eventId = UUID.randomUUID();
    var events = new LinkedList<LinksChangeEvent>();
    var linksByAuthorityId = groupLinksByAuthorityId(links);

    for (var entry : linksByAuthorityId.entrySet()) {
      var authorityId = entry.getKey();
      var authorityLinks = entry.getValue();
      var authority = findAuthorityById(authorityId, instanceId, authoritySources);
      var fieldChangeHolders = findFieldChangeHolders(authority, authorityLinks);

      var fieldChanges = fieldChangeHolders.stream()
        .map(FieldChangeHolder::toFieldChange)
        .toList();

      events.add(constructEvent(eventId, authorityId, UPDATE, authorityLinks, fieldChanges));
    }

    return events;
  }

  private StrippedParsedRecord findAuthorityById(UUID authorityId, UUID instanceId,
                                                 List<StrippedParsedRecord> authoritySources) {
    return authoritySources.stream()
      .filter(parsedRecord -> authorityId.equals(parsedRecord.getExternalIdsHolder().getAuthorityId()))
      .findFirst()
      .orElseThrow(() -> {
        log.warn("Unable to renovate links for instanceId {}", instanceId);
        return new MarcAuthorityNotFoundException(authorityId);
      });
  }

  private List<FieldChangeHolder> findFieldChangeHolders(StrippedParsedRecord authority,
                                                         List<InstanceAuthorityLink> links) {
    var fieldChangeHolders = new LinkedList<FieldChangeHolder>();

    for (var link : links) {
      var linkingRule = link.getLinkingRule();
      var naturalId = link.getAuthorityData().getNaturalId();
      var changedTag = linkingRule.getAuthorityField();

      authority.getParsedRecord().getContent().getFields().stream()
        .flatMap(fields -> fields.entrySet().stream())
        .filter(fieldEntry -> changedTag.equals(fieldEntry.getKey()))
        .findFirst()
        .map(Map.Entry::getValue)
        .ifPresent(authorityField -> {
          var fieldChangeHolder = new FieldChangeHolder(authorityField, linkingRule);
          fieldChangeHolder.addExtraSubfieldChange(getSubfield0Change(naturalId));
          fieldChangeHolders.add(fieldChangeHolder);
        });
    }

    return fieldChangeHolders;
  }

  private SubfieldChange getSubfield0Change(String naturalId) {
    var sourceFiles = sourceFilesService.fetchAuthoritySources();
    var subfield0Value = getSubfield0Value(sourceFiles, naturalId);
    return new SubfieldChange().code("0").value(subfield0Value);
  }
}
