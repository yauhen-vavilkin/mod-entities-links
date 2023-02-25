package org.folio.entlinks.controller;

import static org.folio.support.FileTestUtils.readFile;
import static org.folio.support.base.TestConstants.linkingRulesEndpoint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import lombok.SneakyThrows;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;

@IntegrationTest
class InstanceAuthorityLinkingRulesIT extends IntegrationTestBase {

  private static final String AUTHORITY_RULES_PATH = "classpath:linking-rules/instance-authority.json";

  @Test
  @SneakyThrows
  void getLinkingRules_positive_getInstanceAuthorityRules() {
    var defaultRules = readFile(AUTHORITY_RULES_PATH);

    doGet(linkingRulesEndpoint())
      .andExpect(content().json(defaultRules));
  }
}
