package org.folio.entlinks.client;

import java.util.Set;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.AuthoritySearchResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("search")
public interface SearchClient {

  String OR = " or ";
  String AND = " and ";
  String NATURAL_ID = "naturalId=%s";
  String AUTHORIZED_REF_TYPE = "authRefType=Authorized";

  @GetMapping("/authorities")
  AuthoritySearchResult searchAuthorities(@RequestParam String query,
                                          @RequestParam boolean includeNumberOfTitles);

  default String buildNaturalIdsQuery(Set<String> naturalIds) {
    return AUTHORIZED_REF_TYPE + AND + naturalIds.stream()
      .map(id -> String.format(NATURAL_ID, id))
      .collect(Collectors.joining(OR, "(", ")"));
  }
}
