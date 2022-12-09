package org.folio.entlinks.controller;

import static org.folio.support.TestUtils.readFile;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import lombok.SneakyThrows;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;

@IntegrationTest
class InstanceAuthorityLinkingRulesIT extends IntegrationTestBase {

  private static final String LINKING_RULES_ENDPOINT = "/linking-rules/instance-authority";
  private static final String AUTHORITY_RULES_PATH = "classpath:linking-rules/instance-authority.json";

  @Test
  @SneakyThrows
  void getLinkingRules_positive_getInstanceAuthorityRules() {
    var defaultRules = readFile(AUTHORITY_RULES_PATH);

    doGet(LINKING_RULES_ENDPOINT)
      .andExpect(content().json(defaultRules));
  }
}
