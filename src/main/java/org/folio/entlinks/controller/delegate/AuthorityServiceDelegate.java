package org.folio.entlinks.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityServiceDelegate {

  private final AuthorityService service;
  private final AuthorityMapper mapper;
  private final AuthorityDomainEventPublisher eventPublisher;

  public AuthorityDtoCollection retrieveAuthorityCollection(Integer offset, Integer limit, String cqlQuery) {
    var entities = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthorityCollection(entities);
  }

  public AuthorityDto getAuthorityById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthorityDto createAuthority(AuthorityDto authorityDto) {
    var created = service.create(mapper.toEntity(authorityDto));
    var dto = mapper.toDto(created);
    eventPublisher.publishCreateEvent(dto);
    return dto;
  }

  public void updateAuthority(UUID id, AuthorityDto authorityDto) {
    var modifiedEntity = mapper.toEntity(authorityDto);
    var existingEntity = service.getById(id);
    var updatedEntity = service.update(id, modifiedEntity);
    eventPublisher.publishUpdateEvent(mapper.toDto(existingEntity), mapper.toDto(updatedEntity));
  }

  public void deleteAuthorityById(UUID id) {
    var entity = service.getById(id);
    var dto = mapper.toDto(entity);
    service.deleteById(id);
    eventPublisher.publishDeleteEvent(dto);
  }
}
