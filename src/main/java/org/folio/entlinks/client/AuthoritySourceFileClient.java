package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("authority-source-files")
public interface AuthoritySourceFileClient {


  @GetMapping(produces = APPLICATION_JSON_VALUE)
  AuthoritySourceFiles fetchAuthoritySourceFiles(@RequestParam int limit);

  record AuthoritySourceFile(UUID id, String baseUrl, String name, List<String> codes) { }

  record AuthoritySourceFiles(List<AuthoritySourceFile> authoritySourceFiles) { }
}
