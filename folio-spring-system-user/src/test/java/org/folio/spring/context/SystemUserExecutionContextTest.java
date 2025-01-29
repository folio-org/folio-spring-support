package org.folio.spring.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.function.Supplier;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemUserExecutionContextTest {

  @ParameterizedTest
  @NullAndEmptySource
  void testTokenHeaderExclusion(String value) {
    SystemUserExecutionContext context = new SystemUserExecutionContext(
      null,
      SystemUser
        .builder()
        .token(UserToken.builder().accessToken(value).accessTokenExpiration(Instant.MAX).build())
        .build(),
      null
    );

    assertThat(context.getAllHeaders()).doesNotContainKeys(XOkapiHeaders.TOKEN);
  }

  @Test
  void testTokenRefresh() {
    Supplier<SystemUser> refresher = spy(new TestRefresher());
    UserToken originalToken = spy(UserToken.builder().accessToken("token1").build());

    SystemUserExecutionContext context = new SystemUserExecutionContext(
      null,
      SystemUser.builder().token(originalToken).build(),
      refresher
    );

    when(originalToken.accessTokenExpiration())
      .thenReturn(Instant.MAX) // not yet expired
      .thenReturn(Instant.now()); // expired

    assertThat(context.getToken()).isEqualTo("token1");
    assertThat(context.getToken()).isEqualTo("token2");
    assertThat(context.getToken()).isEqualTo("token2");

    InOrder verifier = inOrder(originalToken, refresher);

    // two calls for verifying time, and once for printing in logs
    verifier.verify(originalToken, times(3)).accessTokenExpiration();
    verifier.verify(refresher, times(1)).get();
    verifier.verifyNoMoreInteractions();
  }

  // needed for mockito to spy and check order
  private static final class TestRefresher implements Supplier<SystemUser> {

    public SystemUser get() {
      return SystemUser
        .builder()
        .token(UserToken.builder().accessToken("token2").accessTokenExpiration(Instant.MAX).build())
        .build();
    }
  }
}
