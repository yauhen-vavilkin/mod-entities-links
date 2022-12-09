package org.folio.entlinks.integration.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.folio.entlinks.client.MappingRulesClient;
import org.folio.entlinks.client.MappingRulesClient.MappingRule;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class MappingRulesServiceTest {

  private @Mock MappingRulesClient client;
  private @InjectMocks MappingRulesService service;

  @Test
  void getFieldTargetsMappingRelations_positive() {
    when(client.fetchAuthorityMappingRules()).thenReturn(Map.of(
      "100", List.of(new MappingRule("a1"), new MappingRule("a2")),
      "101", List.of(new MappingRule("a3"))
    ));

    var actual = service.getFieldTargetsMappingRelations();

    assertThat(actual)
      .hasSize(2)
      .contains(entry("100", List.of("a1", "a2")), entry("101", List.of("a3")));
  }

  @Test
  void getFieldTargetsMappingRelations_negative_clientException() {
    var cause = new IllegalArgumentException("test");
    when(client.fetchAuthorityMappingRules()).thenThrow(cause);

    assertThatThrownBy(() -> service.getFieldTargetsMappingRelations())
      .isInstanceOf(FolioIntegrationException.class)
      .hasCauseExactlyInstanceOf(cause.getClass())
      .hasMessage("Failed to fetch authority mapping rules");
  }
}
