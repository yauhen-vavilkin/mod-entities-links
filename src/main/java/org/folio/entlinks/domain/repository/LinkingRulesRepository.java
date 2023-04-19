package org.folio.entlinks.domain.repository;

import java.util.List;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkingRulesRepository extends JpaRepository<InstanceAuthorityLinkingRule, Integer> {

  List<InstanceAuthorityLinkingRule> findByAuthorityField(String authorityField);
}
