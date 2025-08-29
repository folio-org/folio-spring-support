package org.folio.spring.model;

import lombok.Builder;
import lombok.With;

@Builder
@Deprecated(since = "10.0.0", forRemoval = true)
public record SystemUser(String username, String okapiUrl, String tenantId,
                         @With UserToken token, @With String userId) {
}
