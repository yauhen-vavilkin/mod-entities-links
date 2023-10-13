package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.config.properties.InstanceAuthorityChangeProperties;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.entlinks.integration.dto.AuthoritySourceRecord;
import org.folio.entlinks.integration.internal.AuthoritySourceRecordService;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.AuthorityMappingRulesProcessingService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
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

@UnitTest
@ExtendWith(MockitoExtension.class)
class UpdateAuthorityChangeHandlerTest {

  private @Mock AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private @Mock AuthoritySourceRecordService sourceRecordService;
  private @Mock InstanceAuthorityLinkingRulesService linkingRulesService;
  private @Mock EventProducer<LinkUpdateReport> linksUpdateKafkaTemplate;
  private @Mock FolioExecutionContext context;
  private @Mock InstanceAuthorityLinkingService linkingService;
  private @Mock InstanceAuthorityChangeProperties instanceAuthorityChangeProperties;
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
  void handle_positive() {
    UUID id = UUID.randomUUID();

    var expected = new LinkUpdateReport();
    expected.setFailCause("Source record don't contains [authorityId: " + id + ", tag: notExistingTag]");
    expected.setTenant(context.getTenantId());
    expected.setStatus(LinkUpdateReport.StatusEnum.FAIL);

    when(mappingRulesProcessingService.getTagByAuthorityChangeField(any())).thenReturn("notExistingTag");

    var changes = Map.of(
      AuthorityChangeField.PERSONAL_NAME, new AuthorityChange(AuthorityChangeField.PERSONAL_NAME, "new", "old")
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
