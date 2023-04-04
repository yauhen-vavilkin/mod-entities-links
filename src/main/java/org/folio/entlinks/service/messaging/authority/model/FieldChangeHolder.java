package org.folio.entlinks.service.messaging.authority.model;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bouncycastle.util.Arrays;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.impl.SubfieldImpl;

public class FieldChangeHolder {

  private final InstanceAuthorityLinkingRule linkingRule;

  private final @Getter String bibField;

  private final List<Subfield> authSubfields;

  private final List<SubfieldChange> extraSubfieldChanges = new ArrayList<>();

  public FieldChangeHolder(DataField dataField, InstanceAuthorityLinkingRule linkingRule) {
    this.linkingRule = linkingRule;
    this.bibField = linkingRule.getBibField();
    this.authSubfields = getAuthSubfields(dataField.getSubfields());
  }

  public void addExtraSubfieldChange(SubfieldChange change) {
    if (change != null) {
      extraSubfieldChanges.add(change);
    }
  }

  public FieldChange toFieldChange() {
    var subfieldChanges = toSubfieldsChange();
    if (!extraSubfieldChanges.isEmpty()) {
      subfieldChanges.addAll(extraSubfieldChanges);
    }
    return new FieldChange().field(getBibField()).subfields(subfieldChanges);
  }

  private List<SubfieldChange> toSubfieldsChange() {
    // create subfield changes for subfields that exist in authority
    var subfieldChanges = authSubfields.stream()
      .map(subfield -> new SubfieldChange()
        .code(Character.toString(subfield.getCode()))
        .value(subfield.getData()))
      .collect(Collectors.groupingBy(subfieldChange -> subfieldChange.getCode().charAt(0)));

    // create subfield changes for subfields that missing in authority but still could be controlled
    for (char subfieldCode : linkingRule.getAuthoritySubfields()) {
      var code = getCode(subfieldCode);
      subfieldChanges.putIfAbsent(code, List.of(new SubfieldChange().code(Character.toString(code)).value(EMPTY)));
    }

    return subfieldChanges.values().stream()
      .flatMap(List::stream)
      .sorted(Comparator.comparing(SubfieldChange::getCode))
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<Subfield> getAuthSubfields(List<Subfield> subfields) {
    return subfields.stream()
      .filter(subfield -> Arrays.contains(linkingRule.getAuthoritySubfields(), subfield.getCode()))
      .map(subfield -> {
        var code = getCode(subfield);
        return (Subfield) new SubfieldImpl(code, subfield.getData());
      })
      .sorted(Comparator.comparing(Subfield::getCode))
      .toList();
  }

  private char getCode(Subfield subfield) {
    return getCode(subfield.getCode());
  }

  private char getCode(char subfieldCode) {
    var subfieldModifications = linkingRule.getSubfieldModifications();
    if (subfieldModifications != null && !subfieldModifications.isEmpty()) {
      for (SubfieldModification subfieldModification : subfieldModifications) {
        if (subfieldModification.getSource().charAt(0) == subfieldCode) {
          return subfieldModification.getTarget().charAt(0);
        }
      }
    }
    return subfieldCode;
  }
}
