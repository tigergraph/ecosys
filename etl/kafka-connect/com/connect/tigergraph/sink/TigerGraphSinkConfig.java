package com.connect.tigergraph.sink;

import java.net.URLEncoder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.http.message.BasicNameValuePair;


public class TigerGraphSinkConfig extends AbstractConfig{
    private static final Logger log = LoggerFactory.getLogger(TigerGraphSinkConfig.class);
    public final List<String> hostList;
    public final String loadingJobname;
    public final char eol='\n';
    public final String paramStr;
    public final String sepStr;
    public final String authToken;
    public final int defaultTimeout;
    public final int flushBatchSize;
    public final int flushTimeout;
    public int curIdx = 0;

    private static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define("host_list", Type.LIST, Importance.HIGH, "list of host ip address and port")
            .define("loading_jobname", Type.STRING, Importance.HIGH, "loading job name")
            .define("default_time_out", Type.INT, 600, Importance.HIGH, "timeout for each post request")
            .define("flush_batch_size", Type.INT, 500, Importance.HIGH, "batch size used to commit post")
            .define("flush_time_out", Type.INT, 60, Importance.HIGH, "timeout used to commit post in flush")
            .define("sep",Type.STRING, ",", Importance.HIGH, "line separator used for csv data")
            .define("auth_token",Type.STRING, "", Importance.HIGH, "token string for validation");

    public static ConfigDef getconfig() {
        return CONFIG_DEF;
    }

    private String getParamStr(List<BasicNameValuePair> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (BasicNameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");
            try {
                result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            } catch (IOException ex) {
                throw new ConnectException("Encode Exception: " + ex.getMessage());
            }
        }

        return result.toString();
    }


    public TigerGraphSinkConfig(Map<String, String> props) {
        super(CONFIG_DEF, props);
        loadingJobname = getString("loading_jobname");
        defaultTimeout = getInt("default_time_out");
        flushBatchSize = getInt("flush_batch_size");
        flushTimeout = getInt("flush_time_out");
        sepStr = getString("sep");
        //set up the param for url
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("tag", loadingJobname));
        params.add(new BasicNameValuePair("sep", sepStr));
        params.add(new BasicNameValuePair("eol", "\\n"));
        paramStr = getParamStr(params);

        authToken = getString("auth_token");
        hostList = getList("host_list");
        //add param for each host
        for (int i= 0; i < hostList.size(); ++i) {
            hostList.set(i, "http://" + hostList.get(i) + "/ddl?" + paramStr);
            log.info("The updated hostList[" + String.valueOf(i) + "] = " + hostList.get(i));
        }
    }

    public String GetNextUrl() {
        // keep current index
        int idx = curIdx;
        //increment index by 1
        curIdx = (curIdx + 1) % hostList.size();
        return hostList.get(idx);
    }
}
