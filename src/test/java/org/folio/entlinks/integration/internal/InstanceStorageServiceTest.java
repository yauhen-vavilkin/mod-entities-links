package org.folio.entlinks.integration.internal;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.client.InstanceStorageClient;
import org.folio.entlinks.client.InstanceStorageClient.InventoryInstanceDto;
import org.folio.entlinks.client.InstanceStorageClient.InventoryInstanceDtoCollection;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceStorageServiceTest {

  private @Mock InstanceStorageClient client;
  private @InjectMocks InstanceStorageService service;

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(service, "instanceBatchSize", 2);
  }

  @Test
  void getInstanceTitles_positive() {
    var e1 = new InventoryInstanceDto(UUID.randomUUID().toString(), "title1");
    var e2 = new InventoryInstanceDto(UUID.randomUUID().toString(), "title2");
    var instances = List.of(e1, e2);

    when(client.getInstanceStorageInstances(anyString(), anyInt()))
      .thenReturn(new InventoryInstanceDtoCollection(instances));

    var instanceIds = List.of(e1.id(), e2.id());
    var actual = service.getInstanceTitles(instanceIds);

    verify(client)
      .getInstanceStorageInstances(String.format("id==(%s or %s)", e1.id(), e2.id()), instanceIds.size());
    assertThat(actual)
      .hasSize(instanceIds.size())
      .contains(entry(e1.id(), e1.title()), entry(e2.id(), e2.title()));
  }

  @Test
  void getInstanceTitles_positive_singleRecord() {
    var e1 = new InventoryInstanceDto(UUID.randomUUID().toString(), "title1");
    var instances = singletonList(e1);

    when(client.getInstanceStorageInstances(anyString(), anyInt()))
      .thenReturn(new InventoryInstanceDtoCollection(instances));

    var instanceIds = singletonList(e1.id());
    var actual = service.getInstanceTitles(instanceIds);

    verify(client)
      .getInstanceStorageInstances(String.format("id==(%s)", e1.id()), instanceIds.size());
    assertThat(actual)
      .hasSize(instanceIds.size())
      .contains(entry(e1.id(), e1.title()));
  }

  @Test
  void getInstanceTitles_positive_multipleBatches() {
    var e1 = new InventoryInstanceDto(UUID.randomUUID().toString(), "title1");
    var e2 = new InventoryInstanceDto(UUID.randomUUID().toString(), "title2");
    var e3 = new InventoryInstanceDto(UUID.randomUUID().toString(), "title3");
    var instancesBatch1 = List.of(e1, e2);
    var instancesBatch2 = singletonList(e3);

    when(client.getInstanceStorageInstances(anyString(), anyInt()))
      .thenReturn(new InventoryInstanceDtoCollection(instancesBatch1))
      .thenReturn(new InventoryInstanceDtoCollection(instancesBatch2));

    var instanceIds = List.of(e1.id(), e2.id(), e3.id());
    var actual = service.getInstanceTitles(instanceIds);

    verify(client)
      .getInstanceStorageInstances(String.format("id==(%s or %s)", e1.id(), e2.id()), instancesBatch1.size());
    verify(client)
      .getInstanceStorageInstances(String.format("id==(%s)", e3.id()), instancesBatch2.size());
    assertThat(actual)
      .hasSize(instanceIds.size())
      .contains(entry(e1.id(), e1.title()), entry(e2.id(), e2.title()), entry(e3.id(), e3.title()));
  }

  @Test
  void getInstanceTitles_negative_clientException() {
    var cause = new IllegalArgumentException("test message");
    when(client.getInstanceStorageInstances(anyString(), anyInt())).thenThrow(cause);

    var instanceIds = singletonList(UUID.randomUUID().toString());
    assertThatThrownBy(() -> service.getInstanceTitles(instanceIds))
      .isInstanceOf(FolioIntegrationException.class)
      .hasCauseExactlyInstanceOf(cause.getClass())
      .hasMessage("Failed to fetch instances");

  }
}
