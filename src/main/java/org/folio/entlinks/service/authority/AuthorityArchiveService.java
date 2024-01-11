package org.folio.entlinks.service.authority;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.entity.projection.AuthorityIdDto;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class AuthorityArchiveService {

  private final AuthorityArchiveRepository repository;

  public Page<AuthorityArchive> findAll(Integer offset, Integer limit, String cqlQuery) {
    log.debug("getAll:: Attempts to find all AuthorityArchive by [offset: {}, limit: {}, cql: {}]", offset, limit,
        cqlQuery);

    if (StringUtils.isBlank(cqlQuery)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cqlQuery, new OffsetRequest(offset, limit));
  }

  public Page<AuthorityIdDto> findAllIds(Integer offset, Integer limit, String cqlQuery) {
    log.debug("getAll:: Attempts to find all AuthorityArchive IDs by [offset: {}, limit: {}, cql: {}]",
        offset, limit, cqlQuery);

    if (StringUtils.isBlank(cqlQuery)) {
      return repository.findAllIds(new OffsetRequest(offset, limit))
          .map(projection -> new AuthorityIdDto(projection.getId()));
    }

    return repository.findIdsByCql(cqlQuery, new OffsetRequest(offset, limit));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void delete(AuthorityArchive authorityArchive) {
    log.debug("Deleting authority archive: id = {}", authorityArchive.getId());
    repository.delete(authorityArchive);
  }
}
