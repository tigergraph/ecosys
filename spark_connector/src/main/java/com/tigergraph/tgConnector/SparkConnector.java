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

  public void run(String tg_ip) {
    // create spark context and spark sql instance
    // SparkSession sc = SparkSession.builder().appName("spark2TG").getOrCreate();
    TgConnector tc = new TgConnector(tg_ip, "9000", "tigergraph");

    try {
      // Get json response for realtime query
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
      tc.copyFileToLocal(remote_file, local_file);
      System.out.println("--------------------------");
      System.out.println(json_res2.toString(4));
      System.out.println("--------------------------");

    } catch (Exception e) {
      throw e;
    }
  }
}
