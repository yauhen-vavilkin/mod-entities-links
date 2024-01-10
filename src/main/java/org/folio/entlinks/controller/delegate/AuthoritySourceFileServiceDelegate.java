package org.folio.entlinks.controller.delegate;

import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.CREATE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.DELETE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.UPDATE;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFileHridDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.dto.event.DomainEventType;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthoritySourceFileServiceDelegate {

  private static final String URL_PROTOCOL_PATTERN = "^(https?://www\\.|https?://|www\\.)";
  private static final String CREATE_ACTION = "create";
  private static final String NEXT_HRID_ACTION = "next HRID";

  private final AuthoritySourceFileService service;
  private final AuthoritySourceFileMapper mapper;
  private final ConsortiumTenantsService tenantsService;
  private final ConsortiumPropagationService<AuthoritySourceFile> propagationService;
  private final FolioExecutionContext context;

  public AuthoritySourceFileDtoCollection getAuthoritySourceFiles(Integer offset, Integer limit, String cqlQuery) {
    var entities = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthoritySourceFileCollection(entities);
  }

  public AuthoritySourceFileDto getAuthoritySourceFileById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthoritySourceFileDto createAuthoritySourceFile(AuthoritySourceFilePostDto authoritySourceFile) {
    log.debug("create:: Attempting to create AuthoritySourceFile [createDto: {}]", authoritySourceFile);
    validateActionRightsForTenant(CREATE_ACTION);
    var entity = mapper.toEntity(authoritySourceFile);
    normalizeBaseUrl(entity);
    var created = service.create(entity);

    service.createSequence(created.getSequenceName(), created.getHridStartNumber());

    propagationService.propagate(entity, CREATE, context.getTenantId());
    return mapper.toDto(created);
  }

  public void patchAuthoritySourceFile(UUID id, AuthoritySourceFilePatchDto partiallyModifiedDto) {
    log.debug("patch:: Attempting to patch AuthoritySourceFile [id: {}, patchDto: {}]", id, partiallyModifiedDto);
    var existingEntity = service.getById(id);
    validateModifyPossibility(DomainEventType.UPDATE, existingEntity);
    var partialEntityUpdate = new AuthoritySourceFile(existingEntity);
    partialEntityUpdate = mapper.partialUpdate(partiallyModifiedDto, partialEntityUpdate);
    normalizeBaseUrl(partialEntityUpdate);
    var patched = service.update(id, partialEntityUpdate);
    log.debug("patch:: Authority Source File partially updated: {}", patched);
    propagationService.propagate(patched, UPDATE, context.getTenantId());
  }

  public void deleteAuthoritySourceFileById(UUID id) {
    var entity = service.getById(id);
    validateModifyPossibility(DomainEventType.DELETE, entity);

    service.deleteById(id);
    propagationService.propagate(entity, DELETE, context.getTenantId());
  }

  public AuthoritySourceFileHridDto getAuthoritySourceFileNextHrid(UUID id) {
    log.debug("nextHrid:: Attempting to get next AuthoritySourceFile HRID [id: {}]", id);
    validateActionRightsForTenant(NEXT_HRID_ACTION);

    var hrid = service.nextHrid(id);

    return new AuthoritySourceFileHridDto().id(id).hrid(hrid);
  }

  private void normalizeBaseUrl(AuthoritySourceFile entity) {
    var baseUrl = entity.getBaseUrl();
    if (StringUtils.isNotBlank(baseUrl)) {
      baseUrl = baseUrl.replaceFirst(URL_PROTOCOL_PATTERN, "");
      if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      entity.setBaseUrl(baseUrl);
    }
  }

  private void validateActionRightsForTenant(String action) {
    var tenantId = context.getTenantId();
    if (tenantsService.getConsortiumTenants(tenantId).contains(tenantId)) {
      throw new RequestBodyValidationException("Action '%s' is not supported for consortium member tenant"
        .formatted(action), List.of(new Parameter("tenantId").value(tenantId)));
    }
  }

  private void validateModifyPossibility(DomainEventType eventType, AuthoritySourceFile entity) {
    if (entity.isConsortiumShadowCopy()) {
      throw new RequestBodyValidationException(eventType.name() + " is not applicable to consortium shadow copy",
        List.of(new Parameter("id").value(String.valueOf(entity.getId()))));
    }
  }
}
