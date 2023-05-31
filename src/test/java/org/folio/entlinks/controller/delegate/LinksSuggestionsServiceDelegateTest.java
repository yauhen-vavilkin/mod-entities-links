package org.folio.entlinks.controller.delegate;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.client.SearchClient;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.DataMapper;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.dto.Authority;
import org.folio.entlinks.domain.dto.AuthoritySearchResult;
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
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.AuthorityDataRepository;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.folio.spring.test.type.UnitTest;
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
  private static final String MAX_AUTHORITY_FIELD = "150";
  private static final String NATURAL_ID = "e12345";
  private static final String BASE_URL = "https://base/url/";
  private static final String EXPECTED_SEARCH_QUERY = "authRefType=Authorized and (naturalId=" + NATURAL_ID + ')';

  private @Spy SourceContentMapper contentMapper = Mappers.getMapper(SourceContentMapper.class);
  private @Spy DataMapper dataMapper = Mappers.getMapper(DataMapper.class);
  private @Mock InstanceAuthorityLinkingRulesService linkingRulesService;
  private @Mock LinksSuggestionService suggestionService;
  private @Mock AuthorityDataRepository dataRepository;
  private @Mock SourceStorageClient sourceStorageClient;
  private @Mock SearchClient searchClient;
  private @InjectMocks LinksSuggestionsServiceDelegate serviceDelegate;

  @Test
  void suggestLinksForMarcRecords_shouldSaveAuthoritiesFromSearch() {
    var records = List.of(getRecord("100"));
    var rules = List.of(getRule("100"));

    var authority = new Authority().id(AUTHORITY_ID).naturalId(NATURAL_ID);
    var authorities = new AuthoritySearchResult().authorities(List.of(authority));
    var authorityData = List.of(new AuthorityData(AUTHORITY_ID, NATURAL_ID, false));
    var fetchRequest = getBatchFetchRequestForAuthority(AUTHORITY_ID);
    var strippedParsedRecords = new StrippedParsedRecordCollection(emptyList(), 1);

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(linkingRulesService.getMinAuthorityField()).thenReturn(MIN_AUTHORITY_FIELD);
    when(linkingRulesService.getMaxAuthorityField()).thenReturn(MAX_AUTHORITY_FIELD);

    when(searchClient.buildNaturalIdsQuery(Set.of(NATURAL_ID))).thenReturn(EXPECTED_SEARCH_QUERY);
    when(searchClient.searchAuthorities(EXPECTED_SEARCH_QUERY, false)).thenReturn(authorities);

    when(dataRepository.findByNaturalIds(Set.of(NATURAL_ID))).thenReturn(new ArrayList<>());
    when(dataRepository.saveAll(authorityData)).thenReturn(authorityData);

    when(sourceStorageClient
      .buildBatchFetchRequestForAuthority(Set.of(AUTHORITY_ID), MIN_AUTHORITY_FIELD, MAX_AUTHORITY_FIELD))
      .thenReturn(fetchRequest);
    when(sourceStorageClient.fetchParsedRecordsInBatch(fetchRequest)).thenReturn(
      strippedParsedRecords);

    var parsedContentCollection = new ParsedRecordContentCollection().records(records);
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection);

    verify(dataRepository).saveAll(authorityData);
    verify(searchClient).searchAuthorities(EXPECTED_SEARCH_QUERY, false);
    verify(sourceStorageClient).fetchParsedRecordsInBatch(fetchRequest);
  }

  @Test
  void suggestLinksForMarcRecords_shouldRetrieveAuthoritiesFromTable() {
    var records = List.of(getRecord("100", Map.of("0", NATURAL_ID)));
    var rules = List.of(getRule("100"));
    var authorityData = List.of(new AuthorityData(AUTHORITY_ID, NATURAL_ID, false));
    var fetchRequest = getBatchFetchRequestForAuthority(AUTHORITY_ID);

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(linkingRulesService.getMinAuthorityField()).thenReturn(MIN_AUTHORITY_FIELD);
    when(linkingRulesService.getMaxAuthorityField()).thenReturn(MAX_AUTHORITY_FIELD);

    when(dataRepository.findByNaturalIds(Set.of(NATURAL_ID))).thenReturn(authorityData);
    when(sourceStorageClient
      .buildBatchFetchRequestForAuthority(Set.of(AUTHORITY_ID), MIN_AUTHORITY_FIELD, MAX_AUTHORITY_FIELD))
      .thenReturn(fetchRequest);
    when(sourceStorageClient.fetchParsedRecordsInBatch(fetchRequest)).thenReturn(
      new StrippedParsedRecordCollection(emptyList(), 1));

    var parsedContentCollection = new ParsedRecordContentCollection().records(records);
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection);

    verify(dataRepository).findByNaturalIds(Set.of(NATURAL_ID));
    verify(searchClient, times(0)).searchAuthorities(anyString(), anyBoolean());
    verify(sourceStorageClient).fetchParsedRecordsInBatch(fetchRequest);
  }

  @Test
  void suggestLinksForMarcRecords_shouldExtractNaturalIdFrom0Subfield() {
    var records = List.of(getRecord("100", Map.of("0", BASE_URL + NATURAL_ID)));
    var rules = List.of(getRule("100"));
    var authorityData = List.of(new AuthorityData(AUTHORITY_ID, NATURAL_ID, false));
    var fetchRequest = getBatchFetchRequestForAuthority(AUTHORITY_ID);

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(linkingRulesService.getMinAuthorityField()).thenReturn(MIN_AUTHORITY_FIELD);
    when(linkingRulesService.getMaxAuthorityField()).thenReturn(MAX_AUTHORITY_FIELD);

    when(dataRepository.findByNaturalIds(Set.of(NATURAL_ID))).thenReturn(authorityData);
    when(sourceStorageClient
      .buildBatchFetchRequestForAuthority(Set.of(AUTHORITY_ID), MIN_AUTHORITY_FIELD, MAX_AUTHORITY_FIELD))
      .thenReturn(fetchRequest);
    when(sourceStorageClient.fetchParsedRecordsInBatch(fetchRequest)).thenReturn(
      new StrippedParsedRecordCollection(emptyList(), 1));

    var parsedContentCollection = new ParsedRecordContentCollection().records(records);
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection);

    verify(dataRepository).findByNaturalIds(Set.of(NATURAL_ID));
    verify(searchClient, times(0)).searchAuthorities(anyString(), anyBoolean());
    verify(sourceStorageClient).fetchParsedRecordsInBatch(fetchRequest);
  }

  @Test
  void suggestLinksForMarcRecords_shouldNotFetchAuthorities_ifNoNaturalIdsWasFound() {
    var record = new ParsedRecordContent(emptyList(), "record without naturalId");
    var rules = List.of(getRule("110"));

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);
    when(dataRepository.findByNaturalIds(emptySet())).thenReturn(emptyList());

    var parsedContentCollection = new ParsedRecordContentCollection().records(List.of(record));
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection);

    verify(dataRepository).findByNaturalIds(emptySet());
    verify(searchClient, times(0)).searchAuthorities(anyString(), anyBoolean());
    verify(sourceStorageClient, times(0)).fetchParsedRecordsInBatch(any());
  }

  @Test
  void suggestLinksForMarcRecords_ifBibFieldTagNoConsistInLinkingRules() {
    var records = List.of(getRecord("100"));
    var rules = List.of(getRule("110"));

    when(linkingRulesService.getLinkingRules()).thenReturn(rules);

    var parsedContentCollection = new ParsedRecordContentCollection().records(records);
    serviceDelegate.suggestLinksForMarcRecords(parsedContentCollection);

    verify(dataRepository).findByNaturalIds(emptySet());
    verify(searchClient, times(0)).searchAuthorities(anyString(), anyBoolean());
    verify(sourceStorageClient, times(0)).fetchParsedRecordsInBatch(any());
  }

  private ParsedRecordContent getRecord(String bibField) {
    return getRecord(bibField, Map.of("a", "test"));
  }

  private ParsedRecordContent getRecord(String bibField, Map<String, String> subfields) {
    var field = new FieldContent();
    field.setSubfields(List.of(subfields));
    field.setLinkDetails(new LinkDetails().naturalId(NATURAL_ID));

    var fields = Map.of(bibField, field);
    return new ParsedRecordContent(List.of(fields), "default leader");
  }

  private InstanceAuthorityLinkingRule getRule(String bibField) {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);

    var rule = new InstanceAuthorityLinkingRule();
    rule.setId(1);
    rule.setBibField(bibField);
    rule.setAuthorityField("100");
    rule.setAutoLinkingEnabled(true);
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
