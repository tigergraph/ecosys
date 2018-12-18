package com.tigergraph.tgConnector;

import com.tigergraph.tgConnector.TgConnector;
import java.io.*;
import java.util.*;

import scala.collection.JavaConversions;
import scala.collection.Seq;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.json.JSONObject;


public class SparkConnector implements Serializable {

  public static void main(String[] args) {
    String tg_ip = "";
    if (args.length < 1) {
      System.out.println("Arguments wrongs. augument: tg_ip");
      System.exit(1);
    } else {
      tg_ip = args[0];
    }

    try {
      new SparkConnector().run(tg_ip);
    } catch (Exception e) {
      throw e;
    }
  }

  // example for parse Spark dataset and post to TigerGraph
  private void processHiveData(TgConnector tc, String post_endpoint, String fileVarName, final Dataset<Row> hiveData) {
    hiveData.foreachPartition(rowIterator -> {
      int rowCount = 0;
      StringBuilder payload = new StringBuilder("");
      while (rowIterator.hasNext()) {
        Row row = rowIterator.next();
        rowCount++;
        for (int i = 0; i < row.length(); i++){
            payload.append(Objects.toString(row.get(i), "") + (i == row.length() - 1 ? "\n" : ","));
        }

        if (rowCount == 5000){
          tc.getJsonForPost(post_endpoint, payload.toString(), fileVarName);
          payload.setLength(0);
          rowCount = 0;
        }
      }
      //handle case for the last batch
      if (rowCount > 0){
        tc.getJsonForPost(post_endpoint, payload.toString(), fileVarName);
      }
    });
  }

  public void run(String tg_ip) {
    // create spark context and spark sql instance
    // SparkSession sc = SparkSession.builder().appName("spark2TG").getOrCreate();
    TgConnector tc = new TgConnector(tg_ip, "9000", "tigergraph");

    try {
      // assume you already installed queries: topCoLiked and topCoLiked_file
      // Get json response for a realtime query
      JSONObject json_res = tc.getJsonForQuery("topCoLiked?input_user=id1&topk=10");
      System.out.println("--------------------------");
      System.out.println(json_res.toString(4));
      System.out.println("--------------------------");

      //the remote output file on TigerGraph and local target file.
      String remote_file = "/tmp/abc.csv";
      String local_file = "/tmp/abc.csv";

      // Get json response for a batch query, which will output to a csv file on TG server.
      // this require user increase RESTPP timeout limit.
      JSONObject json_res2  = tc.getJsonForQuery("topCoLiked_file?input_user=id1&topk=10&_f="+remote_file);
      // Copy the output file from TG server to spark local machine
      // you need set up the ssh connection between TG machine and spark cluster before running
      tc.copyFileToLocal(remote_file, local_file);
      System.out.println("--------------------------");
      System.out.println(json_res2.toString(4));
      System.out.println("--------------------------");

      // Get json response for a data post
      StringBuilder payload = new StringBuilder("");
      payload.append("id100,id1000\n");
      payload.append("id100,id2000\n");
      payload.append("id100,id3000\n");
      payload.append("id200,id4000\n");
      payload.append("id200,id1000\n");
      // default fileVariable if f
      JSONObject json_res3 = tc.getJsonForPost("load_cf", payload.toString(), "f");
      System.out.println("--------------------------");
      System.out.println(json_res3.toString(4));
      System.out.println("--------------------------");
      // validate
      JSONObject json_res4 = tc.getJsonForQuery("topCoLiked?input_user=id1000&topk=10");
      System.out.println("--------------------------");
      System.out.println(json_res4.toString(4));
      System.out.println("--------------------------");


    } catch (Exception e) {
      throw e;
    }
  }
}
