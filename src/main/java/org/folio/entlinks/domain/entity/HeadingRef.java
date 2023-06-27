package org.folio.entlinks.domain.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class HeadingRef implements Serializable {

  private String headingType;

  private String heading;

  public HeadingRef(HeadingRef other) {
    this.heading = other.heading;
    this.headingType = other.headingType;
  }
}
