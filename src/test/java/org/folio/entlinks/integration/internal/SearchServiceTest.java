package org.folio.entlinks.integration.internal;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.client.SearchClient;
import org.folio.entlinks.controller.converter.DataMapper;
import org.folio.entlinks.domain.dto.AuthorityRecord;
import org.folio.entlinks.domain.dto.AuthoritySearchResult;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

  private @Mock SearchClient searchClient;
  private @Mock DataMapper dataMapper;
  private @InjectMocks SearchService searchService;

  @Test
  void testSearchAuthoritiesByIds_withNullNaturalIds_shouldReturnEmptyList() {
    // Arrange
    List<UUID> naturalIds = null;

    // Act
    List<AuthorityData> result = searchService.searchAuthoritiesByIds(naturalIds);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(searchClient);
    verifyNoInteractions(dataMapper);
  }

  @Test
  void testSearchAuthoritiesByIds_withEmptyNaturalIds_shouldReturnEmptyList() {
    // Arrange
    List<UUID> naturalIds = Collections.emptyList();

    // Act
    List<AuthorityData> result = searchService.searchAuthoritiesByIds(naturalIds);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(searchClient);
    verifyNoInteractions(dataMapper);
  }

  @Test
  void testSearchAuthoritiesByIds_withNonEmptyNaturalIds_shouldReturnListOfAuthorityData() {
    // Arrange
    final var ids = Arrays.asList(randomUUID(), randomUUID(), randomUUID());

    AuthoritySearchResult searchResult1 = new AuthoritySearchResult();
    searchResult1.setAuthorities(Arrays.asList(new AuthorityRecord(), new AuthorityRecord()));

    AuthoritySearchResult searchResult2 = new AuthoritySearchResult();
    searchResult2.setAuthorities(Collections.singletonList(new AuthorityRecord()));

    AuthorityData authorityData1 = new AuthorityData();
    AuthorityData authorityData2 = new AuthorityData();
    AuthorityData authorityData3 = new AuthorityData();

    when(searchClient.buildIdsQuery(anySet())).thenReturn("query1", "query2");
    when(searchClient.searchAuthorities(anyString(), eq(false))).thenReturn(searchResult1, searchResult2);
    when(dataMapper.convertToData(any(AuthorityRecord.class)))
        .thenReturn(authorityData1, authorityData2, authorityData3);
    searchService.setRequestParamMaxSize(2);

    // Act
    List<AuthorityData> result = searchService.searchAuthoritiesByIds(ids);

    // Assert
    assertThat(result).hasSize(3)
      .containsAll(List.of(authorityData1, authorityData2, authorityData3))
      .allMatch(authorityData -> !authorityData.isDeleted());
    verify(searchClient, times(2)).searchAuthorities(anyString(), eq(false));
    verify(dataMapper, times(3)).convertToData(any(AuthorityRecord.class));
  }

  @Test
  void testSearchAuthoritiesByNaturalIds_withNullNaturalIds_shouldReturnEmptyList() {
    // Arrange
    List<String> naturalIds = null;

    // Act
    List<AuthorityData> result = searchService.searchAuthoritiesByNaturalIds(naturalIds);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(searchClient);
    verifyNoInteractions(dataMapper);
  }

  @Test
  void testSearchAuthoritiesByNaturalIds_withEmptyNaturalIds_shouldReturnEmptyList() {
    // Arrange
    List<String> naturalIds = Collections.emptyList();

    // Act
    List<AuthorityData> result = searchService.searchAuthoritiesByNaturalIds(naturalIds);

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(searchClient);
    verifyNoInteractions(dataMapper);
  }

  @Test
  void testSearchAuthoritiesByNaturalIds_withNonEmptyNaturalIds_shouldReturnListOfAuthorityData() {
    // Arrange
    final var naturalIds = Arrays.asList("id1", "id2", "id3");

    AuthoritySearchResult searchResult1 = new AuthoritySearchResult();
    searchResult1.setAuthorities(Arrays.asList(new AuthorityRecord(), new AuthorityRecord()));

    AuthoritySearchResult searchResult2 = new AuthoritySearchResult();
    searchResult2.setAuthorities(Collections.singletonList(new AuthorityRecord()));

    AuthorityData authorityData1 = new AuthorityData();
    AuthorityData authorityData2 = new AuthorityData();
    AuthorityData authorityData3 = new AuthorityData();

    when(searchClient.buildNaturalIdsQuery(anySet())).thenReturn("query1", "query2");
    when(searchClient.searchAuthorities(anyString(), eq(false))).thenReturn(searchResult1, searchResult2);
    when(dataMapper.convertToData(any(AuthorityRecord.class)))
        .thenReturn(authorityData1, authorityData2, authorityData3);
    searchService.setRequestParamMaxSize(2);

    // Act
    List<AuthorityData> result = searchService.searchAuthoritiesByNaturalIds(naturalIds);

    // Assert
    assertThat(result).hasSize(3)
      .containsAll(List.of(authorityData1, authorityData2, authorityData3))
      .allMatch(authorityData -> !authorityData.isDeleted());
    verify(searchClient, times(2)).searchAuthorities(anyString(), eq(false));
    verify(dataMapper, times(3)).convertToData(any(AuthorityRecord.class));
  }
}
