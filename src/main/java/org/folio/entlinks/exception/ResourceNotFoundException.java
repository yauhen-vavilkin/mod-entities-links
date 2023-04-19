package org.folio.entlinks.exception;

public abstract class ResourceNotFoundException extends RuntimeException {

  private static final String NOT_FOUND_MSG_TEMPLATE = "%s with ID [%s] was not found";

  protected ResourceNotFoundException(String resourceName, Object id) {
    super(String.format(NOT_FOUND_MSG_TEMPLATE, resourceName, id));
  }
}
