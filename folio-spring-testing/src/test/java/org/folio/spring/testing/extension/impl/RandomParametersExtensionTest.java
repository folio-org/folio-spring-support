package org.folio.spring.testing.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.mockito.Mockito.when;

import org.folio.spring.testing.extension.Random;
import org.folio.spring.testing.extension.RandomInt;
import org.folio.spring.testing.extension.RandomLong;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.Mockito;

@UnitTest
class RandomParametersExtensionTest {

  @Test
  void supportsParameter_AnnotatedWithRandomInt_ReturnsTrue() {
    final RandomParametersExtension extension = new RandomParametersExtension();
    ParameterContext parameterContext = Mockito.mock(ParameterContext.class);

    when(parameterContext.isAnnotated(RandomInt.class)).thenReturn(true);

    assertTrue(extension.supportsParameter(parameterContext, null));
  }

  @Test
  void supportsParameter_AnnotatedWithRandomLong_ReturnsTrue() {
    final RandomParametersExtension extension = new RandomParametersExtension();
    ParameterContext parameterContext = Mockito.mock(ParameterContext.class);

    when(parameterContext.isAnnotated(RandomLong.class)).thenReturn(true);

    assertTrue(extension.supportsParameter(parameterContext, null));
  }

  @Test
  void supportsParameter_AnnotatedWithRandom_ReturnsTrue() {
    final RandomParametersExtension extension = new RandomParametersExtension();
    ParameterContext parameterContext = Mockito.mock(ParameterContext.class);

    when(parameterContext.isAnnotated(Random.class)).thenReturn(true);

    assertTrue(extension.supportsParameter(parameterContext, null));
  }

  @Test
  void supportsParameter_NotAnnotated_ReturnsFalse() {
    final RandomParametersExtension extension = new RandomParametersExtension();
    ParameterContext parameterContext = Mockito.mock(ParameterContext.class);

    when(parameterContext.isAnnotated(Random.class)).thenReturn(false);
    when(parameterContext.isAnnotated(RandomInt.class)).thenReturn(false);
    when(parameterContext.isAnnotated(RandomLong.class)).thenReturn(false);

    assertFalse(extension.supportsParameter(parameterContext, null));
  }

  @Test
  void testRandom_InjectRandomObject() {
    TestExecutionResult result =
      invokeTestClassAndRetrieveMethodResult(TestRandomClass.class);

    assertThat(result.getThrowable()).isNotPresent();
    assertThat(result.getStatus()).isEqualTo(TestExecutionResult.Status.SUCCESSFUL);
  }

  @Test
  void testRandom_InjectRandomIntObject() {
    TestExecutionResult result =
      invokeTestClassAndRetrieveMethodResult(TestRandomIntClass.class);

    assertThat(result.getThrowable()).isNotPresent();
    assertThat(result.getStatus()).isEqualTo(TestExecutionResult.Status.SUCCESSFUL);
  }

  @Test
  void testRandom_InjectRandomLongObject() {
    TestExecutionResult result =
      invokeTestClassAndRetrieveMethodResult(TestRandomLongClass.class);

    assertThat(result.getThrowable()).isNotPresent();
    assertThat(result.getStatus()).isEqualTo(TestExecutionResult.Status.SUCCESSFUL);
  }

  private TestExecutionResult invokeTestClassAndRetrieveMethodResult(Class<?> clazz) {
    LauncherDiscoveryRequest request =
      LauncherDiscoveryRequestBuilder.request().selectors(selectClass(clazz)).build();

    Launcher launcher = LauncherFactory.create();

    final TestExecutionResult[] result = new TestExecutionResult[1];

    launcher.registerTestExecutionListeners(
      new TestExecutionListener() {
        @Override
        public void executionFinished(
          TestIdentifier testIdentifier,
          TestExecutionResult testExecutionResult) {
          if (testIdentifier.getDisplayName().startsWith("uniqueTestName")) {
            result[0] = testExecutionResult;
          }
        }
      });

    launcher.execute(request);

    return result[0];
  }

  @ExtendWith(RandomParametersExtension.class)
  static class TestRandomClass {

    @Test
    void uniqueTestName(@Random String s) {
      assertNotNull(s);
    }
  }

  @ExtendWith(RandomParametersExtension.class)
  static class TestRandomIntClass {

    @Test
    void uniqueTestName(@RandomInt(min = 10, max = 11) Integer s) {
      assertNotNull(s);
      assertThat(s).isBetween(10, 11);
    }
  }

  @ExtendWith(RandomParametersExtension.class)
  static class TestRandomLongClass {

    @Test
    void uniqueTestName(@RandomLong(min = 10L, max = 11L) Long s) {
      assertNotNull(s);
      assertThat(s).isBetween(10L, 11L);
    }
  }
}


