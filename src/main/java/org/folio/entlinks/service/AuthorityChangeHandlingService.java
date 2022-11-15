package org.folio.entlinks.service;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.qm.domain.dto.InventoryEvent;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AuthorityChangeHandlingService {

  public int handleAuthoritiesChanges(List<InventoryEvent> events) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
