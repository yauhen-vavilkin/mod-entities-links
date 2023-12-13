package org.folio.entlinks.controller.delegate;

import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.CREATE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.DELETE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.UPDATE;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityServiceDelegate {

  private final AuthorityService service;
  private final AuthorityMapper mapper;
  private final FolioExecutionContext context;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final ConsortiumAuthorityPropagationService propagationService;

  public AuthorityDtoCollection retrieveAuthorityCollection(Integer offset, Integer limit, String cqlQuery) {
    var entities = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthorityCollection(entities);
  }

  public AuthorityDto getAuthorityById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthorityDto createAuthority(AuthorityDto authorityDto) {
    var entity = mapper.toEntity(authorityDto);
    var created = service.create(entity);
    var dto = mapper.toDto(created);
    eventPublisher.publishCreateEvent(dto);
    propagationService.propagate(entity, CREATE, context.getTenantId());
    return dto;
  }

  public void updateAuthority(UUID id, AuthorityDto authorityDto) {
    var modifiedEntity = mapper.toEntity(authorityDto);
    var oldDto = this.getAuthorityById(id);
    var updatedEntity = service.update(id, modifiedEntity);
    var newDto = mapper.toDto(updatedEntity);
    eventPublisher.publishUpdateEvent(oldDto, newDto);
    propagationService.propagate(updatedEntity, UPDATE, context.getTenantId());
  }

  public void deleteAuthorityById(UUID id) {
    var entity = service.getById(id);
    var dto = mapper.toDto(entity);
    service.deleteById(id);
    eventPublisher.publishSoftDeleteEvent(dto);
    propagationService.propagate(entity, DELETE, context.getTenantId());
  }
}
