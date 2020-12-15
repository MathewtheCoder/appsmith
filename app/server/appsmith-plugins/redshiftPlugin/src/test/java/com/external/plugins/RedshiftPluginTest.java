package com.external.plugins;

//TODO: remove unused imports
import com.appsmith.external.models.ActionConfiguration;
import com.appsmith.external.models.ActionExecutionResult;
import com.appsmith.external.models.AuthenticationDTO;
import com.appsmith.external.models.DatasourceConfiguration;
import com.appsmith.external.models.DatasourceStructure;
import com.appsmith.external.models.Endpoint;
import com.appsmith.external.pluginExceptions.AppsmithPluginError;
import com.appsmith.external.pluginExceptions.AppsmithPluginException;
import com.appsmith.external.plugins.PluginExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.mockito.Mockito;
import static org.easymock.EasyMock.expect;

import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.*;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



/**
 * Unit tests for the RedshiftPlugin
 */
@Slf4j
public class RedshiftPluginTest {
    RedshiftPlugin.RedshiftPluginExecutor pluginExecutor = new RedshiftPlugin.RedshiftPluginExecutor();

    private static String address;
    private static Integer port;
    private static String username;
    private static String password;
    private  static String dbName;

    @BeforeClass
    public static void setUp() {
        address = "address";
        port = 5439;
        username = "username";
        password = "password";
        dbName = "dbName";
    }

    private DatasourceConfiguration createDatasourceConfiguration() {
        AuthenticationDTO authDTO = new AuthenticationDTO();
        authDTO.setAuthType(AuthenticationDTO.Type.USERNAME_PASSWORD);
        authDTO.setUsername(username);
        authDTO.setPassword(password);
        authDTO.setDatabaseName(dbName);

        Endpoint endpoint = new Endpoint();
        endpoint.setHost(address);
        endpoint.setPort(port.longValue());

        DatasourceConfiguration dsConfig = new DatasourceConfiguration();
        dsConfig.setAuthentication(authDTO);
        dsConfig.setEndpoints(List.of(endpoint));
        return dsConfig;
    }

    @Test
    public void testConnectRedshiftContainer() {
        DatasourceConfiguration dsConfig = createDatasourceConfiguration();
        Mono<Connection> dsConnectionMono = pluginExecutor.datasourceCreate(dsConfig);

        StepVerifier.create(dsConnectionMono)
                .expectErrorMatches(throwable -> throwable instanceof AppsmithPluginException && throwable.getMessage()
                        .equals(new AppsmithPluginException(AppsmithPluginError.PLUGIN_ERROR, "Error connecting" +
                        " to Redshift.").getMessage()))
                .verify();
    }

    @Test
    public void itShouldValidateDatasourceWithEmptyEndpoints() {

        DatasourceConfiguration dsConfig = createDatasourceConfiguration();
        dsConfig.setEndpoints(new ArrayList<>());

        Assert.assertEquals(Set.of("Missing endpoint."),
                pluginExecutor.validateDatasource(dsConfig));
    }

    @Test
    public void itShouldValidateDatasourceWithEmptyHost() {

        DatasourceConfiguration dsConfig = createDatasourceConfiguration();
        dsConfig.getEndpoints().get(0).setHost("");

        Assert.assertEquals(Set.of("Missing hostname."),
                pluginExecutor.validateDatasource(dsConfig));
    }

    @Test
    public void itShouldValidateDatasourceWithInvalidHostname() {

        String hostname = "jdbc://localhost";
        DatasourceConfiguration dsConfig = createDatasourceConfiguration();
        dsConfig.getEndpoints().get(0).setHost("jdbc://localhost");

        Assert.assertEquals(Set.of("Host value cannot contain `/` or `:` characters. Found `" + hostname + "`."),
                pluginExecutor.validateDatasource(dsConfig));
    }

    @Test
    public void testExecute() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.isValid(Mockito.anyInt())).thenReturn(true);

        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(Mockito.any())).thenReturn(true);
        doNothing().when(mockStatement).close();

        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockResultSet.getObject(Mockito.anyInt())).thenReturn("", 1, "", "Jack", "", "jill", "", "jack@exemplars.com"
                , null, "", "", "", "", "");
        when(mockResultSet.getDate(Mockito.anyInt())).thenReturn(Date.valueOf("2018-12-31"), Date.valueOf("2018-11-30"));
        when(mockResultSet.getString(Mockito.anyInt())).thenReturn("18:32:45", "12:05:06+00");
        when(mockResultSet.getTime(Mockito.anyInt())).thenReturn(Time.valueOf("20:45:15"));
        when(mockResultSet.getObject(Mockito.anyInt(), Mockito.any(Class.class))).thenReturn(OffsetDateTime.parse(
                "2018-11-30T19:45:15+00"));
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        doNothing().when(mockResultSet).close();

        ResultSetMetaData mockResultSetMetaData = mock(ResultSetMetaData.class);
        when(mockResultSet.getMetaData()).thenReturn(mockResultSetMetaData);

        when(mockResultSetMetaData.getColumnCount()).thenReturn(10);
        when(mockResultSetMetaData.getColumnTypeName(Mockito.anyInt())).thenReturn("int4", "varchar", "varchar",
        "varchar", "date", "date", "time", "timetz", "timestamp", "timestamptz");
        when(mockResultSetMetaData.getColumnName(Mockito.anyInt())).thenReturn("id", "username", "password", "email",
                "spouse_dob", "dob", "time1", "time_tz", "created_on", "created_on_tz");

        ActionConfiguration actionConfiguration = new ActionConfiguration();
        actionConfiguration.setBody("SELECT * FROM users WHERE id = 1");
        DatasourceConfiguration dsConfig = createDatasourceConfiguration();
        Mono<Connection> dsConnectionMono = Mono.just(mockConnection);
        Mono<ActionExecutionResult> executeMono = dsConnectionMono
                .flatMap(conn -> pluginExecutor.execute(conn, dsConfig, actionConfiguration));

        StepVerifier.create(executeMono)
                .assertNext(result -> {
                    assertNotNull(result);
                    assertTrue(result.getIsExecutionSuccess());
                    assertNotNull(result.getBody());

                    final JsonNode node = ((ArrayNode) result.getBody()).get(0);
                    assertEquals("2018-12-31", node.get("dob").asText());
                    assertEquals("18:32:45", node.get("time1").asText());
                    assertEquals("12:05:06+00", node.get("time_tz").asText());
                    assertEquals("2018-11-30T20:45:15Z", node.get("created_on").asText());
                    assertEquals("2018-11-30T19:45:15Z", node.get("created_on_tz").asText());
                    assertTrue(node.get("spouse_dob").isNull());
                    assertArrayEquals(
                            new String[]{
                                    "id",
                                    "username",
                                    "password",
                                    "email",
                                    "spouse_dob",
                                    "dob",
                                    "time1",
                                    "time_tz",
                                    "created_on",
                                    "created_on_tz",
                            },
                            new ObjectMapper()
                                    .convertValue(node, LinkedHashMap.class)
                                    .keySet()
                                    .toArray()
                    );
                })
                .verifyComplete();
    }

    @Test
    public void testStructure() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.isValid(Mockito.anyInt())).thenReturn(true);

        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(Mockito.any())).thenReturn(true);
        doNothing().when(mockStatement).close();

        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockStatement.executeQuery(Mockito.anyString())).thenReturn(mockResultSet, mockResultSet, mockResultSet);
        when(mockResultSet.next())
                .thenReturn(true, true, true, true, true, true, true, true, false)
                .thenReturn(true, true, false)
                .thenReturn(true, false);
        when(mockResultSet.getString("kind")).thenReturn("r", "r", "r", "r", "r", "r", "r", "r");
        when(mockResultSet.getString("schema_name")).thenReturn("public", "public", "public", "public", "public",
                "public", "public", "public");
        when(mockResultSet.getString("table_name")).thenReturn("campus", "campus", "possessions", "possessions",
                "possessions", "users", "users", "users");
        when(mockResultSet.getString("name")).thenReturn("id", "name", "id", "title", "user_id", "id", "username",
                "password");
        when(mockResultSet.getString("column_type")).thenReturn("timestamptz", "timestamptz", "int4", "varchar",
                "int4", "int4", "varchar", "varchar");
        when(mockResultSet.getString("default_expr")).thenReturn("now()", "now()", null, null, null, "\"identity\"" +
                "(101507, 0, '1,1'::text)", null, null);
        when(mockResultSet.getString("constraint_name"))
                .thenReturn("possessions_pkey", "users_pkey")
                .thenReturn("user_fk");
        when(mockResultSet.getString("constraint_type"))
                .thenReturn("p", "p")
                .thenReturn("f");
        when(mockResultSet.getString("self_schema"))
                .thenReturn("public", "public")
                .thenReturn("public");
        when(mockResultSet.getString("self_table"))
                .thenReturn("possessions", "users")
                .thenReturn("possessions");
        when(mockResultSet.getString("self_column"))
                .thenReturn("id", "id")
                .thenReturn("user_id");
        when(mockResultSet.getString("foreign_schema")).thenReturn("public");
        when(mockResultSet.getString("foreign_table")).thenReturn("users");
        when(mockResultSet.getString("foreign_column")).thenReturn("id");

        doNothing().when(mockResultSet).close();

        ResultSetMetaData mockResultSetMetaData = mock(ResultSetMetaData.class);
        when(mockResultSet.getMetaData()).thenReturn(mockResultSetMetaData);

        when(mockResultSetMetaData.getColumnCount()).thenReturn(6);
        when(mockResultSetMetaData.getColumnTypeName(Mockito.anyInt())).thenReturn("int4");
        when(mockResultSetMetaData.getColumnName(Mockito.anyInt())).thenReturn("user_id");

        DatasourceConfiguration dsConfig = createDatasourceConfiguration();
        Mono<Connection> dsConnectionMono = Mono.just(mockConnection);
        Mono<DatasourceStructure> structureMono = dsConnectionMono
                .flatMap(connection -> pluginExecutor.getStructure(connection, dsConfig));

        StepVerifier.create(structureMono)
                .assertNext(structure -> {
                    assertNotNull(structure);
                    assertEquals(3, structure.getTables().size());

                    final DatasourceStructure.Table campusTable = structure.getTables().get(0);
                    assertEquals("public.campus", campusTable.getName());
                    assertEquals(DatasourceStructure.TableType.TABLE, campusTable.getType());
                    assertArrayEquals(
                            new DatasourceStructure.Column[]{
                                    new DatasourceStructure.Column("id", "timestamptz", "now()"),
                                    new DatasourceStructure.Column("name", "timestamptz", "now()")
                            },
                            campusTable.getColumns().toArray()
                    );
                    assertEquals(campusTable.getKeys().size(), 0);

                    final DatasourceStructure.Table possessionsTable = structure.getTables().get(1);
                    assertEquals("public.possessions", possessionsTable.getName());
                    assertEquals(DatasourceStructure.TableType.TABLE, possessionsTable.getType());
                    assertArrayEquals(
                            new DatasourceStructure.Column[]{
                                    new DatasourceStructure.Column("id", "int4", null),
                                    new DatasourceStructure.Column("title", "varchar", null),
                                    new DatasourceStructure.Column("user_id", "int4", null),
                            },
                            possessionsTable.getColumns().toArray()
                    );

                    final DatasourceStructure.PrimaryKey possessionsPrimaryKey = new DatasourceStructure.PrimaryKey("possessions_pkey", new ArrayList<>());
                    possessionsPrimaryKey.getColumnNames().add("id");
                    final DatasourceStructure.ForeignKey possessionsUserForeignKey = new DatasourceStructure.ForeignKey(
                            "user_fk",
                            List.of("user_id"),
                            List.of("users.id")
                    );
                    assertArrayEquals(
                            new DatasourceStructure.Key[]{possessionsPrimaryKey, possessionsUserForeignKey},
                            possessionsTable.getKeys().toArray()
                    );

                    assertArrayEquals(
                            new DatasourceStructure.Template[]{
                                    new DatasourceStructure.Template("SELECT", "SELECT * FROM public.\"possessions\" LIMIT 10;"),
                                    new DatasourceStructure.Template("INSERT", "INSERT INTO public.\"possessions\" " +
                                            "(\"id\", \"title\", \"user_id\")\n  VALUES (1, '', 1);"),
                                    new DatasourceStructure.Template("UPDATE", "UPDATE public.\"possessions\" SET\n" +
                                            "    \"id\" = 1\n" +
                                            "    \"title\" = ''\n" +
                                            "    \"user_id\" = 1\n" +
                                            "  WHERE 1 = 0; -- Specify a valid condition here. Removing the condition may update every row in the table!"),
                                    new DatasourceStructure.Template("DELETE", "DELETE FROM public.\"possessions\"\n" +
                                            "  WHERE 1 = 0; -- Specify a valid condition here. Removing the condition may delete everything in the table!"),
                            },
                            possessionsTable.getTemplates().toArray()
                    );

                    final DatasourceStructure.Table usersTable = structure.getTables().get(2);
                    assertEquals("public.users", usersTable.getName());
                    assertEquals(DatasourceStructure.TableType.TABLE, usersTable.getType());
                    assertArrayEquals(
                            new DatasourceStructure.Column[]{
                                    new DatasourceStructure.Column("id", "int4", "\"identity\"(101507, " +
                                            "0, '1,1'::text)"),
                                    new DatasourceStructure.Column("username", "varchar", null),
                                    new DatasourceStructure.Column("password", "varchar", null)
                            },
                            usersTable.getColumns().toArray()
                    );

                    final DatasourceStructure.PrimaryKey usersPrimaryKey = new DatasourceStructure.PrimaryKey("users_pkey", new ArrayList<>());
                    usersPrimaryKey.getColumnNames().add("id");
                    assertArrayEquals(
                            new DatasourceStructure.Key[]{usersPrimaryKey},
                            usersTable.getKeys().toArray()
                    );

                    assertArrayEquals(
                            new DatasourceStructure.Template[]{
                                    new DatasourceStructure.Template("SELECT", "SELECT * FROM public.\"users\" LIMIT 10;"),
                                    new DatasourceStructure.Template("INSERT", "INSERT INTO public.\"users\" (\"username\", \"password\")\n" +
                                            "  VALUES ('', '');"),
                                    new DatasourceStructure.Template("UPDATE", "UPDATE public.\"users\" SET\n" +
                                            "    \"username\" = ''\n" +
                                            "    \"password\" = ''\n" +
                                            "  WHERE 1 = 0; -- Specify a valid condition here. Removing the condition may update every row in the table!"),
                                    new DatasourceStructure.Template("DELETE", "DELETE FROM public.\"users\"\n" +
                                            "  WHERE 1 = 0; -- Specify a valid condition here. Removing the condition may delete everything in the table!"),
                            },
                            usersTable.getTemplates().toArray()
                    );
                })
                .verifyComplete();
    }
}