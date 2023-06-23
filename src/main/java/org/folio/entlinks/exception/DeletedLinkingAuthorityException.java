package org.folio.entlinks.exception;

import java.util.List;
import java.util.Set;
import org.folio.tenant.domain.dto.Parameter;

public class DeletedLinkingAuthorityException extends RequestBodyValidationException {

  private static final String ERROR_MESSAGE = "Cannot save links to deleted authorities.";

  public DeletedLinkingAuthorityException(Set<String> deletedAuthorityIds) {
    super(ERROR_MESSAGE, mapToParameters(deletedAuthorityIds));
  }

  private static List<Parameter> mapToParameters(Set<String> deletedAuthorityIds) {
    return deletedAuthorityIds.stream()
      .map(authorityId -> new Parameter().key("authorityId").value(authorityId))
      .toList();
  }
}
