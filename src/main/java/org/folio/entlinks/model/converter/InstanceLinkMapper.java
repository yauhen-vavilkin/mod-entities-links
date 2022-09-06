package org.folio.entlinks.model.converter;

import org.folio.entlinks.model.entity.InstanceLink;
import org.folio.qm.domain.dto.InstanceLinkDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstanceLinkMapper {

    InstanceLinkDto convert(InstanceLink source);

    InstanceLink convert(InstanceLinkDto source);
}
