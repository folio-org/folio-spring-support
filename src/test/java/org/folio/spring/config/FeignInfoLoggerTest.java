package org.folio.spring.config;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class FeignInfoLoggerTest {

  @Mock
  private Logger mockLogger;

  @Test
  void testLog() {
    var logger = new FeignClientConfiguration.FeignInfoLogger(mockLogger);
    logger.log("test(Key)", "Test %s message", "log");

    verify(mockLogger).info("[test] Test log message");
  }

}