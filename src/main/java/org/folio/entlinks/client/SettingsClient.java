package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("settings")
public interface SettingsClient {

  @GetMapping(value = "/entries", produces = APPLICATION_JSON_VALUE)
  SettingsEntries getSettingsEntries(@RequestParam("query") String query, @RequestParam("limit") int limit);

  record SettingsEntries(List<SettingEntry> items, ResultInfo resultInfo) {}

  record SettingEntry(UUID id, String scope, String key, AuthoritiesExpirationSettingValue value, UUID userId) {
    public SettingEntry(UUID id, String scope, String key, AuthoritiesExpirationSettingValue value) {
      this(id, scope, key, value, null);
    }
  }

  record AuthoritiesExpirationSettingValue(Boolean expirationEnabled, Integer retentionInDays) {}

  record ResultInfo(Integer totalRecords) {}
}
