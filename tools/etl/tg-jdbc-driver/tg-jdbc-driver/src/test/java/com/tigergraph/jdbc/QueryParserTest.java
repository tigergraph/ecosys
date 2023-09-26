package com.tigergraph.jdbc;

import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

/** Unit test for QueryParser */
public class QueryParserTest {

  @Test
  public void testTokenize() throws Exception {
    TGLoggerFactory.initializeLogger(1, null);
    List<String> positiveCases = new ArrayList<>();
    positiveCases.add("run query(a='a',b=1)");
    positiveCases.add("run query(a='a''\"b''',b=1)");
    positiveCases.add("run query(a='',b='')");
    positiveCases.add("run query(a=,b=)");
    positiveCases.add("get vertex(id=?) params (filter='age>1,gender=\"ma '' le\"')");
    positiveCases.add("run query(a=?,b='?')");
    List<String> expected = new ArrayList<>();
    expected.add("[run, query, a, 'a', b, 1]");
    expected.add("[run, query, a, 'a'\"b'', b, 1]");
    expected.add("[run, query, a, '', b, '']");
    expected.add("[run, query, a, b]");
    expected.add("[get, vertex, id, ?, params, filter, 'age>1,gender=\"ma ' le\"']");
    expected.add("[run, query, a, ?, b, '?']");
    for (int i = 0; i < positiveCases.size(); i++) {
      assertEquals(expected.get(i), Arrays.toString(QueryParser.tokenize(positiveCases.get(i))));
    }

    List<String> negativeCases = new ArrayList<>();
    negativeCases.add("run query(a=''a',b=1)");
    negativeCases.add("run query(a='a''\"b'',b=1)");
    negativeCases.add("run query(a='a ' c=1 ',b=1)");
    for (int i = 0; i < negativeCases.size(); i++) {
      final int idx = i;
      assertThrows(SQLException.class, () -> QueryParser.tokenize(negativeCases.get(idx)));
    }
  }

  @Test
  public void testFormat() throws Exception {
    TGLoggerFactory.initializeLogger(1, null);

    String query = "get vertex(person) params(filter='gender=\"female\",age<27', select=?)";
    Map<Integer, Object> parameters = new HashMap<>(10);
    parameters.put(1, "name,age,gender");
    StringBuilder sb = new StringBuilder();
    QueryParser parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "get vertex(Page) params(limit=?)";
    parameters = new HashMap<>(10);
    parameters.put(1, "3");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "get vertex(Page) params(filter=?)";
    parameters.clear();
    parameters.put(1, "page_id=1");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "get edge(Page, ?)";
    parameters.clear();
    parameters.put(1, "2");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "get edge(Page, ?, Linkto)";
    parameters.clear();
    parameters.put(1, "2");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "get edge(Page, ?, Linkto, Page)";
    parameters.clear();
    parameters.put(1, "2");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "get edge(Page, ?, Linkto, Page, ?)";
    parameters.clear();
    parameters.put(1, "2");
    parameters.put(2, "3");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)";
    parameters.clear();
    parameters.put(1, "0.001");
    parameters.put(2, 10);
    parameters.put(3, "0.15");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "run hello(str1='abc +''def', str2='?', str3=?)";
    parameters.clear();
    parameters.put(1, "''");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "status jobid(jdbc.local.12345.aaa)";
    parser = new QueryParser(null, query, null, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "status jobid(?)";
    parameters.clear();
    parameters.put(1, "jdbc.local.12345.aaa");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query = "select * from jobid jdbc.local.12345.aaa where xxx";
    parser = new QueryParser(null, query, null, 0, 0, true);
    sb.append(parser.getEndpoint()).append(System.lineSeparator());

    query =
        "INSERT INTO vertex Page(id, name, 'page rank', page_id, is_active) VALUES('1000', 'new"
            + " page', 0.8, 1000, TRue)";
    parser = new QueryParser(null, query, null, 0, 0, true);
    sb.append(parser.getVertexJson()).append(System.lineSeparator());

    query =
        "INSERT INTO vertex Page(id, name, 'page rank', page_id, is_active) VALUES(?, ?, ?, ?, ?)";
    parameters.clear();
    parameters.put(1, "1000");
    parameters.put(2, "new page");
    parameters.put(3, 0.8);
    parameters.put(4, 1000);
    parameters.put(5, Boolean.FALSE);
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getVertexJson()).append(System.lineSeparator());

    query =
        "INSERT INTO edge Linkto(Page, Page, weight, is_active) VALUES('1000', '1001', 10.7,"
            + " FaLse)";
    parser = new QueryParser(null, query, null, 0, 1, true);
    sb.append(parser.getEdgeJson()).append(System.lineSeparator());

    query = "INSERT INTO edge Linkto(Page, Page, weight, is_active) VALUES(?, ?, ?, ?)";
    parameters.clear();
    parameters.put(1, "1000");
    parameters.put(2, "1001");
    parameters.put(3, 10.7);
    parameters.put(4, Boolean.TRUE);
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getEdgeJson()).append(System.lineSeparator());

    query = "builtins stat_vertex_number(type=?)";
    parameters.clear();
    parameters.put(1, "Page");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getPayload()).append(System.lineSeparator());

    query = "builtins stat_edge_number(type=?)";
    parameters.clear();
    parameters.put(1, "Linkto");
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getPayload()).append(System.lineSeparator());

    query = "find shortestpath(person,?,post,?)";
    parameters.clear();
    parameters.put(1, "Tom");
    parameters.put(2, 112);
    parser = new QueryParser(null, query, parameters, 0, 0, true);
    sb.append(parser.getPayload()).append(System.lineSeparator());

    query = "find allpaths(person,Jack,post,345,5)";
    parser = new QueryParser(null, query, null, 0, 0, true);
    sb.append(parser.getPayload()).append(System.lineSeparator());

    String formattedResult = sb.toString();
    InputStream expected = getClass().getClassLoader().getResourceAsStream("endpoint-expected.dat");
    String expectedString =
        IOUtils.toString(expected).replaceAll("\\n|\\r\\n", System.lineSeparator());
    assertEquals(expectedString, formattedResult);
  }
}
