package org.folio.spring.model;

import java.time.Instant;
import lombok.Builder;

@Builder
@Deprecated(since = "10.0.0", forRemoval = true)
public record UserToken(String accessToken, Instant accessTokenExpiration) {
}
