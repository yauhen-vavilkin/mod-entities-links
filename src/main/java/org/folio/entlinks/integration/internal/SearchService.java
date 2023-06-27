package org.folio.entlinks.integration.internal;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.folio.entlinks.client.SearchClient;
import org.folio.entlinks.controller.converter.DataMapper;
import org.folio.entlinks.domain.dto.AuthorityRecord;
import org.folio.entlinks.domain.dto.AuthoritySearchResult;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

  private static final int REQUEST_PARAM_MAX_SIZE_DEFAULT = 10;

  private final SearchClient searchClient;
  private final DataMapper dataMapper;

  private @Setter int requestParamMaxSize = REQUEST_PARAM_MAX_SIZE_DEFAULT;

  public List<AuthorityData> searchAuthoritiesByIds(Collection<UUID> ids) {
    return getAuthorityData(ids, params -> searchClient.buildIdsQuery(new HashSet<>(params)));
  }

  public List<AuthorityData> searchAuthoritiesByNaturalIds(Collection<String> naturalIds) {
    return getAuthorityData(naturalIds, params -> searchClient.buildNaturalIdsQuery(new HashSet<>(params)));
  }

  private <T> List<AuthorityData> getAuthorityData(Collection<T> params, Function<List<T>, String> paramsMapper) {
    if (params == null || params.isEmpty()) {
      return Collections.emptyList();
    }

    return Lists.partition(new ArrayList<>(params), requestParamMaxSize).stream()
      .map(paramsMapper)
      .map(this::doRequest)
      .flatMap(authoritySearchResult -> authoritySearchResult.getAuthorities().stream())
      .map(this::toAuthorityData)
      .toList();
  }

  private AuthoritySearchResult doRequest(String query) {
    try {
      return searchClient.searchAuthorities(query, false);
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch authorities", e);
    }
  }

  private AuthorityData toAuthorityData(AuthorityRecord authority) {
    var authorityData = dataMapper.convertToData(authority);
    authorityData.setDeleted(false);
    return authorityData;
  }
}
