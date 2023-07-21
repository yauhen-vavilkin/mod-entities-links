package org.folio.entlinks.service.authority;

import static org.folio.entlinks.utils.ServiceUtils.initId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.data.OffsetRequest;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class AuthoritySourceFileService {

  private final AuthoritySourceFileRepository repository;
  private final AuthoritySourceFileMapper mapper;

  public Page<AuthoritySourceFile> getAll(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all AuthoritySourceFile by [offset: {}, limit: {}, cql: {}]", offset, limit,
      cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cql, new OffsetRequest(offset, limit));
  }

  public AuthoritySourceFile getById(UUID id) {
    log.debug("getById:: Loading AuthoritySourceFile by ID [id: {}]", id);

    return repository.findById(id).orElseThrow(() -> new AuthoritySourceFileNotFoundException(id));
  }

  public AuthoritySourceFile create(AuthoritySourceFile entity) {
    log.debug("create:: Attempting to create AuthoritySourceFile [entity: {}]", entity);

    initId(entity);

    for (var code : entity.getAuthoritySourceFileCodes()) {
      code.setAuthoritySourceFile(entity);
    }

    return repository.save(entity);
  }

  public AuthoritySourceFile update(UUID id, AuthoritySourceFile modified) {
    log.debug("update:: Attempting to update AuthoritySourceFile [id: {}]", id);

    if (!Objects.equals(id, modified.getId())) {
      throw new RequestBodyValidationException("Request should have id = " + id,
        List.of(new Parameter("id").value(String.valueOf(modified.getId()))));
    }

    var existingEntity = repository.findById(id).orElseThrow(() -> new AuthoritySourceFileNotFoundException(id));

    copyModifiableFields(existingEntity, modified);

    return repository.save(existingEntity);
  }

  public void deleteById(UUID id) {
    log.debug("deleteById:: Attempt to delete AuthoritySourceFile by [id: {}]", id);

    if (!repository.existsById(id)) {
      throw new AuthoritySourceFileNotFoundException(id);
    }

    repository.deleteById(id);
  }

  private void copyModifiableFields(AuthoritySourceFile existingEntity, AuthoritySourceFile modifiedEntity) {
    existingEntity.setName(modifiedEntity.getName());
    existingEntity.setBaseUrl(modifiedEntity.getBaseUrl());
    existingEntity.setSource(modifiedEntity.getSource());
    existingEntity.setType(modifiedEntity.getType());
    var existingCodes = mapper.toDtoCodes(existingEntity.getAuthoritySourceFileCodes());
    var modifiedCodes = mapper.toDtoCodes(modifiedEntity.getAuthoritySourceFileCodes());
    for (var code : modifiedEntity.getAuthoritySourceFileCodes()) {
      if (!existingCodes.contains(code.getCode())) {
        existingEntity.addCode(code);
      }
    }
    var iterator = existingEntity.getAuthoritySourceFileCodes().iterator();
    while (iterator.hasNext()) {
      var sourceFileCode = iterator.next();
      if (!modifiedCodes.contains(sourceFileCode.getCode())) {
        sourceFileCode.setAuthoritySourceFile(null);
        iterator.remove();
      }
    }
  }
}
