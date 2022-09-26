package org.folio.entlinks.model.converter;

import java.util.List;
import org.folio.entlinks.model.entity.InstanceLink;
import org.folio.entlinks.model.projection.LinkCountView;
import org.folio.qm.domain.dto.InstanceLinkDto;
import org.folio.qm.domain.dto.InstanceLinkDtoCollection;
import org.folio.qm.domain.dto.LinksCountDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstanceLinkMapper {

    InstanceLinkDto convert(InstanceLink source);

    InstanceLink convert(InstanceLinkDto source);

    default InstanceLinkDtoCollection convert(List<InstanceLink> source) {
      var convertedLinks = source.stream().map(this::convert).toList();

      return new InstanceLinkDtoCollection()
          .links(convertedLinks)
          .totalRecords(source.size());
    }

    LinksCountDto convert(LinkCountView source);
}
