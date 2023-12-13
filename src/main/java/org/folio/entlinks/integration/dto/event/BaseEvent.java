package org.folio.entlinks.integration.dto.event;

public interface BaseEvent {

  void setTenant(String tenant);

  void setTs(String ts);
}
