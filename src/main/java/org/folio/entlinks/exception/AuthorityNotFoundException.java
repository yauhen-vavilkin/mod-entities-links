package org.folio.entlinks.exception;

public class AuthorityNotFoundException extends ResourceNotFoundException {

  private static final String RESOURCE_NAME = "Authority";

  public AuthorityNotFoundException(Object id) {
    super(RESOURCE_NAME, id);
  }
}
