package com.tigergraph.jdbc;

import com.tigergraph.jdbc.restpp.driver.QueryParser;

import static org.junit.Assert.assertTrue;
import org.apache.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

/**
 * Unit test for QueryParser
 * The corresponding TigerGraph demo could be found at:
 * https://docs.tigergraph.com/dev/gsql-examples/common-applications#example-2-page-rank
 */
public class QueryParserTest extends TestCase {

  public QueryParserTest(String name) {
    super(name);
  }

  public void testFormat() throws Exception {
    String query = "get Page(limit=?)";
    Map<Integer, Object> parameters = new HashMap<>(10);
    parameters.put(1, "3");
    StringBuilder sb = new StringBuilder();
    QueryParser parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get Page(filter=?)";
    parameters.clear();
    parameters.put(1, "page_id=1");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get edges(Page, ?)";
    parameters.clear();
    parameters.put(1, "2");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get edges(Page, ?, Linkto)";
    parameters.clear();
    parameters.put(1, "2");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get edges(Page, ?, Linkto, Page)";
    parameters.clear();
    parameters.put(1, "2");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get edge(Page, ?, Linkto, Page, ?)";
    parameters.clear();
    parameters.put(1, "2");
    parameters.put(2, "3");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEndpoint()).append("\n");

    query = "run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)";
    parameters.clear();
    parameters.put(1, "0.001");
    parameters.put(2, 10);
    parameters.put(3, "0.15");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEndpoint()).append("\n");

    query = "INSERT INTO vertex Page(id, name, 'page rank', page_id, is_active) VALUES('1000', 'new page', 0.8, 1000, TRue)";
    parser = new QueryParser(null, query, null, 0, 0);
    sb.append(parser.getVertexJson()).append("\n");

    query = "INSERT INTO vertex Page(id, name, 'page rank', page_id, is_active) VALUES(?, ?, ?, ?, ?)";
    parameters.clear();
    parameters.put(1, "1000");
    parameters.put(2, "new page");
    parameters.put(3, 0.8);
    parameters.put(4, 1000);
    parameters.put(5, Boolean.FALSE);
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getVertexJson()).append("\n");

    query = "INSERT INTO edge Linkto(Page, Page, weight, is_active) VALUES('1000', '1001', 10.7, FaLse)";
    parser = new QueryParser(null, query, null, 0, 0);
    sb.append(parser.getEdgeJson()).append("\n");

    query = "INSERT INTO edge Linkto(Page, Page, weight, is_active) VALUES(?, ?, ?, ?)";
    parameters.clear();
    parameters.put(1, "1000");
    parameters.put(2, "1001");
    parameters.put(3, 10.7);
    parameters.put(4, Boolean.TRUE);
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getEdgeJson()).append("\n");

    query = "builtins stat_vertex_number(type=?)";
    parameters.clear();
    parameters.put(1, "Page");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getPayload()).append("\n");

    query = "builtins stat_edge_number(type=?)";
    parameters.clear();
    parameters.put(1, "Linkto");
    parser = new QueryParser(null, query, parameters, 0, 0);
    sb.append(parser.getPayload()).append("\n");

    String formattedResult = sb.toString();
    InputStream expected =
      getClass().getClassLoader().getResourceAsStream("endpoint-expected.dat");
    assertEquals(IOUtils.toString(expected), formattedResult);
  }
}
