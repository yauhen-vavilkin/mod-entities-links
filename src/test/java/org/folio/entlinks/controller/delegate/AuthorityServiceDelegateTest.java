package org.folio.entlinks.controller.delegate;

import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.CREATE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.DELETE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.UPDATE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityServiceDelegateTest {

  public static final String CONSORTIUM_SOURCE = "CONSORTIUM-MARC";

  private final ArgumentCaptor<AuthorityDto> captor = ArgumentCaptor.forClass(AuthorityDto.class);
  @Mock
  private AuthorityService service;
  @Mock
  private AuthorityMapper mapper;
  @Mock
  private AuthorityDomainEventPublisher eventPublisher;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private ConsortiumAuthorityPropagationService propagationService;
  @InjectMocks
  private AuthorityServiceDelegate delegate;

  @BeforeEach
  void setUp() {
    lenient().when(context.getTenantId()).thenReturn(TENANT_ID);
  }

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
    doNothing().when(propagationService).propagate(entity, CREATE, TENANT_ID);
    when(mapper.toDto(any(Authority.class))).thenReturn(expectedDto);

    // when
    var created = delegate.createAuthority(dto);

    // then
    verify(eventPublisher).publishCreateEvent(captor.capture());
    assertEquals(expectedDto, created);
    assertEquals(expectedDto, captor.getValue());
    verify(propagationService).propagate(entity, CREATE, TENANT_ID);
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
    doNothing().when(propagationService).propagate(modifiedEntity, UPDATE, TENANT_ID);
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
    verify(propagationService).propagate(modifiedEntity, UPDATE, TENANT_ID);
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
    doNothing().when(propagationService).propagate(entity, DELETE, TENANT_ID);

    // when
    delegate.deleteAuthorityById(id);

    // then
    verify(eventPublisher).publishSoftDeleteEvent(captor.capture());
    assertEquals(dto, captor.getValue());
    verify(service).getById(id);
    verify(service).deleteById(id);
    verifyNoMoreInteractions(service);
    verify(mapper).toDto(any(Authority.class));
    verify(propagationService).propagate(entity, DELETE, TENANT_ID);
  }

  @Test
  void shouldNotUpdateConsortiumShadowCopyAuthority() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    entity.setSource(CONSORTIUM_SOURCE);
    var expectedParam = new Parameter("id").value(id.toString());
    when(service.getById(id)).thenReturn(entity);

    // then
    Assertions.assertThatThrownBy(() -> delegate.updateAuthority(id, null))
        .isInstanceOf(RequestBodyValidationException.class)
        .hasMessage("UPDATE is not applicable to consortium shadow copy")
        .extracting(ex -> (RequestBodyValidationException) ex)
        .matches(ex -> ex.getInvalidParameters().get(0).equals(expectedParam));
    verify(mapper, times(1)).toEntity(any());
    verifyNoMoreInteractions(mapper);
    verifyNoMoreInteractions(service);
    verifyNoInteractions(eventPublisher);
    verifyNoInteractions(propagationService);
  }

  @Test
  void shouldNotDeleteConsortiumShadowCopyAuthority() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    entity.setSource(CONSORTIUM_SOURCE);
    var expectedParam = new Parameter("id").value(id.toString());
    when(service.getById(id)).thenReturn(entity);

    // then
    Assertions.assertThatThrownBy(() -> delegate.deleteAuthorityById(id))
        .isInstanceOf(RequestBodyValidationException.class)
        .hasMessage("DELETE is not applicable to consortium shadow copy")
        .extracting(ex -> (RequestBodyValidationException) ex)
        .matches(ex -> ex.getInvalidParameters().get(0).equals(expectedParam));
    verifyNoInteractions(mapper);
    verifyNoMoreInteractions(service);
    verifyNoInteractions(eventPublisher);
    verifyNoInteractions(propagationService);
  }
}
