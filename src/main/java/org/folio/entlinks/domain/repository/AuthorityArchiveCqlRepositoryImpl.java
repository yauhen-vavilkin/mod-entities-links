package org.folio.entlinks.domain.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.domain.entity.projection.AuthorityIdDto;
import org.folio.spring.cql.Cql2JpaCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;

public class AuthorityArchiveCqlRepositoryImpl implements AuthorityArchiveCqlRepository {

  private final EntityManager em;
  private final Cql2JpaCriteria<AuthorityArchive> cql2JpaCriteria;

  public AuthorityArchiveCqlRepositoryImpl(EntityManager em) {
    this.em = em;
    this.cql2JpaCriteria = new Cql2JpaCriteria<>(AuthorityArchive.class, em);
  }

  @Override
  public Page<AuthorityIdDto> findIdsByCql(String cql, Pageable pageable) {
    var collectBy = cql2JpaCriteria.createCollectSpecification(cql);
    var countBy = cql2JpaCriteria.createCountSpecification(cql);

    var cb = em.getCriteriaBuilder();
    var query = cb.createQuery(AuthorityIdDto.class);
    var root = query.from(AuthorityArchive.class);

    query.select(cb.construct(AuthorityIdDto.class, root.get(AuthorityBase.ID_COLUMN)));
    query.where(collectBy.toPredicate(root, query, cb));

    List<AuthorityIdDto> resultList = em
        .createQuery(query)
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(countBy));
  }

  private long count(Specification<AuthorityArchive> specification) {
    var criteria = cql2JpaCriteria.toCountCriteria(specification);
    return em.createQuery(criteria).getSingleResult();
  }
}
