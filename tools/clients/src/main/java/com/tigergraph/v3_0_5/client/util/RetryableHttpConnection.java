package com.tigergraph.v3_0_5.client.util;

import org.apache.http.client.utils.URIBuilder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tigergraph.v3_0_5.client.util.SystemUtils.logger;

/**
 * A RetryableHttpConnection that takes a list of IPs and ports,
 * tries to set up connection to one of them, and retry if it can't
 * connect successfully.
 * It will try each ip/port up to {@code maxRetry} times.
 */
public class RetryableHttpConnection {

  private static final String LOCAL_HOST = "127.0.0.1";
  private static final int DEFAULT_CONNECTION_TIMEOUT = 300000; // 5 min
  // Interval (seconds) of retry to connect to GSQL server.
  private static final int RETRY_INTERVAL = 1;
  private List<String> ips;
  private List<Integer> ports;
  // max retry per ip/port
  private int maxRetry = 1;
  // rotation index for ip/port, so we can use the one succeed last time
  // to save some time.
  private int index = 0;
  // for SSL
  private SSLContext sslContext = null;
  // default port
  private int defaultPrivatePort = 8123;

  // Path to CA Certificate. {@code null} if not given.
  public RetryableHttpConnection(boolean ssl, String cert, int defaultPort) throws Exception {
    if (ssl) {
      sslContext = cert != null 
        ? getSSLContext(cert) 
        : javax.net.ssl.SSLContext.getDefault();
    }
    ips = new ArrayList<>();
    ports = new ArrayList<>();
    defaultPrivatePort = defaultPort;
  }

  public void setMaxRetry(int maxRetry) {
    this.maxRetry = maxRetry;
  }

  @Override
  public int hashCode() {
    return ips.hashCode();
  }

  public void addIpPort(String ip, int port) {
    try {
      buildUrl(null, ip, port);
      ips.add(ip);
      ports.add(port);
    } catch (URISyntaxException e) {
      logger.error(e);
      SystemUtils.exit(SystemUtils.ExitStatus.WRONG_ARGUMENT_ERROR, "Invalid URL: " + e.getInput());
    } catch (MalformedURLException e) {
      // this does not validate URL and only detects unknown protocol
      logger.error(e);
      SystemUtils.exit(SystemUtils.ExitStatus.RUNTIME_ERROR, "Unknown protocol:%s", e.getMessage());
    }
  }

  public void addLocalHost(int port) {
    addIpPort(LOCAL_HOST, port);
  }

  public boolean isLocal() {
    return ports.size() == 1 && ips.size() == 1
        && ips.get(0).equals(LOCAL_HOST)
        && ports.get(0).equals(defaultPrivatePort);
  }

  public URI getCurrentURI() throws MalformedURLException, URISyntaxException {
    URL url = buildUrl(null, ips.get(index), ports.get(index));
    return url.toURI();
  }

  /**
   * Build the endpoint URL to GSQL server.
   * e.g. http://127.0.0.1:8123/gsql (local)
   *      http://127.0.0.1:8123/gsql/endpoint (local)
   *      https://192.168.0.10:14240/gsqlserver/gsql (remote)
   * @param endpoint the endpoint, null if no endpoint just test gsql-server url is correct
   * @param ip the ip address
   * @param port the port of gsql-server
   * @return the URL to gsql-server with given inputs
   * @throws URISyntaxException, MalformedURLException if given input can't build a valid URL
   */
  private URL buildUrl(String endpoint, String ip, int port)
      throws URISyntaxException, MalformedURLException {

    String baseEndpoint = ip.equals(LOCAL_HOST) && port == defaultPrivatePort
        ? "gsql" : "gsqlserver/gsql";
    String path = endpoint != null
        ? String.join("/", "", baseEndpoint, endpoint)
        : String.join("/", "", baseEndpoint);

    URI uri = new URIBuilder()
        .setScheme(sslContext != null ? "https" : "http")
        .setHost(ip)
        .setPort(port)
        .setPath(path)
        .build();
    return uri.toURL();
  }

  /**
   * Open a http or https connection with given {@code url}
   * @param url the url to open connection with
   * @return the HttpURLConnection or HttpsURLConnection
   * @throws Exception
   */
  private HttpURLConnection openConnection(URL url) throws Exception {
    if (sslContext != null) {
      // open HttpsURLConnection when CA Certificate is given
      HttpsURLConnection connSecured = (HttpsURLConnection) url.openConnection();
      connSecured.setSSLSocketFactory(sslContext.getSocketFactory());
      return connSecured;
    } else {
      return (HttpURLConnection) url.openConnection();
    }
  }

  /**
   * Build the http connection by given inputs with {@code ips} and {@code ports}.
   * Try the {@code ips} and {@code ports} one by one until success.
   * Each ip and port will retry up to {@code maxRetry} times.
   * @param endpoint the gsql-server endpoint
   * @param method GET/POST/PUT
   * @param data the payload, null if no payload
   * @param headers the map of headers, null if no headers
   * @return the HttpURLConnection
   * @throws Exception
   */
  public HttpURLConnection connect(String endpoint, String method, String data,
      Map<String, String> headers) throws Exception {

    logger.info("RetryableHttpConnection: endpoint: %s, data.len: %d, method: %s",
        endpoint, data.length(), method);

    // try all ips
    for (int i = 0; i < ips.size() && !ips.isEmpty(); ++i, ++index) {
      if (index >= ips.size()) {
        index = 0;
      }

      try {
        // 1. build URL
        URL url = buildUrl(endpoint, ips.get(index), ports.get(index));

        // 2. open connection
        HttpURLConnection conn = openConnection(url);
        conn.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);

        // set headers, e.g. user auth token
        if (headers != null) {
          headers.forEach((k, v) -> conn.setRequestProperty(k, v));
        }
        conn.setRequestMethod(method);
        conn.setUseCaches(false);
        conn.setDoInput(true);

        // retry up to maxRetry times
        for (int j = 0; j < maxRetry; ++j) {
          if (j > 0) {
            TimeUnit.SECONDS.sleep(RETRY_INTERVAL);
          }

          try {
            //2. write data to the connection if needed
            if (data != null) {
              conn.setDoOutput(true);

              //Send request
              DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
              wr.writeBytes(data);
              wr.flush();
              wr.close();
            }

            //3. get response
            // gsql-server will only return 200 or 401 (unauthorized)
            int code = conn.getResponseCode();
            if (code == 200 || code == 401) {
              return conn;
            }
            logger.error("code: %d, msg: %s", code, conn.getResponseMessage());
            // retry when there's server error HTTP 5xx
            if (code / 500 == 1) {
              continue;
            }
            // get something else, try next ip/port
            break;
          } catch (IOException e) {
            // silently retry to connect this ip/port
          }
        }

      } catch (URISyntaxException | IOException | InterruptedException e) {
        // got non-retryable exception for an ip/port, log and try next
        logger.error(e);
      }
    }
    logger.error("Unable to connect to all IPs");
    throw new ConnectException();
  }

  /**
   * load the CA and use it in the https connection
   * @param filename the CA filename
   * @return the SSL context
   */
  private SSLContext getSSLContext(String filename) throws Exception {
    try {
      // Load CAs from an InputStream
      // (could be from a resource or ByteArrayInputStream or ...)
      // X.509 is a standard that defines the format of public key certificates, used in TLS/SSL.
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      InputStream caInput = new BufferedInputStream(new FileInputStream(filename));
      Certificate ca = cf.generateCertificate(caInput);

      // Create a KeyStore containing our trusted CAs
      String keyStoreType = KeyStore.getDefaultType();
      KeyStore keyStore = KeyStore.getInstance(keyStoreType);
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca", ca);

      // Create a TrustManager that trusts the CAs in our KeyStore
      String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
      tmf.init(keyStore);

      // Create an SSLContext that uses our TrustManager
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, tmf.getTrustManagers(), null);
      return context;
    } catch (Exception e) {
      throw new Exception("Failed to load the CA file: " + e.getMessage(), e);
    }
  }
}
