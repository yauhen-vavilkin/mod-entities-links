package org.folio.entlinks.domain.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.spring.cql.Cql2JpaCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;

public class AuthorityCqlRepositoryImpl implements AuthorityCqlRepository {

  private final EntityManager em;
  private final Cql2JpaCriteria<Authority> cql2JpaCriteria;

  public AuthorityCqlRepositoryImpl(EntityManager em) {
    this.em = em;
    this.cql2JpaCriteria = new Cql2JpaCriteria<>(Authority.class, em);
  }

  @Override
  public Page<Authority> findByCqlAndDeletedFalse(String cqlQuery, Pageable pageable) {

    var collectBy = collectByQueryAndDeletedFalse(cqlQuery);
    var countBy = countByQueryAndDeletedFalse(cqlQuery);
    var criteria = cql2JpaCriteria.toCollectCriteria(collectBy);

    List<Authority> resultList = em
        .createQuery(criteria)
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(countBy));
  }

  private long count(Specification<Authority> specification) {
    var criteria = cql2JpaCriteria.toCountCriteria(specification);
    return em.createQuery(criteria).getSingleResult();
  }

  private Specification<Authority> collectByQueryAndDeletedFalse(String cqlQuery) {
    return AuthorityCqlRepository.deletedIs(false).and(cql2JpaCriteria.createCollectSpecification(cqlQuery));
  }

  private Specification<Authority> countByQueryAndDeletedFalse(String cqlQuery) {
    return AuthorityCqlRepository.deletedIs(false).and(cql2JpaCriteria.createCountSpecification(cqlQuery));
  }
}
