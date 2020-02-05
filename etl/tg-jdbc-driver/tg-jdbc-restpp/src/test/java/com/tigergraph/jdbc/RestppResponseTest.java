package com.tigergraph.jdbc;

import com.tigergraph.jdbc.restpp.driver.RestppResponse;

import static org.junit.Assert.assertTrue;
import org.apache.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

/**
 * Unit test for RestppResponse.
 * The corresponding TigerGraph demo could be found at:
 * https://docs.tigergraph.com/dev/gsql-examples/common-applications#example-2-page-rank
 */
public class RestppResponseTest extends TestCase {

	public RestppResponseTest(String name) {
		super(name);
	}
	
	public void testFormat() throws Exception {
		InputStream inputStream = 
			getClass().getClassLoader().getResourceAsStream("response.xml");
    String content = IOUtils.toString(inputStream, "utf-8");
		RestppResponse restpp = new RestppResponse();
    restpp.parse(content);
		String formattedResult = restpp.toString();
		InputStream expected = 
			getClass().getClassLoader().getResourceAsStream("response-expected.dat");
		assertEquals(IOUtils.toString(expected), formattedResult);
	}
}
