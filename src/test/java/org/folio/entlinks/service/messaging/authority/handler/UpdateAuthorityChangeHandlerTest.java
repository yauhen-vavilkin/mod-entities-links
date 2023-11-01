package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.NATURAL_ID;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.PERSONAL_NAME;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.config.properties.InstanceAuthorityChangeProperties;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.ChangeTargetLink;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.entlinks.integration.dto.AuthoritySourceRecord;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.AuthorityMappingRulesProcessingService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marc4j.marc.impl.RecordImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UpdateAuthorityChangeHandlerTest {

  private @Mock AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private @Mock InstanceAuthorityLinkingRulesService linkingRulesService;
  private @Mock EventProducer<LinkUpdateReport> linksUpdateKafkaTemplate;
  private @Mock FolioExecutionContext context;
  private @Mock InstanceAuthorityLinkingService linkingService;
  private @Mock InstanceAuthorityChangeProperties instanceAuthorityChangeProperties;
  private @Mock AuthoritySourceFileRepository sourceFileRepository;
  private @InjectMocks UpdateAuthorityChangeHandler handler;

  @Captor
  private ArgumentCaptor<List<LinkUpdateReport>> producerRecord;

  @Test
  void getReplyEventType_positive() {
    var actual = handler.getReplyEventType();

    assertEquals(TypeEnum.UPDATE, actual);
  }

  @Test
  void supportedInventoryEventType_positive() {
    var actual = handler.supportedAuthorityChangeType();

    assertEquals(AuthorityChangeType.UPDATE, actual);
  }

  @Test
  void handle_negative_whenTagIsNotSupported() {
    UUID id = UUID.randomUUID();

    var expected = new LinkUpdateReport();
    expected.setFailCause("Source record don't contains [authorityId: " + id + ", tag: notExistingTag]");
    expected.setTenant(context.getTenantId());
    expected.setStatus(LinkUpdateReport.StatusEnum.FAIL);

    when(mappingRulesProcessingService.getTagByAuthorityChangeField(any())).thenReturn("notExistingTag");

    var changes = Map.of(
      PERSONAL_NAME, new AuthorityChange(PERSONAL_NAME, "new", "old")
    );
    var event = new AuthorityChangeHolder(new AuthorityDomainEvent(id), changes, emptyMap(), 0);
    event.setSourceRecord(new AuthoritySourceRecord(id, UUID.randomUUID(), new RecordImpl()));
    handler.handle(List.of(event));

    verify(linksUpdateKafkaTemplate).sendMessages(producerRecord.capture());
    assertThat(producerRecord.getValue().get(0))
      .extracting("tenant", "failCause", "status")
      .contains(expected.getTenant(), expected.getFailCause(), expected.getStatus());
  }

  @Test
  void handle_positive_whenNaturalIdChanged() {
    var authorityId = UUID.randomUUID();
    var instanceId = UUID.randomUUID();

    when(instanceAuthorityChangeProperties.getNumPartitions()).thenReturn(2);
    when(linkingService.getLinksByAuthorityId(eq(authorityId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
      new InstanceAuthorityLink(1L, new Authority().withId(authorityId), instanceId,
        new InstanceAuthorityLinkingRule(1, "100", "100", new char[] {'a'}, null, null, true),
        InstanceAuthorityLinkStatus.ACTUAL, null)
    )));

    var changeHolder = new AuthorityChangeHolder(new AuthorityDomainEvent(authorityId,
      new AuthorityDto().naturalId("n1010101").sourceFileId(UUID.randomUUID()),
      new AuthorityDto().naturalId("1010101"), DomainEventType.UPDATE, TENANT_ID),
      Map.of(NATURAL_ID, new AuthorityChange(NATURAL_ID, "1010101", "n1010101")), emptyMap(), 1);
    changeHolder.setSourceRecord(new AuthoritySourceRecord(authorityId, UUID.randomUUID(), new RecordImpl()));
    var actual = handler.handle(List.of(changeHolder));
    assertThat(actual).isNotEmpty().hasSize(1);
    assertThat(actual.get(0))
      .extracting(LinksChangeEvent::getAuthorityId, LinksChangeEvent::getType)
      .containsExactly(authorityId, TypeEnum.UPDATE);
    assertThat(actual.get(0).getUpdateTargets()).hasSize(1);
    assertThat(actual.get(0).getUpdateTargets().get(0))
      .hasFieldOrPropertyWithValue("field", "100")
      .hasFieldOrPropertyWithValue("links", List.of(new ChangeTargetLink().linkId(1L).instanceId(instanceId)));

    assertThat(actual.get(0).getSubfieldsChanges()).hasSize(1);
    assertThat(actual.get(0).getSubfieldsChanges().get(0).getSubfields()).hasSize(1);
    assertThat(actual.get(0).getSubfieldsChanges().get(0).getSubfields().get(0))
      .extracting("code", "value")
      .containsExactly("0", "1010101");
  }

  @Test
  void handle_positive_emptyEventList() {
    var actual = handler.handle(emptyList());

    assertThat(actual).isEmpty();
  }

  @Test
  void handle_positive_nullEventList() {
    var actual = handler.handle(null);

    assertThat(actual).isEmpty();
  }
}
