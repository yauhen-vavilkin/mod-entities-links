package org.folio.entlinks.service.authority;

import static org.folio.entlinks.utils.ServiceUtils.initId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.projection.AuthorityIdDto;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.data.OffsetRequest;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class AuthorityService {

  private final AuthorityRepository repository;
  private final AuthoritySourceFileRepository sourceFileRepository;

  public Page<Authority> getAll(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all Authority by [offset: {}, limit: {}, cql: {}]", offset, limit,
      cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAllByDeletedFalse(new OffsetRequest(offset, limit));
    }

    return repository.findByCqlAndDeletedFalse(cql, new OffsetRequest(offset, limit));
  }

  public Page<AuthorityIdDto> getAllIds(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all Authority IDs by [offset: {}, limit: {}, cql: {}]",
        offset, limit, cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAllIdsByDeletedFalse(new OffsetRequest(offset, limit))
          .map(projection -> new AuthorityIdDto(projection.getId()));
    }

    return repository.findIdsByCqlAndDeletedFalse(cql, new OffsetRequest(offset, limit));
  }

  public Authority getById(UUID id) {
    log.debug("getById:: Loading Authority by ID [id: {}]", id);

    return repository.findByIdAndDeletedFalse(id).orElseThrow(() -> new AuthorityNotFoundException(id));
  }

  public Map<UUID, Authority> getAllByIds(Collection<UUID> ids) {
    return repository.findAllByIdInAndDeletedFalse(ids).stream()
      .collect(Collectors.toMap(Authority::getId, Function.identity()));
  }

  @Transactional
  public Authority create(Authority entity) {
    log.debug("create:: Attempting to create Authority [entity: {}]", entity);
    validateSourceFile(entity);
    initId(entity);
    return repository.save(entity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(
    retryFor = OptimisticLockingException.class,
    maxAttempts = 2,
    backoff = @Backoff(delay = 500))
  public Authority update(UUID id, Authority modified) {
    log.debug("update:: Attempting to update Authority [authority: {}]", modified);

    if (!Objects.equals(id, modified.getId())) {
      throw new RequestBodyValidationException("Request should have id = " + id,
        List.of(new Parameter("id").value(String.valueOf(modified.getId()))));
    }

    var existing = repository.findByIdAndDeletedFalse(id).orElseThrow(() -> new AuthorityNotFoundException(id));
    if (modified.getVersion() < existing.getVersion()) {
      throw OptimisticLockingException.optimisticLockingOnUpdate(id, existing.getVersion(), modified.getVersion());
    }

    validateSourceFile(modified);
    copyModifiableFields(existing, modified);

    return repository.save(existing);
  }

  /**
   * Performs soft-delete of {@link Authority} records.
   *
   * @param id authority record id of {@link UUID} type
   */
  @Transactional
  public void deleteById(UUID id) {
    log.debug("deleteById:: Attempt to delete Authority by [id: {}]", id);

    var authority = repository.findByIdAndDeletedFalse(id)
      .orElseThrow(() -> new AuthorityNotFoundException(id));
    authority.setDeleted(true);

    repository.save(authority);
  }

  /**
   * Performs hard-delete of {@link Authority} records.
   *
   * @param ids collection of authority record ids of {@link UUID} type
   */
  @Transactional
  public void batchDeleteByIds(Collection<UUID> ids) {
    repository.deleteAllByIdInBatch(ids);
  }

  private void copyModifiableFields(Authority existing, Authority modified) {
    existing.setHeading(modified.getHeading());
    existing.setHeadingType(modified.getHeadingType());
    existing.setSource(modified.getSource());
    existing.setNaturalId(modified.getNaturalId());
    existing.setSubjectHeadingCode(modified.getSubjectHeadingCode());
    existing.setSftHeadings(modified.getSftHeadings());
    existing.setSaftHeadings(modified.getSaftHeadings());
    existing.setIdentifiers(modified.getIdentifiers());
    existing.setNotes(modified.getNotes());
    existing.setVersion(existing.getVersion() + 1);

    Optional.ofNullable(modified.getAuthoritySourceFile())
      .map(AuthoritySourceFile::getId)
      .ifPresentOrElse(sourceFileId -> {
        var sourceFile = new AuthoritySourceFile();
        sourceFile.setId(sourceFileId);
        existing.setAuthoritySourceFile(sourceFile);
      }, () -> existing.setAuthoritySourceFile(null));
  }

  private void validateSourceFile(Authority authority) {
    if (authority.getAuthoritySourceFile() != null) {
      var id = authority.getAuthoritySourceFile().getId();
      if (id != null && !sourceFileRepository.existsById(id)) {
        throw new AuthoritySourceFileNotFoundException(id);
      }
    }
  }
}
