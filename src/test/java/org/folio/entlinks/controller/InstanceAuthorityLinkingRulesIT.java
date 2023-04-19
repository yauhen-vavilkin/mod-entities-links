package org.folio.entlinks.controller;

import static java.lang.Integer.MAX_VALUE;
import static org.folio.support.FileTestUtils.readFile;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.JsonTestUtils.toObject;
import static org.folio.support.base.TestConstants.linkingRulesEndpoint;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.dto.SubfieldValidation;
import org.folio.entlinks.exception.type.ErrorCode;
import org.folio.spring.test.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Log4j2
@IntegrationTest
class InstanceAuthorityLinkingRulesIT extends IntegrationTestBase {

  private static final String AUTHORITY_RULES_PATH = "classpath:linking-rules/instance-authority.json";
  private static final TypeReference<List<LinkingRuleDto>> RULES_TYPE_REFERENCE = new TypeReference<>() { };

  private List<LinkingRuleDto> defaultRules;

  @BeforeEach
  void setUp() {
    log.info("Restoring default rules: started");
    defaultRules = toObject(readFile(AUTHORITY_RULES_PATH), RULES_TYPE_REFERENCE, objectMapper);
    for (LinkingRuleDto defaultRule : defaultRules) {
      doPatch(linkingRulesEndpoint(defaultRule.getId()), defaultRule);
    }
    log.info("Restoring default rules: finished");
  }

  @Test
  @SneakyThrows
  void getLinkingRules_positive() {
    doGet(linkingRulesEndpoint())
      .andExpect(content().json(asJson(defaultRules, objectMapper), true));
  }

  @Test
  @SneakyThrows
  void getLinkingRulesById_positive() {
    doGet(linkingRulesEndpoint(1))
      .andExpect(content().json(asJson(defaultRules.get(0), objectMapper), true));
  }

  @Test
  @SneakyThrows
  void getLinkingRulesById_negative_notFound() {
    tryGet(linkingRulesEndpoint(MAX_VALUE))
      .andExpect(status().isNotFound())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("LinkingRuleNotFoundException")))
      .andExpect(errorCodeMatch(is(ErrorCode.NOT_FOUND_ERROR.getValue())))
      .andExpect(errorMessageMatch(is(String.format("Linking rule with ID [%s] was not found", MAX_VALUE))));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_positive_shouldUpdateExpectedFields() {
    var request = new LinkingRulePatchRequest().id(1).autoLinkingEnabled(false);
    doPatch(linkingRulesEndpoint(1), request);

    doGet(linkingRulesEndpoint(1))
      .andExpect(content().json(asJson(defaultRules.get(0).autoLinkingEnabled(false), objectMapper)));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_positive_shouldNotUpdateUnexpectedFields() {
    var request = new LinkingRuleDto().id(1)
      .authorityField("abc")
      .authoritySubfields(List.of("a"))
      .addSubfieldModificationsItem(new SubfieldModification().source("1").target("2"))
      .validation(new SubfieldValidation().addExistenceItem(Map.of("s", true)))
      .autoLinkingEnabled(true);
    doPatch(linkingRulesEndpoint(1), request);

    doGet(linkingRulesEndpoint(1))
      .andExpect(content().json(asJson(defaultRules.get(0), objectMapper), true));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_negative_notFound() {
    var request = new LinkingRulePatchRequest().id(MAX_VALUE).autoLinkingEnabled(false);
    tryPatch(linkingRulesEndpoint(MAX_VALUE), request)
      .andExpect(status().isNotFound())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("LinkingRuleNotFoundException")))
      .andExpect(errorCodeMatch(is(ErrorCode.NOT_FOUND_ERROR.getValue())))
      .andExpect(errorMessageMatch(is(String.format("Linking rule with ID [%s] was not found", MAX_VALUE))));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_negative_requestBodyValidation() {
    var request = new LinkingRulePatchRequest().autoLinkingEnabled(false);
    tryPatch(linkingRulesEndpoint(MAX_VALUE), request)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorCode.VALIDATION_ERROR.getValue())))
      .andExpect(errorParameterMatch(is("id")))
      .andExpect(errorMessageMatch(is(String.format("Request should have id = %s", MAX_VALUE))));
  }
}
