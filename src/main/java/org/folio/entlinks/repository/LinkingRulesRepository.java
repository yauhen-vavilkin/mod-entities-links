package org.folio.entlinks.repository;

import org.folio.entlinks.model.entity.LinkingRules;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkingRulesRepository extends JpaRepository<LinkingRules, String> {

  LinkingRules findByLinkingPairType(String linkingPairType);
}
