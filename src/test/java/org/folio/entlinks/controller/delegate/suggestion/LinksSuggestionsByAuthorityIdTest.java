package org.folio.entlinks.controller.delegate.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class LinksSuggestionsByAuthorityIdTest {
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
  private LinksSuggestionsByAuthorityId delegate;

  @Test
  void extractIds_positive() {
    var authorityId = UUID.randomUUID();
    var expectedIds = List.of(UUID.randomUUID(), authorityId);
    var ids = List.of(expectedIds.get(0).toString(), "test", "");

    var subfields = Map.of("9", ids);
    var linkDetails = new LinkDetails().authorityId(authorityId);
    var field = new FieldParsedContent("100", "/", "/", subfields, linkDetails);

    var actual = delegate.extractIds(field);
    assertThat(actual).isEqualTo(new HashSet<>(expectedIds));
  }

  @Test
  void extractIds_negative_noSubfieldsAndNullLinkDetails() {
    var field = new FieldParsedContent("100", "/", "/", new HashMap<>(), null);
    var actual = delegate.extractIds(field);
    assertThat(actual).isEmpty();
  }

  @Test
  void extractIds_negative_noSubfieldsAndNullAuthorityId() {
    var field = new FieldParsedContent("100", "/", "/", new HashMap<>(), new LinkDetails());
    var actual = delegate.extractIds(field);
    assertThat(actual).isEmpty();
  }

  @Test
  void findExistingAuthorities_positive() {
    var ids = new HashSet<UUID>();
    delegate.findExistingAuthorities(ids);
    verify(dataRepository).findAllById(ids);
  }

  @Test
  void extractId_positive() {
    var authorityData = AuthorityData.builder().id(UUID.randomUUID()).build();
    var actual = delegate.extractId(authorityData);
    assertThat(actual).isEqualTo(authorityData.getId());
  }

  @Test
  void searchAuthorities_positive() {
    var ids = new HashSet<UUID>();
    delegate.searchAuthorities(ids);
    verify(searchService).searchAuthoritiesByIds(new ArrayList<>(ids));
  }
}
