package com.tigergraph.connector.jdbc;
import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class JdbcTGWriter
{
    private TGJdbcConfig tgjdbc_config = null;
    private String url;
    private StringBuffer StrBuf = null;
    private long msg_count = 0;
    private boolean force_flush;

    /**
     * Constructor for objects of class JdbcTGWriter.
     * @param TGJdbcConfig
     */
    public JdbcTGWriter(TGJdbcConfig tgjdbc_config) throws Exception
    {
        this.tgjdbc_config = tgjdbc_config;
        this.url = tgjdbc_config.tg_url + "/ddl/" +
            tgjdbc_config.tg_graph +
            "?tag=" + tgjdbc_config.tg_loadjob +
            "&sep=" + tgjdbc_config.tg_separator_url +
            "&eol=" + tgjdbc_config.tg_eol_url;
        this.StrBuf = new StringBuffer();
        this.force_flush = false;
        System.out.println(this.url);
    }


    public void BatchPost(String data_string) throws Exception
    {
      try
      {
        if (data_string.length() > 0) {
            this.StrBuf.append(data_string+this.tgjdbc_config.tg_eol_ascii);
            msg_count++;
        }

        if (msg_count == this.tgjdbc_config.tg_batch_size ||
            force_flush == true) {

            String urlParameters  = StrBuf.toString();
            byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
            int    postDataLength = postData.length;

            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setDoOutput( true );
            conn.setInstanceFollowRedirects( false );
            conn.setRequestMethod( "POST" );
            conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty( "charset", "utf-8");
            conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
            conn.setUseCaches( false );
            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                wr.write( postData );
            }

            InputStreamReader InStrmR = new InputStreamReader(conn.getInputStream());

            // /* Debug: check the return */
            // Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            // for (int c; (c = in.read()) >= 0;)
            //     System.out.print((char)c);
            //     System.out.println();

            conn.disconnect();

            /** Reset for next round */
            this.msg_count = 0;
            this.StrBuf.setLength(0);
        }
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
    }

    /**
     * make sure all the buffered data are sent
     */
    public void close() throws Exception
    {
        this.force_flush = true;
        BatchPost("");
    }

}

