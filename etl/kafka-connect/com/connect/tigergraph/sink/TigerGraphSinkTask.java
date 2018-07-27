/******************************************************************************
 * Copyright (c)  2015-2017, TigerGraph Inc.
 * All rights reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 ******************************************************************************/
package com.connect.tigergraph.sink;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;


//http request
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;


public class TigerGraphSinkTask extends SinkTask {
    private static final Logger log = LoggerFactory.getLogger(TigerGraphSinkTask.class);
    int accumulated;
    long lastCommitTime;
    long parseTime;
    StringBuilder ret;
    TigerGraphSinkConfig config;
    HttpURLConnection conn;
    Gson gson;
    //used to convert struct into json format
    private final JsonConverter converter;
    public TigerGraphSinkTask () {
        this.converter = new JsonConverter();
        this.conn = null;
        this.gson = new Gson();
        this.accumulated = 0;
        this.lastCommitTime = System.currentTimeMillis();
        this.ret = new StringBuilder();
        this.parseTime = 0;
    }
    @Override
    public void start(final Map<String, String> props) {
        config = new TigerGraphSinkConfig(props);
        this.accumulated = 0;
        this.ret = new StringBuilder();
        this.parseTime = 0;
    }

    private void commit() {
        if (this.accumulated >= this.config.flushBatchSize) {
            log.info("commited: " + String.valueOf(this.accumulated)
                    + " messages with size = "
                    + String.valueOf(this.ret.length())
                    + " bytes, which took "
                    + String.valueOf(System.currentTimeMillis() - this.lastCommitTime)
                    + " ms, including "
                    + String.valueOf(this.parseTime)
                    + " ms parsing time."
            );
            this.accumulated = 0;
            this.lastCommitTime = System.currentTimeMillis();
            SendHttpConnection(this.config.GetNextUrl(), this.ret.toString());
            this.ret = new StringBuilder();
            this.parseTime = 0;
        }
    }
    @Override
    public void put(Collection<SinkRecord> records) {
        long parseStart = System.currentTimeMillis();
        if (records.isEmpty()) {
            return;
        }
        for (SinkRecord record: records) {
            Object value = record.value();
            if (value instanceof Struct) {
                log.debug("record.value() is Struct type");
                byte[] rawJson = converter.fromConnectData(
                        record.topic(),
                        record.valueSchema(),
                        value
                );
                ret.append(rawJson);
            } else if (value instanceof HashMap){
                log.debug("record.value() is HashMap type with gson.toJson(value), i.e.");
                log.debug(this.gson.toJson(value));
                ret.append(this.gson.toJson(value));
            } else {
                log.debug("record.value() is not a Struct or a HashMap type and write directly");
                ret.append(value);
            }
            ret.append(config.eol);
        }
        log.debug("url = " + config.GetNextUrl());
        log.debug("data = " + ret.toString());
        this.accumulated += records.size();
        this.parseTime += System.currentTimeMillis() - parseStart;
        commit();
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> map) {
        // Not necessary
        long currTime = System.currentTimeMillis();
        if (this.lastCommitTime - currTime >= this.config.flushTimeout
                && this.ret.length() > 0) {
            log.info("flush commited: " + String.valueOf(this.accumulated)
                    + " messages with size = "
                    + String.valueOf(this.ret.length())
                    + ", which took"
                    + String.valueOf(System.currentTimeMillis() - this.lastCommitTime)
                    + " ms, including "
                    + String.valueOf(this.parseTime)
                    + " ms parsing time."
            );
            this.accumulated = 0;
            this.lastCommitTime = System.currentTimeMillis();
            SendHttpConnection(this.config.GetNextUrl(), this.ret.toString());
            this.ret = new StringBuilder();
            this.parseTime = 0;
        }
    }

    @Override
    public void open(Collection<TopicPartition> partitions) {
    }

    @Override
    public void close(Collection<TopicPartition> partitions) {
    }

    @Override
    public void stop() throws ConnectException {
    }

    private String GetErrorResponseStr(String url, HttpURLConnection cnn) {
        StringBuilder ret = new StringBuilder();
        try {
            ret.append("url: \'");
            ret.append(url);
            ret.append("\' failed with HTTP error code : ");
            ret.append(cnn.getResponseCode());
            ret.append(" with response message \'");
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(cnn.getErrorStream()));
            // read the response body
            String output;
            while ((output = br.readLine()) != null) {
                ret.append(output);
            }
            // close the errorstream
            br.close();
        } catch (IOException ex) {
            throw new ConnectException("Exception when sending request: " + ex.getMessage());
        }

        return ret.toString();
    }

    private void SendHttpConnection(String url, String data) {
        try{
            conn = (HttpURLConnection) (new URL(url)).openConnection();
        } catch (IOException e) {
            throw new ConnectException("Exception when set url: " + e.getMessage());
        }
        log.debug("URL is successful set.");
        conn.setConnectTimeout(config.defaultTimeout);
        // set user auth token
        if (!config.authToken.isEmpty()) {
            log.debug("Token is set as \'" + config.authToken + "\'.");
            conn.setRequestProperty("Authorization", "Bearer " + config.authToken);
        }
        try{
            conn.setRequestMethod("POST");
        } catch (IOException e) {
            throw new ConnectException("Exception when set method post: " + e.getMessage());
        }
        log.debug("POST method is successful set.");
        conn.setDoInput(true);

        //2. write data to the connect
        conn.setDoOutput(true);
        //Send request data
        try{
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            throw new ConnectException("Exception when set data: " + e.getMessage());
        }
        log.debug("Data is successful set as: \n" + data);

        StringBuilder test = new StringBuilder();
        int responseCode;
        try {
            responseCode = conn.getResponseCode();
            test.append(responseCode);
        } catch (IOException ex) {
            throw new ConnectException("Exception when get response code: " + ex.getMessage());
        }
        log.debug("Get response code = " + String.valueOf(responseCode));
        if (responseCode != 200) {
            log.error(GetErrorResponseStr(url, conn));
            conn.disconnect();
            return;
        }
        //read input stream, which is the response from RESTPP
        StringBuilder ret = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                ret.append(output);
            }
        } catch (IOException e) {
            log.error("SendHttpConnection Exception!!!\n");
            StringBuilder r = new StringBuilder();
            try {
                r.append(conn.getResponseCode());
            } catch (IOException ex) {
                throw new ConnectException("Exception when get response code: " + ex.getMessage());
            }
            log.info("IOException with ResponseCode: " + r.toString());
            log.error(GetErrorResponseStr(url, conn));
            conn.disconnect();
            return;
        }
        log.debug("Response Message: " + ret.toString());
        try {
            JSONObject json = new JSONObject(ret.toString());
            if (!json.has("reports")) {
                log.info("Error Response Message: " + ret.toString());
            }
        } catch (JSONException e) {
            log.info("Error Response Message: " + ret.toString() + " with message: " + e.getMessage());
        }
        //4. close connection
        conn.disconnect();
    }

    @Override
    public String version() {
        return "TigerGraphSinkTask.V0.1";
    }
}


