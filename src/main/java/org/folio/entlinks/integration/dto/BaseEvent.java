package org.folio.entlinks.integration.dto;

public interface BaseEvent {

  void setTenant(String tenant);

  void setTs(String ts);
}
