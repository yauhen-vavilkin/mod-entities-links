package org.folio.entlinks.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthoritySourceFileServiceDelegate {

  private static final String URL_PROTOCOL_PATTERN = "^(https?://www\\.|https?://|www\\.)";

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

  public AuthoritySourceFileDto createAuthoritySourceFile(AuthoritySourceFilePostDto authoritySourceFile) {
    var entity = mapper.toEntity(authoritySourceFile);
    normalizeBaseUrl(entity);
    var created = service.create(entity);

    service.createSequence(created.getSequenceName(), created.getHridStartNumber());

    return mapper.toDto(created);
  }

  public void patchAuthoritySourceFile(UUID id, AuthoritySourceFilePatchDto partiallyModifiedDto) {
    log.debug("patch:: Attempting to patch AuthoritySourceFile [id: {}, patchDto: {}]", id, partiallyModifiedDto);
    var existingEntity = service.getById(id);
    var partialEntityUpdate = new AuthoritySourceFile(existingEntity);
    partialEntityUpdate = mapper.partialUpdate(partiallyModifiedDto, partialEntityUpdate);
    normalizeBaseUrl(partialEntityUpdate);
    var patched = service.update(id, partialEntityUpdate);
    log.debug("patch:: Authority Source File partially updated: {}", patched);
  }

  public void deleteAuthorityNoteTypeById(UUID id) {
    service.deleteById(id);
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
}
