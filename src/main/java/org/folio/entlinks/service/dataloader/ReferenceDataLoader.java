package org.folio.entlinks.service.dataloader;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthorityNoteTypeRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.service.authority.AuthorityNoteTypeService;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReferenceDataLoader {

  private static final String BASE_DIR = "reference-data";
  private static final String AUTHORITY_NOTE_TYPES_DIR = "authority-note-types";
  private static final String AUTHORITY_SOURCE_FILES_DIR = "authority-source-files";

  private final PathMatchingResourcePatternResolver resourcePatternResolver =
    new PathMatchingResourcePatternResolver(ReferenceDataLoader.class.getClassLoader());

  private final AuthorityNoteTypeService noteTypeService;
  private final AuthoritySourceFileService sourceFileService;
  private final AuthorityNoteTypeRepository noteTypeRepository;
  private final AuthoritySourceFileRepository sourceFileRepository;
  private final ObjectMapper mapper;

  public void loadRefData() {
    try {
      log.info("Loading reference data");
      loadAuthorityNoteTypes();
      loadAuthoritySourceFiles();
    } catch (Exception e) {
      log.warn("Unable to load reference data", e);
      throw new IllegalStateException("Unable to load reference data", e);
    }
    log.info("Finished loading reference data");
  }

  private void loadAuthorityNoteTypes() {
    for (var res : getResources(AUTHORITY_NOTE_TYPES_DIR)) {
      AuthorityNoteType noteType = deserializeRecord(AuthorityNoteType.class, res);

      var existing = getExistingNoteType(noteType);

      if (Objects.isNull(existing)) {
        log.info("Creating reference Authority Note Type {}", noteType);
        noteTypeService.create(noteType);
      }
    }
  }

  private void loadAuthoritySourceFiles() {
    registerAuthoritySourceFileDeserializer();
    for (var res : getResources(AUTHORITY_SOURCE_FILES_DIR)) {
      AuthoritySourceFile sourceFile = deserializeRecord(AuthoritySourceFile.class, res);

      var existing = getExistingSourceFile(sourceFile);

      if (Objects.isNull(existing)) {
        log.info("Creating reference Authority Source File {}", sourceFile);
        sourceFileService.create(sourceFile);
      }
    }
  }

  private void registerAuthoritySourceFileDeserializer() {
    var module = new SimpleModule(
      "AuthoritySourceFileDeserializer",
      new Version(1, 0, 0, null, null, null));
    module.addDeserializer(AuthoritySourceFile.class, new SourceFileDeserializer());
    mapper.registerModule(module);
  }

  private <T> T deserializeRecord(Class<T> resourceType, Resource res) {
    try {
      return mapper.readValue(res.getInputStream(), resourceType);
    } catch (IOException e) {
      var msg = String.format("Failed to deserialize reference data of type %s from file: %s",
        resourceType, res.getFilename());
      log.warn(msg, e);
      throw new IllegalStateException(msg, e);
    }
  }

  private Resource[] getResources(String resourceDir) {
    try {
      return resourcePatternResolver.getResources(String.format("/%s/%s/%s", BASE_DIR, resourceDir, "*.json"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load reference data on path: " + resourceDir, e);
    }
  }

  private AuthorityNoteType getExistingNoteType(AuthorityNoteType noteType) {
    if (noteType == null) {
      return null;
    }

    var id = noteType.getId();
    if (id != null) {
      return noteTypeRepository.findById(id).orElse(null);
    }

    return noteTypeRepository.findByName(noteType.getName()).orElse(null);
  }

  private AuthoritySourceFile getExistingSourceFile(AuthoritySourceFile sourceFile) {
    if (sourceFile == null) {
      return null;
    }

    var id = sourceFile.getId();
    if (id != null) {
      return sourceFileRepository.findById(id).orElse(null);
    }

    return sourceFileRepository.findByName(sourceFile.getName()).orElse(null);
  }
}
