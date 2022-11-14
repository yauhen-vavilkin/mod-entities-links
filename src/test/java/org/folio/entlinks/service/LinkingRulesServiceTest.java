package org.folio.entlinks.service;

import org.folio.entlinks.model.converter.LinkingRulesMapperImpl;
import org.folio.entlinks.model.entity.LinkingRules;
import org.folio.entlinks.repository.LinkingRulesRepository;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.json.JsonParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.LinkingPairType.INSTANCE_AUTHORITY;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinkingRulesServiceTest {

  @Mock
  private LinkingRulesRepository repository;

  private LinkingRulesService service;

  @BeforeEach
  void setUp() {
    service = new LinkingRulesService(repository, new LinkingRulesMapperImpl());
  }

  @Test
  void getInstanceAuthorityRules_negative_invalidJsonFormat() {
    var invalidRules = LinkingRules.builder()
        .linkingPairType(INSTANCE_AUTHORITY.name())
        .jsonb("invalid json")
        .build();

    when(repository.findByLinkingPairType(INSTANCE_AUTHORITY.name())).thenReturn(invalidRules);

    var exception = Assertions.assertThrows(JsonParseException.class,
        () -> service.getLinkingRules(INSTANCE_AUTHORITY));

    assertThat(exception)
        .hasMessage("Cannot parse JSON");
  }
}
