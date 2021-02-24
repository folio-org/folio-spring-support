package org.folio.spring.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataSourceFolioWrapperTest {
  @Mock
  private Connection connection;
  @Mock
  private DataSource dataSource;
  @Mock
  private FolioExecutionContext executionContext;
  @InjectMocks
  private DataSourceFolioWrapper wrapper;

  @BeforeEach
  void setUpStubs() throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
  }

  @Test
  void shouldThrowIllegalArgumentIfTenantIsNotValid() {
    when(executionContext.getTenantId()).thenReturn("f\"_drop_table");

    assertThrows(IllegalArgumentException.class, () -> wrapper.getConnection());
  }

  @Test
  void shouldReturnConnectionIfTenantValid() throws Exception {
    var preparedStatement = mock(PreparedStatement.class);

    when(executionContext.getTenantId()).thenReturn("diku");
    when(executionContext.getFolioModuleMetadata())
      .thenReturn(mock(FolioModuleMetadata.class));
    when(executionContext.getFolioModuleMetadata().getDBSchemaName(any()))
      .thenReturn("diku_folio_spring_base");
    when(connection.prepareStatement(any())).thenReturn(preparedStatement);

    assertNotNull(wrapper.getConnection());
  }

  @Test
  void shouldReturnConnectionIfTenantIsNull() throws Exception {
    var preparedStatement = mock(PreparedStatement.class);

    when(executionContext.getTenantId()).thenReturn(null);
    when(connection.prepareStatement(any())).thenReturn(preparedStatement);

    assertNotNull(wrapper.getConnection());
  }

  @Test
  void shouldReturnConnectionIfTenantIsEmpty() throws Exception {
    var preparedStatement = mock(PreparedStatement.class);

    when(executionContext.getTenantId()).thenReturn("");
    when(connection.prepareStatement(any())).thenReturn(preparedStatement);

    assertNotNull(wrapper.getConnection());
  }
}
