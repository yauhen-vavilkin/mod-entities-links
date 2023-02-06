package org.folio.entlinks.service.messaging.authority.model;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.SubfieldImpl;

class FieldChangeHolderTest {

  private static SubfieldChange valueChangeEmpty(String f) {
    return valueChange(f, EMPTY);
  }

  private static SubfieldChange valueChange(String f, String empty) {
    return new SubfieldChange().code(f).value(empty);
  }

  @Test
  void getBibSubfieldCodes_positive_modificationsExists() {
    var dataField = new DataFieldImpl("100", '0', '0');
    dataField.addSubfield(new SubfieldImpl('a', "a-data"));
    dataField.addSubfield(new SubfieldImpl('d', "d-data"));
    dataField.addSubfield(new SubfieldImpl('h', "h-data"));
    dataField.addSubfield(new SubfieldImpl('t', "t-data"));
    var linkingRule = new InstanceAuthorityLinkingRule();
    linkingRule.setAuthorityField("100");
    linkingRule.setBibField("240");
    linkingRule.setAuthoritySubfields(new char[] {'f', 'g', 'h', 'k', 'l', 'm', 'n', 'o', 'p', 'r', 's', 't'});
    linkingRule.setSubfieldModifications(List.of(new SubfieldModification().source("t").target("a")));

    var fieldChangeHolder = new FieldChangeHolder(dataField, linkingRule);

    assertThat(fieldChangeHolder.getBibSubfieldCodes()).containsExactly('a', 'h');
  }

  @Test
  void toFieldChange_positive_modificationsExists() {
    var dataField = new DataFieldImpl("100", '0', '0');
    dataField.addSubfield(new SubfieldImpl('a', "a-data"));
    dataField.addSubfield(new SubfieldImpl('d', "d-data"));
    dataField.addSubfield(new SubfieldImpl('h', "h-data"));
    dataField.addSubfield(new SubfieldImpl('t', "t-data"));
    var linkingRule = new InstanceAuthorityLinkingRule();
    linkingRule.setAuthorityField("100");
    linkingRule.setBibField("240");
    linkingRule.setAuthoritySubfields(new char[] {'f', 'g', 'h', 'k', 'l', 'm', 'n', 't'});
    linkingRule.setSubfieldModifications(List.of(new SubfieldModification().source("t").target("a")));

    var fieldChangeHolder = new FieldChangeHolder(dataField, linkingRule);

    var actual = fieldChangeHolder.toFieldChange();

    assertThat(actual)
      .extracting(FieldChange::getField, FieldChange::getSubfields)
      .containsExactly(linkingRule.getBibField(), List.of(
        valueChange("a", "t-data"),
        valueChangeEmpty("f"),
        valueChangeEmpty("g"),
        valueChange("h", "h-data"),
        valueChangeEmpty("k"),
        valueChangeEmpty("l"),
        valueChangeEmpty("m"),
        valueChangeEmpty("n")
      ));
  }

  @Test
  void toFieldChange_positive_whenSubfield0ChangeExist() {
    var dataField = new DataFieldImpl("100", '0', '0');
    dataField.addSubfield(new SubfieldImpl('h', "h-data"));
    dataField.addSubfield(new SubfieldImpl('a', "a-data"));
    dataField.addSubfield(new SubfieldImpl('d', "d-data"));
    dataField.addSubfield(new SubfieldImpl('t', "t-data"));
    var linkingRule = new InstanceAuthorityLinkingRule();
    linkingRule.setAuthorityField("100");
    linkingRule.setBibField("240");
    linkingRule.setAuthoritySubfields(new char[] {'a', 'g', 'h', 'k'});

    var fieldChangeHolder = new FieldChangeHolder(dataField, linkingRule);
    fieldChangeHolder.addExtraSubfieldChange(valueChange("0", "0-data"));

    var actual = fieldChangeHolder.toFieldChange();

    assertThat(actual)
      .extracting(FieldChange::getField, FieldChange::getSubfields)
      .containsExactly(linkingRule.getBibField(), List.of(
        valueChange("a", "a-data"),
        valueChangeEmpty("g"),
        valueChange("h", "h-data"),
        valueChangeEmpty("k"),
        valueChange("0", "0-data")
      ));
  }

  @Test
  void toFieldChange_positive_whenSubfield0ChangeWasTryingToAddButItNull() {
    var dataField = new DataFieldImpl("100", '0', '0');
    dataField.addSubfield(new SubfieldImpl('h', "h-data"));
    dataField.addSubfield(new SubfieldImpl('a', "a-data"));
    dataField.addSubfield(new SubfieldImpl('d', "d-data"));
    dataField.addSubfield(new SubfieldImpl('t', "t-data"));
    var linkingRule = new InstanceAuthorityLinkingRule();
    linkingRule.setAuthorityField("100");
    linkingRule.setBibField("240");
    linkingRule.setAuthoritySubfields(new char[] {'a', 'g', 'h', 'k'});

    var fieldChangeHolder = new FieldChangeHolder(dataField, linkingRule);
    fieldChangeHolder.addExtraSubfieldChange(null);

    var actual = fieldChangeHolder.toFieldChange();

    assertThat(actual)
      .extracting(FieldChange::getField, FieldChange::getSubfields)
      .containsExactly(linkingRule.getBibField(), List.of(
        valueChange("a", "a-data"),
        valueChangeEmpty("g"),
        valueChange("h", "h-data"),
        valueChangeEmpty("k")
      ));
  }
}
