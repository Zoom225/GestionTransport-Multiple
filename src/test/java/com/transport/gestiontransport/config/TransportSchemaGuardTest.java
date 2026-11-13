package com.transport.gestiontransport.config;

import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.sql.ResultSet;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransportSchemaGuardTest {
    @Test
    void refuseUnSchemaHistoriqueAvantTouteGenerationHibernate() throws Exception {
        assertThatThrownBy(() -> TransportSchemaGuard.verify(postgres(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("avant Hibernate");
    }

    @Test
    void accepteUneBasePostgresNeuveOuMigree() throws Exception {
        assertThatCode(() -> TransportSchemaGuard.verify(postgres(0))).doesNotThrowAnyException();
    }

    @Test
    void preserveLesTestsH2Existants() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("H2");
        TransportSchemaGuard.verify(source);
        verify(connection, never()).createStatement();
    }

    private DataSource postgres(long tablesHistoriques) throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(result);
        when(result.getLong(1)).thenReturn(tablesHistoriques);
        return source;
    }
}
