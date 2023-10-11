package org.folio.entlinks.exception;

import java.util.UUID;

public final class OptimisticLockingException extends RuntimeException {

  private OptimisticLockingException(String errorMessage) {
    super(errorMessage);
  }

  public static OptimisticLockingException optimisticLockingOnUpdate(UUID recordId,
                                                                     int existingVersion,
                                                                     int requestVersion) {
    var errorMessage = String.format("Cannot update record %s because it has been changed (optimistic locking): "
        + "Stored _version is %d, _version of request is %d",
        recordId.toString(), existingVersion, requestVersion);
    return new OptimisticLockingException(errorMessage);
  }
}
