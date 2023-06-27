package org.folio.entlinks.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.service.authority.AuthorityService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityServiceDelegate {

  private final AuthorityService service;
  private final AuthorityMapper mapper;

  public AuthorityDtoCollection retrieveAuthorityCollection(Integer offset, Integer limit, String cqlQuery) {
    var entities = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthoritySourceFileCollection(entities);
  }

  public AuthorityDto getAuthorityStorageById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthorityDto createAuthorityStorage(AuthorityDto authorityDto) {
    var created = service.create(mapper.toEntity(authorityDto));
    return mapper.toDto(created);
  }

  public void updateAuthorityStorage(UUID id, AuthorityDto authorityDto) {
    var entity = mapper.toEntity(authorityDto);
    service.update(id, entity);
  }

  public void deleteAuthorityStorageById(UUID id) {
    service.deleteById(id);
  }
}
