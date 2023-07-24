package org.folio.entlinks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.service.authority.AuthorityNoteTypeService;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;


@UnitTest
@ExtendWith(MockitoExtension.class)
class ReferenceDataLoaderTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AuthorityNoteTypeService noteTypeService = mock(AuthorityNoteTypeService.class);

  private final AuthoritySourceFileService sourceFileService = mock(AuthoritySourceFileService.class);

  private final ReferenceDataLoader referenceDataLoader =
      new ReferenceDataLoader(noteTypeService, sourceFileService, OBJECT_MAPPER);

  @BeforeAll
  static void setUp() {
    var module = new SimpleModule(
        "CustomDeserializer",
        new Version(1, 0, 0, null, null, null));
    module.addDeserializer(AuthoritySourceFile.class, new CustomAuthoritySourceFileDeserializer());
    OBJECT_MAPPER.registerModule(module);
  }

  @Test
  void shouldLoadReferenceData() {
    when(noteTypeService.getById(any(UUID.class))).thenReturn(null);
    when(noteTypeService.create(any(AuthorityNoteType.class))).thenAnswer(i -> i.getArguments()[0]);
    when(sourceFileService.getById(any(UUID.class))).thenReturn(null);
    when(sourceFileService.create(any(AuthoritySourceFile.class))).thenAnswer(i -> i.getArguments()[0]);

    referenceDataLoader.loadRefData();

    verify(noteTypeService).getById(any(UUID.class));
    verify(sourceFileService).getById(any(UUID.class));

    var noteTypeCaptor = ArgumentCaptor.forClass(AuthorityNoteType.class);
    var sourceFileCaptor = ArgumentCaptor.forClass(AuthoritySourceFile.class);
    verify(noteTypeService).create(noteTypeCaptor.capture());
    verify(sourceFileService).create(sourceFileCaptor.capture());

    var loadedNoteType = noteTypeCaptor.getValue();
    assertNotNull(loadedNoteType.getId());
    assertEquals("76c74801-afec-45a0-aad7-3ff23591e147", loadedNoteType.getId().toString());
    assertEquals("general note", loadedNoteType.getName());
    assertEquals("folio", loadedNoteType.getSource());

    var loadedSourceFile = sourceFileCaptor.getValue();
    assertNotNull(loadedSourceFile.getId());
    assertEquals("cb58492d-018e-442d-9ce3-35aabfc524aa", loadedSourceFile.getId().toString());
    assertEquals("Art & architecture thesaurus (AAT)", loadedSourceFile.getName());
    assertThat(loadedSourceFile.getAuthoritySourceFileCodes()).hasSize(1);
    assertEquals("aat", loadedSourceFile.getAuthoritySourceFileCodes().iterator().next().getCode());
    assertEquals("Subjects", loadedSourceFile.getType());
    assertEquals("vocab.getty.edu/aat/", loadedSourceFile.getBaseUrl());
    assertEquals("folio", loadedSourceFile.getSource());
  }

  static class CustomAuthoritySourceFileDeserializer extends StdDeserializer<AuthoritySourceFile> {

    CustomAuthoritySourceFileDeserializer() {
      this(null);
    }

    CustomAuthoritySourceFileDeserializer(Class<?> vc) {
      super(vc);
    }

    @SneakyThrows
    @Override
    public AuthoritySourceFile deserialize(JsonParser parser, DeserializationContext deserializer) {
      var sourceFile = new AuthoritySourceFile();
      ObjectCodec codec = parser.getCodec();
      JsonNode node = codec.readTree(parser);

      // get source file codes
      var iterator = node.get("codes").elements();
      while (iterator.hasNext()) {
        var codeNode = iterator.next();
        var code = codeNode.asText();
        var sourceFileCode = new AuthoritySourceFileCode();
        sourceFileCode.setCode(code);
        sourceFile.addCode(sourceFileCode);
      }
      var id = node.get("id").asText();
      var type = node.get("type").asText();
      var name = node.get("name").asText();
      var url = node.get("baseUrl").asText();
      var source = node.get("source").asText();

      sourceFile.setId(UUID.fromString(id));
      sourceFile.setName(name);
      sourceFile.setType(type);
      sourceFile.setBaseUrl(url);
      sourceFile.setSource(source);

      return sourceFile;
    }
  }
}
