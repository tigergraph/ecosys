package com.tigergraph.jdbc;

import com.tigergraph.jdbc.restpp.RestppResultSet;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import com.tigergraph.jdbc.restpp.driver.QueryType;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Unit test for RestppResultSetTest. */
public class RestppResultSetTest extends TestCase {

  public RestppResultSetTest(String name) {
    super(name);
  }

  public void testParseResult() throws Exception {
    TGLoggerFactory.initializeLogger(1);
    List<JSONObject> resultList = new ArrayList<>();
    List<String> field_list = new ArrayList<>();

    /** parse vertex */
    InputStream inVertex = getClass().getClassLoader().getResourceAsStream("resultset-vertex.dat");
    String[] vertexStrArray = IOUtils.toString(inVertex).split(System.lineSeparator());

    for (String vertexStr : vertexStrArray) {
      JSONObject obj = new JSONObject(vertexStr);
      resultList.add(obj);
    }

    RestppResultSet vertexRs =
        new RestppResultSet(
            null, resultList, field_list, QueryType.QUERY_TYPE_GRAPH_GET_VERTEX, false);
    StringBuilder vertexStrBuiler = new StringBuilder();
    do {
      java.sql.ResultSetMetaData metaData = vertexRs.getMetaData();
      vertexStrBuiler.append("Table: " + metaData.getCatalogName(1)).append(System.lineSeparator());
      vertexStrBuiler.append(metaData.getColumnName(1));

      for (int i = 2; i <= metaData.getColumnCount(); ++i) {
        vertexStrBuiler.append("\t" + metaData.getColumnName(i));
      }
      vertexStrBuiler.append(System.lineSeparator());
      while (vertexRs.next()) {
        vertexStrBuiler.append(vertexRs.getObject(1));
        for (int i = 2; i <= metaData.getColumnCount(); ++i) {
          Object obj = vertexRs.getObject(i);
          vertexStrBuiler.append("\t" + String.valueOf(obj));
        }
        vertexStrBuiler.append(System.lineSeparator());
        ;
      }
    } while (!vertexRs.isLast());

    InputStream inExpectedVertex =
        getClass().getClassLoader().getResourceAsStream("resultset-vertex-expected.dat");
    String expectedStr =
        IOUtils.toString(inExpectedVertex).replaceAll("\\n|\\r\\n", System.lineSeparator());
    assertEquals(expectedStr, vertexStrBuiler.toString());

    inVertex.close();
    inExpectedVertex.close();
    resultList.clear();

    /** parse edge */
    InputStream inEdge = getClass().getClassLoader().getResourceAsStream("resultset-edge.dat");
    String[] edgeStrArray = IOUtils.toString(inEdge).split(System.lineSeparator());
    inEdge.close();

    for (String edgeStr : edgeStrArray) {
      JSONObject obj = new JSONObject(edgeStr);
      resultList.add(obj);
    }

    RestppResultSet edgeRs =
        new RestppResultSet(
            null, resultList, field_list, QueryType.QUERY_TYPE_GRAPH_GET_EDGE, false);
    StringBuilder edgeStrBuiler = new StringBuilder();
    do {
      java.sql.ResultSetMetaData metaData = edgeRs.getMetaData();
      edgeStrBuiler.append("Table: " + metaData.getCatalogName(1)).append(System.lineSeparator());
      edgeStrBuiler.append(metaData.getColumnName(1));

      for (int i = 2; i <= metaData.getColumnCount(); ++i) {
        edgeStrBuiler.append("\t" + metaData.getColumnName(i));
      }
      edgeStrBuiler.append(System.lineSeparator());
      while (edgeRs.next()) {
        edgeStrBuiler.append(edgeRs.getObject(1));
        for (int i = 2; i <= metaData.getColumnCount(); ++i) {
          Object obj = edgeRs.getObject(i);
          edgeStrBuiler.append("\t" + String.valueOf(obj));
        }
        edgeStrBuiler.append(System.lineSeparator());
        ;
      }
    } while (!edgeRs.isLast());

    InputStream inExpectedEdge =
        getClass().getClassLoader().getResourceAsStream("resultset-edge-expected.dat");
    String expectedEdgeStr =
        IOUtils.toString(inExpectedEdge).replaceAll("\\n|\\r\\n", System.lineSeparator());
    assertEquals(expectedEdgeStr, edgeStrBuiler.toString());

    inEdge.close();
    inExpectedEdge.close();
    resultList.clear();

    /** parse path */
    InputStream inPath = getClass().getClassLoader().getResourceAsStream("resultset-path.dat");
    String[] pathStrArray = {IOUtils.toString(inPath)};
    inPath.close();

    for (String pathStr : pathStrArray) {
      JSONObject obj = new JSONObject(pathStr);
      resultList.add(obj);
    }

    RestppResultSet pathRs =
        new RestppResultSet(null, resultList, field_list, QueryType.QUERY_TYPE_ALLPATHS, false);
    StringBuilder pathStrBuiler = new StringBuilder();
    do {
      java.sql.ResultSetMetaData metaData = pathRs.getMetaData();
      pathStrBuiler.append("Table: " + metaData.getCatalogName(1)).append(System.lineSeparator());
      pathStrBuiler.append(metaData.getColumnName(1));

      for (int i = 2; i <= metaData.getColumnCount(); ++i) {
        pathStrBuiler.append("\t" + metaData.getColumnName(i));
      }
      pathStrBuiler.append(System.lineSeparator());
      while (pathRs.next()) {
        pathStrBuiler.append(pathRs.getObject(1));
        for (int i = 2; i <= metaData.getColumnCount(); ++i) {
          Object obj = pathRs.getObject(i);
          pathStrBuiler.append("\t" + String.valueOf(obj));
        }
        pathStrBuiler.append(System.lineSeparator());
        ;
      }
    } while (!pathRs.isLast());
    InputStream inExpectedPath =
        getClass().getClassLoader().getResourceAsStream("resultset-path-expected.dat");
    String expectedPathStr =
        IOUtils.toString(inExpectedPath).replaceAll("\\n|\\r\\n", System.lineSeparator());
    assertEquals(expectedPathStr, pathStrBuiler.toString());

    inExpectedPath.close();
  }
}
