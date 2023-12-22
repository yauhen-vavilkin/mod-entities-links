package org.folio.entlinks.service.authority;

import static org.folio.entlinks.domain.entity.AuthoritySourceType.FOLIO;
import static org.folio.entlinks.utils.ServiceUtils.initId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceType;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.data.OffsetRequest;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class AuthoritySourceFileService {

  private static final String AUTHORITY_SEQUENCE_NAME_TEMPLATE = "hrid_authority_local_file_%s_seq";
  private final AuthoritySourceFileRepository repository;
  private final AuthoritySourceFileMapper mapper;
  private final JdbcTemplate jdbcTemplate;
  private final FolioModuleMetadata moduleMetadata;
  private final FolioExecutionContext folioExecutionContext;

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

  public AuthoritySourceFile getByName(String name) {
    log.debug("getById:: Loading AuthoritySourceFile by Name [name: {}]", name);

    if (StringUtils.isBlank(name)) {
      return null;
    }

    return repository.findByName(name).orElse(null);
  }

  @Transactional
  public AuthoritySourceFile create(AuthoritySourceFile entity) {
    log.debug("create:: Attempting to create AuthoritySourceFile [entity: {}]", entity);

    validateOnCreate(entity);

    initOnCreate(entity);

    return repository.save(entity);
  }

  public AuthoritySourceFile update(@Nonnull UUID id, AuthoritySourceFile modified) {
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
    var authoritySourceFile = repository.findById(id)
        .orElseThrow(() -> new AuthoritySourceFileNotFoundException(id));
    if (!FOLIO.getValue().equals(authoritySourceFile.getSource())) {
      repository.deleteById(id);
    } else {
      throw new RequestBodyValidationException("Cannot delete Authority source file with source 'folio'",
          List.of(new Parameter("source").value(authoritySourceFile.getSource())));
    }
  }

  public long getNextSequenceNumber(String sequenceName) {
    return repository.getNextSequenceNumber(sequenceName);
  }

  public void createSequence(String sequenceName, int startNumber) {
    var command = String.format("""
        CREATE SEQUENCE %s MINVALUE %d INCREMENT BY 1 OWNED BY %s.authority_source_file.sequence_name
        """,
        sequenceName, startNumber, moduleMetadata.getDBSchemaName(folioExecutionContext.getTenantId()));
    jdbcTemplate.execute(command);
  }

  private void validateOnCreate(AuthoritySourceFile entity) {
    if (!AuthoritySourceType.LOCAL.getValue().equals(entity.getSource())) {
      throw new RequestBodyValidationException("Only Authority Source File with source Local can be created",
          List.of(new Parameter("source").value(entity.getSource())));
    }

    if (entity.getAuthoritySourceFileCodes().size() != 1) {
      var codes = entity.getAuthoritySourceFileCodes().stream()
          .map(AuthoritySourceFileCode::getCode)
          .collect(Collectors.joining(","));
      throw new RequestBodyValidationException("Authority Source File with source Local should have only one prefix",
          List.of(new Parameter("code").value(codes)));
    }

    var code = entity.getAuthoritySourceFileCodes().iterator().next().getCode();
    if (StringUtils.isBlank(code) || !StringUtils.isAlpha(code)) {
      throw new RequestBodyValidationException("Authority Source File prefix should be non-empty sequence of letters",
          List.of(new Parameter("code").value(code)));
    }
  }

  private void initOnCreate(AuthoritySourceFile entity) {
    initId(entity);

    var sourceFileCode = entity.getAuthoritySourceFileCodes().iterator().next();
    sourceFileCode.setAuthoritySourceFile(entity);
    var sequenceName = String.format(AUTHORITY_SEQUENCE_NAME_TEMPLATE, sourceFileCode.getCode());
    entity.setSequenceName(sequenceName);

    if (entity.getHridStartNumber() == null) {
      entity.setHridStartNumber(1);
    }
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
