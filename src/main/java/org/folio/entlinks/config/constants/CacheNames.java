package org.folio.entlinks.config.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheNames {

  public static final String AUTHORITY_MAPPING_RULES_CACHE = "authority-mapping-rules-cache";
  public static final String AUTHORITY_LINKING_RULES_CACHE = "authority-linking-rules-cache";
  public static final String CONSORTIUM_TENANTS_CACHE = "consortium-tenants-cache";
  public static final String CONSORTIUM_CENTRAL_TENANT = "consortium-central-tenant-cache";
}
