package org.folio.spring.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record UserToken(String accessToken, Instant accessTokenExpiration) {
}
