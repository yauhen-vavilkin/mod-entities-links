package org.folio.entlinks.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityNoteTypeMapper;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDto;
import org.folio.entlinks.domain.dto.AuthorityNoteTypeDtoCollection;
import org.folio.entlinks.service.authority.AuthorityNoteTypeService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityNoteTypeServiceDelegate {

  private final AuthorityNoteTypeService service;
  private final AuthorityNoteTypeMapper mapper;

  public AuthorityNoteTypeDtoCollection getAuthorityNoteTypes(Integer offset, Integer limit, String cqlQuery) {
    var noteTypes = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthorityNoteTypeCollection(noteTypes);
  }

  public AuthorityNoteTypeDto getAuthorityNoteTypeById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthorityNoteTypeDto createAuthorityNoteType(AuthorityNoteTypeDto authorityNoteTypeDto) {
    var created = service.create(mapper.toEntity(authorityNoteTypeDto));
    return mapper.toDto(created);
  }

  public void updateAuthorityNoteType(UUID id, AuthorityNoteTypeDto authorityNoteTypeDto) {
    var entity = mapper.toEntity(authorityNoteTypeDto);
    service.update(id, entity);
  }

  public void deleteAuthorityNoteTypeById(UUID id) {
    service.deleteById(id);
  }
}
