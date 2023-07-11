package org.folio.entlinks.controller.delegate.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.repository.AuthorityDataRepository;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.internal.SearchService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionService;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksSuggestionsByAuthorityNaturalIdTest {
  @Mock
  private InstanceAuthorityLinkingRulesService linkingRulesService;
  @Mock
  private LinksSuggestionService suggestionService;
  @Mock
  private AuthorityDataRepository dataRepository;
  @Mock
  private SourceStorageClient sourceStorageClient;
  @Mock
  private SourceContentMapper contentMapper;
  @Mock
  private SearchService searchService;

  @InjectMocks
  private LinksSuggestionsByAuthorityNaturalId delegate;

  @Test
  void extractIds_positive() {
    var expectedNaturalIds = List.of("test0", "test1", "test2");

    var subfields = Map.of("0", expectedNaturalIds.subList(0, 2));
    var linkDetails = new LinkDetails().authorityNaturalId(expectedNaturalIds.get(2));
    var field = new FieldParsedContent("100", "/", "/", subfields, linkDetails);

    var actual = delegate.extractIds(field);
    assertThat(actual).isEqualTo(new HashSet<>(expectedNaturalIds));
  }

  @Test
  void extractIds_negative_noSubfieldsAndNullLinkDetails() {
    var field = new FieldParsedContent("100", "/", "/", new HashMap<>(), null);
    var actual = delegate.extractIds(field);
    assertThat(actual).isEmpty();
  }

  @Test
  void extractIds_negative_noSubfieldsAndNullNaturalId() {
    var field = new FieldParsedContent("100", "/", "/", new HashMap<>(), new LinkDetails());
    var actual = delegate.extractIds(field);
    assertThat(actual).isEmpty();
  }

  @Test
  void findExistingAuthorities_positive() {
    var ids = new HashSet<String>();
    delegate.findExistingAuthorities(ids);
    verify(dataRepository).findByNaturalIds(ids);
  }

  @Test
  void extractId_positive() {
    var authorityData = AuthorityData.builder().naturalId("test").build();
    var actual = delegate.extractId(authorityData);
    assertThat(actual).isEqualTo(authorityData.getNaturalId());
  }

  @Test
  void searchAuthorities_positive() {
    var ids = new HashSet<String>();
    delegate.searchAuthorities(ids);
    verify(searchService).searchAuthoritiesByNaturalIds(new ArrayList<>(ids));
  }
}
