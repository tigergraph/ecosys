package com.tigergraph.jdbc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import org.junit.jupiter.api.*;
import java.util.Properties;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Base64;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.sql.SQLException;

// ###################################################
// #     test request url, type and paload           #
// ###################################################
// get expected request from https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints
// convert json payload to string by https://jsontostring.com

public class RequestTest {
  static WireMockServer wireMockServer;
  static Connection con;
  static Properties properties;

  @BeforeAll
  static void prepare() throws Exception {
    TGLoggerFactory.initializeLogger(1);
    // Mock server for RESTPP
    wireMockServer = new WireMockServer(options().dynamicPort());
    wireMockServer.start();
    // always return a correct response to avoid exceptions
    wireMockServer.stubFor(any(anyUrl())
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"error\":false,\"results\":[{\"vertexName\":\"Person\"}]}")));

    // Connection to tg
    properties = new Properties();
    String ipAddr = "127.0.0.1";
    Integer port = wireMockServer.port();
    properties.put("username", "tigergraph");
    properties.put("password", "tigergraph");
    properties.put("graph", "social");
    properties.put("debug", "0");
    properties.put("filename", "file1");
    properties.put("sep", ",");
    properties.put("eol", "\n");
    StringBuilder sb = new StringBuilder();
    sb.append("jdbc:tg:http://").append(ipAddr).append(":").append(port);

    com.tigergraph.jdbc.Driver driver = new Driver();
    try {
      con = driver.connect(sb.toString(), properties);
    } catch (Exception e) {
      // ignore response error info
    }
  }

  @AfterAll
  static void release() {
    wireMockServer.stop();
  }

  @Test
  @DisplayName("Should get token")
  void sendAuthRequest() throws SQLException, InterruptedException {
    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/restpp/requesttoken"))
        .withHeader("Authorization",
            equalTo("Basic " + new String(Base64.getEncoder()
                .encode(("tigergraph:tigergraph").getBytes()))))
        .withRequestBody(equalToJson("{\"graph\":\"social\"}")));
  }

  @Test
  @DisplayName("Should be builtin requests")
  void sendBuiltinRequest() throws SQLException, InterruptedException {
    Statement stmt = con.createStatement();
    String query = "builtins stat_vertex_number(type=Linkto)";
    stmt.executeQuery(query);

    query = "builtins stat_vertex_number(type=?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "Linkto");
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }

    wireMockServer.verify(2, postRequestedFor(urlEqualTo("/restpp/builtins/social"))
        .withRequestBody(equalToJson("{\"function\":\"stat_vertex_number\",\"type\":\"Linkto\"}")));
  }

  @Test
  @DisplayName("Should be interpreted requests")
  void sendInterpretedRequest() throws SQLException, InterruptedException {
    String query = "run interpreted(a=?, b=?)";
    String query_body = "INTERPRET QUERY (int a, int b) FOR GRAPH social {\n"
        + "PRINT a, b;\n"
        + "}\n";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "10");
    pstmt.setString(2, "20");
    pstmt.setString(3, query_body); // The query body is passed as a parameter.
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }

    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/gsqlserver/interpreted_query?a=10&b=20"))
        .withRequestBody(equalTo("INTERPRET QUERY (int a, int b) FOR GRAPH social {\n"
            + "PRINT a, b;\n"
            + "}\n")));
  }

  @Test
  @DisplayName("Should be pre-installed requests")
  void sendInstalledRequest() throws SQLException, InterruptedException {
    String query = "RUN tg_pagerank(v_type=?, e_type=? max_change=?," +
        "max_iter=?, damping=?, top_k=?, print_accum=?," +
        "result_attr=?, file_path=?, display_edges=?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "person");
    pstmt.setString(2, "friendship");
    pstmt.setString(3, "0.001");
    pstmt.setInt(4, 25);
    pstmt.setString(5, "0.85");
    pstmt.setInt(6, 3);
    pstmt.setBoolean(7, true);
    pstmt.setString(8, "");
    pstmt.setString(9, "");
    pstmt.setBoolean(10, false);
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }

    wireMockServer.verify(1, getRequestedFor(urlEqualTo(
        "/restpp/query/social/tg_pagerank?v_type=person&e_type=friendship&max_change=0.001&max_iter=25&damping=0.85&top_k=3&print_accum=true&result_attr=&file_path=&display_edges=false")));
  }

  @Test
  @DisplayName("Should be loading job requests")
  void sendLoadingRequest() throws SQLException, InterruptedException {
    String query = "INSERT INTO job load_social(line) VALUES(?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "a1,b,c");
    pstmt.addBatch();
    pstmt.setString(1, "a2,b,c");
    pstmt.addBatch();
    pstmt.setString(1, "a3,b,c");
    pstmt.addBatch();
    try {
      pstmt.executeBatch();
    } catch (Exception e) {
      // ignore response error info
    }

    wireMockServer.verify(1,
        postRequestedFor(urlEqualTo("/restpp/ddl/social?tag=load_social&filename=file1&sep=%2C&eol=%0A"))
            .withRequestBody(equalTo("a1,b,c\na2,b,c\na3,b,c")));
  }

  // For spark data frame with null fields, it will invoke setNull to send null value to jdbc
  // it should be converted to empty csv field instead of "null"
  @Test
  @DisplayName("Should be loading job requests with empty attributes")
  void sendLoadingRequestWithNullParms() throws SQLException, InterruptedException {
    String query = "INSERT INTO job load_social(id,a,b,c) VALUES(?,?,?,?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setNull(1, 0);
    pstmt.setNull(2, 0);
    pstmt.setNull(3, 0);
    pstmt.setNull(4, 0);
    pstmt.addBatch();
    try {
      pstmt.executeBatch();
    } catch (Exception e) {
      // ignore response error info
    }

    wireMockServer.verify(1,
        postRequestedFor(urlEqualTo("/restpp/ddl/social?tag=load_social&filename=file1&sep=%2C&eol=%0A"))
            .withRequestBody(equalTo(",,,")));
  }

  @Test
  @DisplayName("Should be get edge requests")
  void sendGetEdgeRequest() throws SQLException, InterruptedException {
    String query = "get edge(person, ?, friendship, person, ?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "Jerry");
    pstmt.setString(2, "Tom");
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }

    wireMockServer.verify(1,
        getRequestedFor(urlEqualTo("/restpp/graph/social/edges/person/Jerry/friendship/person/Tom")));
  }

  @Test
  @DisplayName("Should be get edges requests")
  void sendGetEdgesRequest() throws SQLException, InterruptedException {
    String query = "get edge(Page, ?, Linkto, Page)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "50");
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }
    wireMockServer.verify(1, getRequestedFor(urlEqualTo("/restpp/graph/social/edges/Page/50/Linkto/Page")));
  }

  @Test
  @DisplayName("Should be get vertex requests")
  void sendGetVertexRequest() throws SQLException, InterruptedException {
    String query = "get vertex(Person) params(limit=?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "50");
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }
    wireMockServer.verify(1, getRequestedFor(urlEqualTo("/restpp/graph/social/vertices/Person?limit=50")));
  }

  @Test
  @DisplayName("Should be insert vertex requests")
  void sendInsertRequest() throws SQLException, InterruptedException {
    String query = "INSERT INTO vertex person(name, name, age, gender, state ) VALUES(?, ?, ?, ?, ?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "tom");
    pstmt.setString(2, "tom");
    pstmt.setInt(3, 1);
    pstmt.setString(4, "Female");
    pstmt.setString(5, "ny");
    pstmt.addBatch();
    pstmt.setString(1, "jerry");
    pstmt.setString(2, "jerry");
    pstmt.setInt(3, 1);
    pstmt.setString(4, "Male");
    pstmt.setString(5, "la");
    pstmt.addBatch();
    try {
      pstmt.executeBatch();
    } catch (Exception e) {
      // ignore response error info
    }
    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/restpp/graph/social"))
        .withRequestBody(equalToJson(
            "{\"vertices\":{\"person\":{\"tom\":{\"name\":{\"value\":\"tom\"},\"age\":{\"value\":1},\"gender\":{\"value\":\"Female\"},\"state\":{\"value\":\"ny\"}}},\"person\":{\"jerry\":{\"name\":{\"value\":\"jerry\"},\"age\":{\"value\":1},\"gender\":{\"value\":\"Male\"},\"state\":{\"value\":\"la\"}}}}}")));
  }

  @Test
  @DisplayName("Should be insert edge requests")
  void sendInsertEdgeRequest() throws SQLException, InterruptedException {
    String query = "INSERT INTO edge Linkto(Page, Page, Attr) VALUES(?, ?, ?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setInt(1, 10);
    pstmt.setInt(2, 20);
    pstmt.setString(3, "red");
    pstmt.addBatch();
    pstmt.setInt(1, 20);
    pstmt.setInt(2, 30);
    pstmt.setString(3, "black");

    pstmt.addBatch();
    try {
      pstmt.executeBatch();
    } catch (Exception e) {
      // ignore response error info
    }
    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/restpp/graph/social"))
        .withRequestBody(equalToJson(
            "{\"edges\":{\"Page\":{\"10\":{\"Linkto\":{\"Page\":{\"20\":{\"Attr\":{\"value\":\"red\"}}}}}},\"Page\":{\"20\":{\"Linkto\":{\"Page\":{\"30\":{\"Attr\":{\"value\":\"black\"}}}}}}}}")));
  }

  @Test
  @DisplayName("Should be delete vertex requests")
  void sendDeleteVertexRequest() throws SQLException, InterruptedException {
    String query = "DELETE vertex(person, ?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "Tom");
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }
    wireMockServer.verify(1, deleteRequestedFor(urlEqualTo("/restpp/graph/social/vertices/person/Tom")));
  }

  @Test
  @DisplayName("Should be delete edge requests")
  void sendDeleteEdgeRequest() throws SQLException, InterruptedException {
    String query = "DELETE edge(person, ?, friendship, person, ?)";
    PreparedStatement pstmt = con.prepareStatement(query);
    pstmt.setString(1, "Bob");
    pstmt.setString(2, "Tom");
    try {
      pstmt.executeQuery();
    } catch (Exception e) {
      // ignore response error info
    }
    wireMockServer.verify(1,
        deleteRequestedFor(urlEqualTo("/restpp/graph/social/edges/person/Bob/friendship/person/Tom")));
  }
}