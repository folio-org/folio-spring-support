package org.folio.spring.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FolioLoggingContextLookupTest {

  private static final String TEST_TENANT = "test";
  private static final String TEST_USER_ID = "af51421f-72bb-4672-ac6d-f933c7bc3fbd";
  private static final String TEST_REQUEST_ID = "614";
  private static final String TEST_MOD_ID = "mod-test";
  private final FolioLoggingContextLookup contextLookup = new FolioLoggingContextLookup();

  @BeforeEach
  void setUp() {
    FolioLoggingContextHolder.removeFolioExecutionContext(null);
  }

  @Test
  void testLookupWhenContextIsEmpty() {
    var lookupVal = contextLookup.lookup("tenantid");
    assertEquals("", lookupVal);
  }

  @Test
  void testLookupWhenKeyIsNull() {
    assertThrows(IllegalArgumentException.class, () -> contextLookup.lookup(null));
  }

  @Test
  void testLookupWhenContextNotSupportProvidedKey() {
    var lookupVal = contextLookup.lookup("not-supported-key");
    assertEquals("", lookupVal);
  }

  @Test
  void testLookupForTenantId() {
    saveFolioContext();
    var lookupVal = contextLookup.lookup("tenantid");
    assertEquals(TEST_TENANT, lookupVal);
  }

  @Test
  void testLookupForUserId() {
    saveFolioContext();
    var lookupVal = contextLookup.lookup("userid");
    assertEquals(TEST_USER_ID, lookupVal);
  }

  @Test
  void testLookupForRequestId() {
    saveFolioContext();
    var lookupVal = contextLookup.lookup("requestid");
    assertEquals(TEST_REQUEST_ID, lookupVal);
  }

  @Test
  void testLookupForModuleId() {
    saveFolioContext();
    var lookupVal = contextLookup.lookup("moduleid");
    assertEquals(TEST_MOD_ID, lookupVal);
  }

  private void saveFolioContext() {
    FolioLoggingContextHolder.putFolioExecutionContext(new FolioExecutionContext() {

      @Override
      public String getTenantId() {
        return TEST_TENANT;
      }

      @Override
      public UUID getUserId() {
        return UUID.fromString(TEST_USER_ID);
      }

      @Override
      public String getRequestId() {
        return TEST_REQUEST_ID;
      }

      @Override
      public FolioModuleMetadata getFolioModuleMetadata() {
        return new FolioModuleMetadata() {

          @Override
          public String getModuleName() {
            return TEST_MOD_ID;
          }

          @Override public String getDBSchemaName(String tenantId) {
            return null;
          }
        };
      }
    });
  }
}
