package org.folio.entlinks.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@ToString
@Table(name = "authority_archive")
public class AuthorityArchive extends AuthorityBase {
}
