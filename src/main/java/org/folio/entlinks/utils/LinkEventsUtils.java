package org.folio.entlinks.utils;


import static org.folio.entlinks.utils.DateUtils.currentTsInString;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.folio.entlinks.domain.dto.ChangeTarget;
import org.folio.entlinks.domain.dto.ChangeTargetLink;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;

@UtilityClass
public class LinkEventsUtils {

  public static LinksChangeEvent constructEvent(UUID jobId, UUID authorityId,
                                                LinksChangeEvent.TypeEnum eventType,
                                                List<InstanceAuthorityLink> partition,
                                                List<FieldChange> fieldChanges) {
    return new LinksChangeEvent()
      .jobId(jobId)
      .type(eventType)
      .authorityId(authorityId)
      .updateTargets(toChangeTargets(partition))
      .subfieldsChanges(fieldChanges)
      .ts(currentTsInString());
  }


  public static List<ChangeTarget> toChangeTargets(List<InstanceAuthorityLink> partition) {
    return partition.stream()
      .collect(Collectors.groupingBy(link -> link.getLinkingRule().getBibField()))
      .entrySet().stream()
      .map(e -> new ChangeTarget().field(e.getKey())
        .links(e.getValue().stream().map(LinkEventsUtils::toChangeTargetLink).toList()))
      .toList();
  }

  public static ChangeTargetLink toChangeTargetLink(InstanceAuthorityLink instanceAuthorityLink) {
    return new ChangeTargetLink().linkId(instanceAuthorityLink.getId())
      .instanceId(instanceAuthorityLink.getInstanceId());
  }

  public static Map<UUID, List<InstanceAuthorityLink>> groupLinksByAuthorityId(List<InstanceAuthorityLink> links) {
    return links.stream()
      .collect(Collectors.groupingBy(link -> link.getAuthorityData().getId()));
  }
}
