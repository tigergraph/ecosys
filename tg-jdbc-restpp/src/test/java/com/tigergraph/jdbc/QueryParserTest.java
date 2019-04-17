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
 */
public class QueryParserTest extends TestCase {

	public QueryParserTest(String name) {
		super(name);
	}
	
	public void testFormat() throws Exception {
    String query = "get Block(limit=?)";
    Map<String, Object> parameters = new HashMap<>(10);
    parameters.put("0", "000000005d1ee9f633d16642d1bd8af362c3e49e7ce819937ebe6873aaa8c7b7");
    StringBuilder sb = new StringBuilder();
		QueryParser parser = new QueryParser(query, parameters, Boolean.FALSE);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get Block(filter=?)";
    parameters.clear();
    parameters.put("0", "height=1");
		parser = new QueryParser(query, parameters, Boolean.FALSE);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get edges(Block, ?)";
    parameters.clear();
    parameters.put("0", "000000005d1ee9f633d16642d1bd8af362c3e49e7ce819937ebe6873aaa8c7b7");
		parser = new QueryParser(query, parameters, Boolean.FALSE);
    sb.append(parser.getEndpoint()).append("\n");

    query = "get edge(Block, ?, chain, Block, ?)";
    parameters.clear();
    parameters.put("0", "000000005d1ee9f633d16642d1bd8af362c3e49e7ce819937ebe6873aaa8c7b7");
    parameters.put("1", "00000000184d61d43e667b4aebe6224e0a3265a2be87048b7924d7339de6095d");
		parser = new QueryParser(query, parameters, Boolean.FALSE);
    sb.append(parser.getEndpoint()).append("\n");

    query = "run pageRank(maxChange=?, maxIteration=?, dumpingFactor=?)";
    parameters.clear();
    parameters.put("0", "0.001");
    parameters.put("1", 10);
    parameters.put("2", "0.15");
		parser = new QueryParser(query, parameters, Boolean.FALSE);
    sb.append(parser.getEndpoint()).append("\n");

    query = "run pageRank(maxChange=?, maxIteration=?, dumpingFactor=?)";
		String formattedResult = sb.toString();
		InputStream expected = 
			getClass().getClassLoader().getResourceAsStream("endpoint-expected.dat");
		assertEquals(IOUtils.toString(expected), formattedResult);
	}
}
