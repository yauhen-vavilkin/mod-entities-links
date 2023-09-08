package org.folio.entlinks.domain.repository;

import org.folio.entlinks.domain.entity.Authority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface AuthorityCqlRepository {

  static Specification<Authority> deletedIs(Boolean deleted) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Authority.DELETED_COLUMN), deleted);
  }

  Page<Authority> findByCqlAndDeletedFalse(String cql, Pageable pageable);
}
