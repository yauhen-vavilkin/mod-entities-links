package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("mapping-rules")
public interface MappingRulesClient {

  @GetMapping(value = "/marc-authority", produces = APPLICATION_JSON_VALUE)
  Map<String, List<MappingRule>> fetchAuthorityMappingRules();

  record MappingRule(String target) { }

}
