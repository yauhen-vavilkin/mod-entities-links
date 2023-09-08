package org.folio.entlinks.domain.entity.base;

public interface Identifiable<T> {

  T getId();

  void setId(T id);

}
