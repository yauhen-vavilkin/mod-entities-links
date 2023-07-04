package org.folio.entlinks.exception;

public class ReindexJobNotFoundException extends ResourceNotFoundException {

  private static final String RESOURCE_NAME = "Reindex job";

  public ReindexJobNotFoundException(Object id) {
    super(RESOURCE_NAME, id);
  }
}
