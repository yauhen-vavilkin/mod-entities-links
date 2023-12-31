package org.folio.entlinks.service.dataloader;

import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.FOLIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Iterator;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class SourceFileDeserializerTest {

  public static final String BASE_URL = "vocab.getty.edu/aat/";

  @Mock
  private JsonParser jsonParser;

  @Mock
  private DeserializationContext deserializationContext;

  @Test
  void testDeserialize_folio_type() throws IOException {
    var sourceNode = mock(JsonNode.class);

    setUpCommonMockBehaviors(sourceNode);
    when(sourceNode.asText()).thenReturn("folio");

    var deserializer = new SourceFileDeserializer();
    var sourceFile = deserializer.deserialize(jsonParser, deserializationContext);

    assertEquals(BASE_URL, sourceFile.getBaseUrl());
    assertEquals(FOLIO, sourceFile.getSource());
  }

  private void setUpCommonMockBehaviors(JsonNode sourceNode) throws IOException {
    var codec = mock(ObjectCodec.class);
    var codesNode = mock(JsonNode.class);
    var rootNode = mock(JsonNode.class);
    var baseUrlNode = mock(JsonNode.class);

    when(jsonParser.getCodec()).thenReturn(codec);
    when(codec.readTree(jsonParser)).thenReturn(rootNode);

    when(rootNode.get("id")).thenReturn(mock(JsonNode.class));
    when(rootNode.get("type")).thenReturn(mock(JsonNode.class));
    when(rootNode.get("name")).thenReturn(mock(JsonNode.class));
    when(rootNode.get("codes")).thenReturn(codesNode);
    when(codesNode.elements()).thenReturn(mock(Iterator.class));
    when(rootNode.get("baseUrl")).thenReturn(baseUrlNode);
    when(rootNode.get("source")).thenReturn(sourceNode);
    when(baseUrlNode.asText()).thenReturn(BASE_URL);

  }
}
