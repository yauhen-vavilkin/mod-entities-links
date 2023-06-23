package org.folio.entlinks.service.links.model;

import java.util.List;
import java.util.Set;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;

public record AuthorityRuleValidationResult(Set<AuthorityData> validAuthorities,
                                            List<InstanceAuthorityLink> validLinks,
                                            List<InstanceAuthorityLink> invalidLinks) {
}
