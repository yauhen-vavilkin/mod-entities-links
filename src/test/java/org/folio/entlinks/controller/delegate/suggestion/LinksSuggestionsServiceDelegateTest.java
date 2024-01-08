package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.dto.ExternalIdType;
import org.folio.entlinks.domain.dto.FetchConditions;
import org.folio.entlinks.domain.dto.FetchParsedRecordsBatchRequest;
import org.folio.entlinks.domain.dto.FieldContent;
import org.folio.entlinks.domain.dto.FieldRange;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.RecordType;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.service.consortium.ConsortiumTenantExecutor;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksSuggestionsServiceDelegateTest {

  private static final UUID AUTHORITY_ID = UUID.randomUUID();
  private static final String MIN_AUTHORITY_FIELD = "100";
  private static final String MAX_AUTHORITY_FIELD = "155";
  private static final String NATURAL_ID = "e12345";
  private static final String AUTHORITY_SOURCE_MARC = "marc";
  private static final String BASE_URL = "https://base/url/";

  private @Spy SourceContentMapper contentMapper = Mappers.getMapper(SourceContentMapper.class);
  private @Mock InstanceAuthorityLinkingRulesService linkingRulesService;
  private @Mock LinksSuggestionService suggestionService;
  private @Mock AuthorityRepository authorityRepository;
  private @Mock SourceStorageClient sourceStorageClient;
  private @Mock ConsortiumTenantExecutor executor;
  private @InjectMocks LinksSuggestionsByAuthorityNaturalId serviceDelegate;

  @Test
  void suggestLinksForMarcRecords_shouldSaveAuthoritiesFromSearch() {
    var authority1 = Authority.builder()
        .id(AUTHORITY_ID).naturalId(NATURAL_ID).source(AUTHORITY_SOURCE_MARC).build();
    var authority2 = Authority.builder()
        .id(UUID.randomUUID()).naturalId(NATURAL_ID).source(AUTHORITY_SOURCE_MARC).build();
    authority2.makeAsConsortiumShadowCopy();
    var fetchRequest = getBatchFetchRequestForAuthority(AUTHORITY_ID);
    var records = List.of(getRecord("100", Map.of("0", NATURAL_ID)));
    var rules = List.of(getRule("100"));

    when(authorityRepository.findByNaturalIdInAndDeletedFalse(Set.of(NATURAL_ID)))
        .thenReturn(List.of(authority1, authority2));
    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(linkingRulesService.getMinAuthorityField()).thenReturn(MIN_AUTHORITY_FIELD);
    when(linkingRulesService.getMaxAuthorityField()).thenReturn(MAX_AUTHORITY_FIELD);
    when(sourceStorageClient
      .buildBatchFetchRequestForAuthority(Set.of(AUTHORITY_ID), MIN_AUTHORITY_FIELD, MAX_AUTHORITY_FIELD))
      .thenReturn(fetchRequest);
    var strippedParsedRecords = new StrippedParsedRecordCollection(emptyList(), 1);
    when(sourceStorageClient.fetchParsedRecordsInBatch(fetchRequest)).thenReturn(strippedParsedRecords);
    when(executor.executeAsCentralTenant(any())).thenReturn(strippedParsedRecords);
    var parsedContentCollection = new ParsedRecordContentCollection().records(records);

    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection, false);

    verify(sourceStorageClient).fetchParsedRecordsInBatch(fetchRequest);
    verify(executor).executeAsCentralTenant(any());
    verify(suggestionService)
        .fillLinkDetailsWithSuggestedAuthorities(any(),
            eq(List.of()), eq(Map.of("100", rules)), eq("0"), eq(false));
  }

  @Test
  void suggestLinksForMarcRecords_shouldRetrieveAuthoritiesFromTable() {
    var authority = Authority.builder()
        .id(AUTHORITY_ID).naturalId(NATURAL_ID).source(AUTHORITY_SOURCE_MARC).build();
    var authorities = List.of(authority);
    var fetchRequest = getBatchFetchRequestForAuthority(AUTHORITY_ID);
    var rules = List.of(getRule("100"));

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(linkingRulesService.getMinAuthorityField()).thenReturn(MIN_AUTHORITY_FIELD);
    when(linkingRulesService.getMaxAuthorityField()).thenReturn(MAX_AUTHORITY_FIELD);

    when(authorityRepository.findByNaturalIdInAndDeletedFalse(Set.of(NATURAL_ID))).thenReturn(authorities);
    when(sourceStorageClient
      .buildBatchFetchRequestForAuthority(Set.of(AUTHORITY_ID), MIN_AUTHORITY_FIELD, MAX_AUTHORITY_FIELD))
      .thenReturn(fetchRequest);
    when(sourceStorageClient.fetchParsedRecordsInBatch(fetchRequest)).thenReturn(
      new StrippedParsedRecordCollection(emptyList(), 1));
    var records = List.of(getRecord("100", Map.of("0", NATURAL_ID)));
    var parsedContentCollection = new ParsedRecordContentCollection().records(records);

    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection, false);

    verify(authorityRepository).findByNaturalIdInAndDeletedFalse(Set.of(NATURAL_ID));
    verify(sourceStorageClient).fetchParsedRecordsInBatch(fetchRequest);
    verify(suggestionService)
      .fillLinkDetailsWithSuggestedAuthorities(any(),
          eq(List.of()), eq(Map.of("100", rules)), eq("0"), eq(false));
    verifyNoInteractions(executor);
  }

  @Test
  void suggestLinksForMarcRecords_shouldExtractNaturalIdFrom0Subfield() {
    var authority = Authority.builder()
        .id(AUTHORITY_ID).naturalId(NATURAL_ID).source(AUTHORITY_SOURCE_MARC).build();
    var authorities = List.of(authority);
    var fetchRequest = getBatchFetchRequestForAuthority(AUTHORITY_ID);
    var rules = List.of(getRule("100"));

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(linkingRulesService.getMinAuthorityField()).thenReturn(MIN_AUTHORITY_FIELD);
    when(linkingRulesService.getMaxAuthorityField()).thenReturn(MAX_AUTHORITY_FIELD);

    when(authorityRepository.findByNaturalIdInAndDeletedFalse(Set.of(NATURAL_ID))).thenReturn(authorities);
    when(sourceStorageClient
      .buildBatchFetchRequestForAuthority(Set.of(AUTHORITY_ID), MIN_AUTHORITY_FIELD, MAX_AUTHORITY_FIELD))
      .thenReturn(fetchRequest);
    when(sourceStorageClient.fetchParsedRecordsInBatch(fetchRequest)).thenReturn(
      new StrippedParsedRecordCollection(emptyList(), 1));

    var records = List.of(getRecord("100", Map.of("0", BASE_URL + NATURAL_ID)));
    var parsedContentCollection = new ParsedRecordContentCollection().records(records);
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection, false);

    verify(authorityRepository).findByNaturalIdInAndDeletedFalse(Set.of(NATURAL_ID));
    verify(sourceStorageClient).fetchParsedRecordsInBatch(fetchRequest);
    verifyNoInteractions(executor);
  }

  @Test
  void suggestLinksForMarcRecords_shouldNotFetchAuthorities_ifNoNaturalIdsWasFound() {
    var record = new ParsedRecordContent(emptyList(), "record without naturalId");
    var rules = List.of(getRule("110"));

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(authorityRepository.findByNaturalIdInAndDeletedFalse(emptySet())).thenReturn(emptyList());

    var parsedContentCollection = new ParsedRecordContentCollection().records(List.of(record));
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection, false);

    verify(authorityRepository).findByNaturalIdInAndDeletedFalse(emptySet());
    verify(sourceStorageClient, times(0)).fetchParsedRecordsInBatch(any());
  }

  @Test
  void suggestLinksForMarcRecords_ifBibFieldTagNoConsistInLinkingRules() {
    var records = List.of(getRecord("100"));
    var rules = List.of(getRule("110"));

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);

    var parsedContentCollection = new ParsedRecordContentCollection().records(records);
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection, false);

    verify(authorityRepository).findByNaturalIdInAndDeletedFalse(emptySet());
    verify(sourceStorageClient, times(0)).fetchParsedRecordsInBatch(any());
  }

  @Test
  void suggestLinksForMarcRecords_shouldFillError_ifAutoLinkingDisabled() {
    var record = getRecord("100", Map.of("0", "test"));
    var rules = List.of(getRule("100", false));

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(authorityRepository.findByNaturalIdInAndDeletedFalse(emptySet())).thenReturn(emptyList());

    var parsedContentCollection = new ParsedRecordContentCollection().records(List.of(record));
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection, false);

    verify(authorityRepository).findByNaturalIdInAndDeletedFalse(emptySet());
    verifyNoInteractions(sourceStorageClient);
    verify(suggestionService).fillErrorDetailsWithDisabledAutoLinking(any(), any());
  }

  private ParsedRecordContent getRecord(String bibField) {
    return getRecord(bibField, Map.of("a", "test"));
  }

  private ParsedRecordContent getRecord(String bibField, Map<String, String> subfields) {
    var field = new FieldContent();
    field.setSubfields(List.of(subfields));
    field.setLinkDetails(new LinkDetails().authorityNaturalId(NATURAL_ID));

    var fields = Map.of(bibField, field);
    return new ParsedRecordContent(List.of(fields), "default leader");
  }

  private InstanceAuthorityLinkingRule getRule(String bibField) {
    return getRule(bibField, true);
  }

  private InstanceAuthorityLinkingRule getRule(String bibField, Boolean autoLinkingEnabled) {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);

    var rule = new InstanceAuthorityLinkingRule();
    rule.setId(1);
    rule.setBibField(bibField);
    rule.setAuthorityField("100");
    rule.setAutoLinkingEnabled(autoLinkingEnabled);
    rule.setAuthoritySubfields(new char[] {'a', 'b'});
    rule.setSubfieldModifications(List.of(modification));
    rule.setSubfieldsExistenceValidations(existence);

    return rule;
  }

  private FetchParsedRecordsBatchRequest getBatchFetchRequestForAuthority(UUID externalIds) {
    var fieldRange = List.of(new FieldRange(MIN_AUTHORITY_FIELD, MAX_AUTHORITY_FIELD));
    var fetchConditions = new FetchConditions()
      .idType(ExternalIdType.AUTHORITY)
      .ids(Set.of(externalIds));

    return new FetchParsedRecordsBatchRequest(fetchConditions, fieldRange, RecordType.MARC_AUTHORITY);
  }
}
