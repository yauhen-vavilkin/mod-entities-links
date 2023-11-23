package org.folio.entlinks.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

  @Mock
  private SettingsClient settingsClient;

  @InjectMocks
  private SettingsService service;

  @Test
  void shouldNotReturnAuthorityExpirationSeetingWhenNoSettingsExist() {
    when(settingsClient.getSettingsEntries(any(String.class), any(Integer.class)))
        .thenReturn(new SettingsClient.SettingsEntries(List.of(), null));

    var setting = service.getAuthorityExpireSetting();

    assertEquals(Optional.empty(), setting);
  }

  @Test
  void shouldThrowIntegrationExceptionWhenFetchingSettingsFailed() {
    when(settingsClient.getSettingsEntries(any(String.class), any(Integer.class)))
        .thenThrow(new RuntimeException());

    assertThrows(FolioIntegrationException.class, () -> service.getAuthorityExpireSetting());
  }

  @Test
  void shouldReturnAuthorityExpirationSeeting() {
    var settingValue = new SettingsClient.AuthoritiesExpirationSettingValue(true, 5);
    var settingEntry = new SettingsClient.SettingEntry(UUID.randomUUID(),
        SettingsService.AUTHORITIES_EXPIRE_SETTING_SCOPE, SettingsService.AUTHORITIES_EXPIRE_SETTING_KEY, settingValue);
    when(settingsClient.getSettingsEntries(any(String.class), any(Integer.class)))
        .thenReturn(new SettingsClient.SettingsEntries(List.of(settingEntry), new SettingsClient.ResultInfo(1)));

    var setting = service.getAuthorityExpireSetting();

    assertEquals(Optional.of(settingEntry), setting);
  }
}
