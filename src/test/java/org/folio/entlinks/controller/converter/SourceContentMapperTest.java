package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;

import java.util.List;
import java.util.Map;
import org.folio.entlinks.domain.dto.ExternalIdsHolder;
import org.folio.entlinks.domain.dto.FieldContent;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecordParsedRecord;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.spring.test.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@UnitTest
class SourceContentMapperTest {

  private final SourceContentMapper mapper = new SourceContentMapperImpl();

  @Test
  void testConvertToParsedContentCollection() {
    SourceParsedContent content = createSourceParsedContent();

    var listOfContent = List.of(content);

    ParsedRecordContentCollection contentCollection = mapper.convertToParsedContentCollection(listOfContent);

    assertThat(contentCollection.getRecords()).hasSize(1);
    assertThat(contentCollection.getRecords().get(0).getLeader()).isEqualTo(content.getLeader());
    FieldContent tag = contentCollection.getRecords().get(0).getFields().get(0).get("tag");
    assertThat(tag.getInd1()).isEqualTo(content.getFields().get(0).getInd1());
    assertThat(tag.getInd2()).isEqualTo(content.getFields().get(0).getInd2());
    var subFieldValue = content.getFields().get(0).getSubfields().get("a").get(0);
    assertThat(tag.getSubfields().get(0)).containsEntry("a", subFieldValue);

  }

  @Test
  void testConvertToParsedContent() {
    ParsedRecordContent content = createParsedRecordContent();

    SourceParsedContent result = mapper.convertToParsedContent(content);

    assertThat(result.getLeader()).isEqualTo(content.getLeader());
    FieldParsedContent parsedContent = result.getFields().get(0);
    assertThat(parsedContent.getInd1()).isEqualTo(content.getFields().get(0).get("tag").getInd1());
    assertThat(parsedContent.getInd2()).isEqualTo(content.getFields().get(0).get("tag").getInd2());
    assertThat(parsedContent.getLinkDetails()).isEqualTo(content.getFields().get(0).get("tag").getLinkDetails());
  }

  @Test
  void testConvertToAuthorityParsedContent() {
    var recordCollection = new StrippedParsedRecordCollection();
    StrippedParsedRecord strippedParsedRecord = createStrippedParsedRecord();
    recordCollection.getRecords().add(strippedParsedRecord);

    var authority = Authority.builder().id(TEST_ID).source(TEST_PROPERTY_VALUE).naturalId(TEST_ID.toString()).build();
    var authorities = List.of(authority);

    List<AuthorityParsedContent> resultList = mapper.convertToAuthorityParsedContent(recordCollection, authorities);

    var authorityParsedContent = resultList.get(0);
    var parsedContent = authorityParsedContent.getFields().get(0);
    var field = strippedParsedRecord.getParsedRecord().getContent().getFields().get(0);
    assertThat(resultList).hasSize(1);
    assertThat(authorityParsedContent.getId()).isEqualTo(authorities.get(0).getId());
    assertThat(authorityParsedContent.getNaturalId()).isEqualTo(authorities.get(0).getNaturalId());
    assertThat(parsedContent.getInd1()).isEqualTo(field.get("tag").getInd1());
    assertThat(parsedContent.getInd2()).isEqualTo(field.get("tag").getInd2());
    assertThat(parsedContent.getLinkDetails()).isEqualTo(field.get("tag").getLinkDetails());
    assertThat(authorityParsedContent.getLeader())
        .isEqualTo(strippedParsedRecord.getParsedRecord().getContent().getLeader());
    assertThat(parsedContent.getSubfields().get("a").get(0))
        .isEqualTo(field.get("tag").getSubfields().get(0).get("a"));
  }


  @Test
  void testConvertToParsedContent_ContentCollection() {
    ParsedRecordContent record = createParsedRecordContent();
    var contentCollection = new ParsedRecordContentCollection();
    contentCollection.setRecords(List.of(record));

    List<SourceParsedContent> resultList = mapper.convertToParsedContent(contentCollection);

    FieldParsedContent parsedContent = resultList.get(0).getFields().get(0);
    Map<String, FieldContent> contentMap = record.getFields().get(0);
    assertThat(resultList).hasSize(1);
    assertThat(resultList.get(0).getLeader()).isEqualTo(record.getLeader());
    assertThat(parsedContent.getInd1()).isEqualTo(contentMap.get("tag").getInd1());
    assertThat(parsedContent.getInd2()).isEqualTo(contentMap.get("tag").getInd2());
    assertThat(parsedContent.getLinkDetails()).isEqualTo(contentMap.get("tag").getLinkDetails());
    assertThat(parsedContent.getSubfields().get("a").get(0))
        .isEqualTo(contentMap.get("tag").getSubfields().get(0).get("a"));

  }

  @NotNull
  private static StrippedParsedRecord createStrippedParsedRecord() {
    var strippedParsedRecord = new StrippedParsedRecord();
    var externalIdsHolder = new ExternalIdsHolder();
    externalIdsHolder.setAuthorityId(TEST_ID);
    strippedParsedRecord.setExternalIdsHolder(externalIdsHolder);
    var parsedRecord = new StrippedParsedRecordParsedRecord();
    var parsedRecordContent = createParsedRecordContent();
    parsedRecordContent.setLeader(TEST_PROPERTY_VALUE);
    parsedRecord.setContent(createParsedRecordContent());
    strippedParsedRecord.setParsedRecord(parsedRecord);
    return strippedParsedRecord;
  }

  @NotNull
  private static ParsedRecordContent createParsedRecordContent() {
    FieldContent fieldContent = new FieldContent();
    fieldContent.setInd1("ind1");
    fieldContent.setInd2("ind2");
    fieldContent.setSubfields(List.of(Map.of("a", "a1", "b", "b1")));
    fieldContent.setLinkDetails(new LinkDetails());
    Map<String, FieldContent> fields = Map.of("tag", fieldContent);
    return new ParsedRecordContent(List.of(fields), "leader");
  }

  @NotNull
  private static SourceParsedContent createSourceParsedContent() {
    FieldParsedContent fieldContent =
        new FieldParsedContent("tag", "ind1", "ind2",
            Map.of("a", List.of("a1", "a2")), new LinkDetails());
    return new SourceParsedContent(TEST_ID, TEST_PROPERTY_VALUE, List.of(fieldContent));
  }
}
