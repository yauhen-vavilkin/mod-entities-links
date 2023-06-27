package org.folio.entlinks.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthoritySourceFileServiceDelegate {

  private final AuthoritySourceFileService service;
  private final AuthoritySourceFileMapper mapper;

  public AuthoritySourceFileDtoCollection getAuthoritySourceFiles(Integer offset, Integer limit, String cqlQuery) {
    var entities = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthoritySourceFileCollection(entities);
  }

  public AuthoritySourceFileDto getAuthoritySourceFileById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthoritySourceFileDto createAuthoritySourceFile(AuthoritySourceFileDto authoritySourceFile) {
    var created = service.create(mapper.toEntity(authoritySourceFile));
    return mapper.toDto(created);
  }

  public void updateAuthoritySourceFile(UUID id, AuthoritySourceFileDto authoritySourceFile) {
    var entity = mapper.toEntity(authoritySourceFile);
    service.update(id, entity);
  }

  public void patchAuthoritySourceFile(UUID id, AuthoritySourceFilePatchDto partiallyModifiedDto) {
    log.info("patch:: Attempting to patch AuthoritySourceFile [id: {}, patchDto: {}]", id, partiallyModifiedDto);
    var existingEntity = service.getById(id);
    var partialEntityUpdate = new AuthoritySourceFile(existingEntity);
    partialEntityUpdate = mapper.partialUpdate(partiallyModifiedDto, partialEntityUpdate);
    service.update(id, partialEntityUpdate);
  }

  public void deleteAuthorityNoteTypeById(UUID id) {
    service.deleteById(id);
  }
}
