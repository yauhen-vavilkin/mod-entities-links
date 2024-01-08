package org.folio.entlinks.service.consortium.propagation;

import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ConsortiumAuthoritySourceFilePropagationService extends ConsortiumPropagationService<AuthoritySourceFile> {

  private final AuthoritySourceFileService sourceFileService;

  public ConsortiumAuthoritySourceFilePropagationService(AuthoritySourceFileService sourceFileService,
                                                         ConsortiumTenantsService tenantsService,
                                                         SystemUserScopedExecutionService executionService) {
    super(tenantsService, executionService);
    this.sourceFileService = sourceFileService;
  }

  protected void doPropagation(AuthoritySourceFile sourceFile, PropagationType propagationType) {
    sourceFile.markAsConsortiumShadowCopy();
    switch (propagationType) {
      case CREATE -> sourceFileService.create(sourceFile);
      case UPDATE -> sourceFileService.update(sourceFile.getId(), sourceFile);
      case DELETE -> sourceFileService.deleteById(sourceFile.getId());
      default -> throw new IllegalStateException("Unexpected value: " + propagationType);
    }
  }

}
