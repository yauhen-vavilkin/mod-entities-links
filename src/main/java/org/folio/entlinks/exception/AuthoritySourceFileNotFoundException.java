package org.folio.entlinks.exception;

public class AuthoritySourceFileNotFoundException extends ResourceNotFoundException {

  private static final String RESOURCE_NAME = "Authority Source File";

  public AuthoritySourceFileNotFoundException(Object id) {
    super(RESOURCE_NAME, id);
  }
}
