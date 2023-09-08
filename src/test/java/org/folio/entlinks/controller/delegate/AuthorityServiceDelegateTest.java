package org.folio.entlinks.controller.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityServiceDelegateTest {

  @Mock
  private AuthorityService service;

  @Mock
  private AuthorityMapper mapper;

  @Mock
  private AuthorityDomainEventPublisher eventPublisher;

  @InjectMocks
  private AuthorityServiceDelegate delegate;

  private final ArgumentCaptor<AuthorityDto> captor = ArgumentCaptor.forClass(AuthorityDto.class);

  @Test
  void shouldCreateAuthority() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    var expectedDto = new AuthorityDto().id(id);
    var dto = new AuthorityDto().id(id);
    when(mapper.toEntity(any(AuthorityDto.class))).thenReturn(entity);
    when(service.create(entity)).thenReturn(entity);
    when(mapper.toDto(any(Authority.class))).thenReturn(expectedDto);

    // when
    var created = delegate.createAuthority(dto);

    // then
    verify(eventPublisher).publishCreateEvent(captor.capture());
    assertEquals(expectedDto, created);
    assertEquals(expectedDto, captor.getValue());
  }

  @Test
  void shouldUpdateAuthority() {
    // given
    var id = UUID.randomUUID();
    var modificationDto = new AuthorityDto().id(id);
    var existingEntity = new Authority();
    existingEntity.setId(id);
    var modifiedEntity = new Authority();
    modifiedEntity.setId(id);
    var oldDto = new AuthorityDto().id(id);
    var newDto = new AuthorityDto().id(id);

    when(mapper.toEntity(modificationDto)).thenReturn(modifiedEntity);
    when(service.getById(id)).thenReturn(existingEntity);
    when(mapper.toDto(any(Authority.class))).thenReturn(oldDto).thenReturn(newDto);
    when(service.update(id, modifiedEntity)).thenReturn(modifiedEntity);
    var captor2 = ArgumentCaptor.forClass(AuthorityDto.class);

    // when
    delegate.updateAuthority(id, modificationDto);

    // then
    verify(eventPublisher).publishUpdateEvent(captor.capture(), captor2.capture());
    assertEquals(oldDto, captor.getValue());
    assertEquals(newDto, captor2.getValue());
    verify(service).getById(id);
    verify(service).update(any(UUID.class), any(Authority.class));
    verifyNoMoreInteractions(service);
    verify(mapper, times(2)).toDto(any(Authority.class));
    verify(mapper).toEntity(any(AuthorityDto.class));
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void shouldDeleteAuthority() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    var dto = new AuthorityDto().id(id);
    when(service.getById(id)).thenReturn(entity);
    when(mapper.toDto(entity)).thenReturn(dto);
    doNothing().when(service).deleteById(id);

    // when
    delegate.deleteAuthorityById(id);

    // then
    verify(eventPublisher).publishDeleteEvent(captor.capture());
    assertEquals(dto, captor.getValue());
    verify(service).getById(id);
    verify(service).deleteById(id);
    verifyNoMoreInteractions(service);
    verify(mapper).toDto(any(Authority.class));
  }
}
