package org.folio.entlinks.integration;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class SettingsService {

  public static final String AUTHORITIES_EXPIRE_SETTING_KEY = "authority-archives-expiration";
  public static final String AUTHORITIES_EXPIRE_SETTING_SCOPE = "authority-storage";

  private static final String AUTHORITIES_EXPIRE_SETTING_FETCH_QUERY =
      "scope=authority-storage%20AND%20key=authority-archives-expiration";

  private static final int DEFAULT_REQUEST_LIMIT = 10000;

  private final SettingsClient settingsClient;

  public Optional<SettingsClient.SettingEntry> getAuthorityExpireSetting() {
    var settingsEntries = fetchSettingsEntries();

    if (settingsEntries == null || CollectionUtils.isEmpty(settingsEntries.items())) {
      return Optional.empty();
    }

    return settingsEntries.items().stream()
        .filter(entry -> entry.scope().equals(AUTHORITIES_EXPIRE_SETTING_SCOPE))
        .filter(entry -> entry.key().equals(AUTHORITIES_EXPIRE_SETTING_KEY))
        .findFirst();
  }

  private SettingsClient.SettingsEntries fetchSettingsEntries() {
    try {
      return settingsClient.getSettingsEntries(AUTHORITIES_EXPIRE_SETTING_FETCH_QUERY, DEFAULT_REQUEST_LIMIT);
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch settings", e);
    }
  }
}
