package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.entlinks.config.properties.InstanceAuthorityChangeProperties;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.springframework.stereotype.Component;

@Component
public class DeleteAuthorityChangeHandler extends AbstractAuthorityChangeHandler {

  private final InstanceAuthorityLinkingService linkingService;

  public DeleteAuthorityChangeHandler(InstanceAuthorityLinkingService linkingService,
                                      InstanceAuthorityChangeProperties instanceAuthorityChangeProperties) {
    super(instanceAuthorityChangeProperties, linkingService);
    this.linkingService = linkingService;
  }

  @Override
  public List<LinksChangeEvent> handle(List<AuthorityChangeHolder> changes) {
    if (changes == null || changes.isEmpty()) {
      return emptyList();
    }

    List<LinksChangeEvent> linksEvents = new ArrayList<>();

    for (var change : changes) {
      var linksChangeEvents = handleLinksByPartitions(change.getAuthorityId(),
        instanceLinks -> constructEvent(UUID.randomUUID(), change.getAuthorityId(), instanceLinks, emptyList())
      );
      linksEvents.addAll(linksChangeEvents);
    }

    var authorityIds = changes.stream().map(AuthorityChangeHolder::getAuthorityId).collect(Collectors.toSet());
    linkingService.deleteByAuthorityIdIn(authorityIds);
    return linksEvents;
  }

  @Override
  public LinksChangeEvent.TypeEnum getReplyEventType() {
    return LinksChangeEvent.TypeEnum.DELETE;
  }

  @Override
  public AuthorityChangeType supportedAuthorityChangeType() {
    return AuthorityChangeType.DELETE;
  }

}
