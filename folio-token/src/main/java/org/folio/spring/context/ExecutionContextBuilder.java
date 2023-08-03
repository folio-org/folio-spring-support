package org.folio.spring.context;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.spring.kafka.KafkaUtils.getHeaderValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionContextBuilder {

  private final FolioModuleMetadata moduleMetadata;

  public Builder builder() {
    return new Builder(moduleMetadata);
  }

  public FolioExecutionContext forMessageHeaders(MessageHeaders headers) {
    var tenantId = getHeaderValue(XOkapiHeaders.TENANT, headers);
    var okapiUrl = getHeaderValue(XOkapiHeaders.URL, headers);
    var token = getHeaderValue(XOkapiHeaders.TOKEN, headers);
    var userId = getHeaderValue(XOkapiHeaders.USER_ID, headers);
    var requestId = getHeaderValue(XOkapiHeaders.REQUEST_ID, headers);

    return buildContext(okapiUrl, tenantId, token, userId, requestId);
  }

  public FolioExecutionContext forSystemUser(SystemUser systemUser) {
    var okapiUrl = systemUser.okapiUrl();
    var tenantId = systemUser.tenantId();
    var token = systemUser.token() == null ? null : systemUser.token().accessToken();
    var userId = systemUser.userId();

    return buildContext(okapiUrl, tenantId, token, userId, null);
  }

  private FolioExecutionContext buildContext(String okapiUrl, String tenantId, String token, String userId,
                                             String requestId) {
    Map<String, Collection<String>> headers = new HashMap<>();
    if (isNotBlank(okapiUrl)) {
      headers.put(XOkapiHeaders.URL, singleton(okapiUrl));
    }
    if (isNotBlank(tenantId)) {
      headers.put(XOkapiHeaders.TENANT, singleton(tenantId));
    }
    if (isNotBlank(token)) {
      headers.put(XOkapiHeaders.TOKEN, singleton(token));
    }
    if (isNotBlank(userId)) {
      headers.put(XOkapiHeaders.USER_ID, singleton(userId));
    }
    if (isNotBlank(requestId)) {
      headers.put(XOkapiHeaders.REQUEST_ID, singleton(requestId));
    }

    return builder()
        .withTenantId(tenantId)
        .withOkapiUrl(okapiUrl)
        .withUserId(userId)
        .withToken(token)
        .withRequestId(requestId)
        .withOkapiHeaders(headers)
        .withAllHeaders(headers)
        .build();
  }

  @With
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {

    private final FolioModuleMetadata moduleMetadata;
    private final Map<String, Collection<String>> allHeaders;
    private final Map<String, Collection<String>> okapiHeaders;
    private String tenantId;
    private String okapiUrl;
    private String token;
    private String userId;
    private String requestId;

    public Builder(FolioModuleMetadata moduleMetadata) {
      this.moduleMetadata = moduleMetadata;
      this.allHeaders = new HashMap<>();
      this.okapiHeaders = new HashMap<>();
    }

    public FolioExecutionContext build() {
      return new FolioExecutionContext() {
        @Override
        public String getTenantId() {
          return tenantId;
        }

        @Override
        public String getOkapiUrl() {
          return okapiUrl;
        }

        @Override
        public String getToken() {
          return token;
        }

        @Override
        public UUID getUserId() {
          return isNotBlank(userId) ? UUID.fromString(userId) : FolioExecutionContext.super.getUserId();
        }

        @Override
        public String getRequestId() {
          return requestId;
        }

        @Override
        public Map<String, Collection<String>> getAllHeaders() {
          return allHeaders;
        }

        @Override
        public Map<String, Collection<String>> getOkapiHeaders() {
          return okapiHeaders;
        }

        @Override
        public FolioModuleMetadata getFolioModuleMetadata() {
          return moduleMetadata;
        }
      };
    }
  }
}
