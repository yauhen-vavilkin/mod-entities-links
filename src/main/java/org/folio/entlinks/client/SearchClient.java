package org.folio.entlinks.client;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.AuthoritySearchResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("search")
public interface SearchClient {

  String OR = " or ";
  String AND = " and ";
  String ID_CQL = "id=%s";
  String NATURAL_ID_CQL = "naturalId=\"%s\"";
  String AUTHORIZED_REF_TYPE_CQL = "authRefType=Authorized";

  @GetMapping("/authorities")
  AuthoritySearchResult searchAuthorities(@RequestParam String query,
                                          @RequestParam boolean includeNumberOfTitles);

  default String buildNaturalIdsQuery(Set<String> naturalIds) {
    return buildQuery(naturalIds, id -> String.format(NATURAL_ID_CQL, id));
  }

  default String buildIdsQuery(Set<UUID> ids) {
    return buildQuery(ids, id -> String.format(ID_CQL, id));
  }

  private static <T> String buildQuery(Set<T> params, Function<T, String> queryFunction) {
    return AUTHORIZED_REF_TYPE_CQL + AND + params.stream()
      .map(queryFunction)
      .collect(Collectors.joining(OR, "(", ")"));
  }
}
