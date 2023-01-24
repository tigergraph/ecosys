package com.tigergraph.jdbc;

import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Unit test for QueryParser */
public class QueryParserTest extends TestCase {

  public QueryParserTest(String name) {
    super(name);
  }

  public void testFormat() throws Exception {
    TGLoggerFactory.initializeLogger(1);

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
