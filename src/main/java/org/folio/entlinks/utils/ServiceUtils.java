package org.folio.entlinks.utils;

import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.entlinks.domain.entity.base.Identifiable;

@UtilityClass
public class ServiceUtils {

  public static <E extends Identifiable<UUID>> void initId(E identifiable) {
    if (identifiable.getId() == null) {
      identifiable.setId(UUID.randomUUID());
    }
  }
}
