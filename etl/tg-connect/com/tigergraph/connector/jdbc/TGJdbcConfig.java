package com.tigergraph.connector.jdbc;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TGJdbcConfig
{
    public String jdbc_url;
    public String jdbc_driver;
    public String db_user;
    public String db_password;
    public String db_query;
    public String tg_url;
    public String tg_graph;
    public String tg_loadjob;
    public char   tg_separator_ascii;
    public String tg_separator_url;
    public char   tg_eol_ascii;
    public String tg_eol_url;
    public long   tg_batch_size;

    @SuppressWarnings("unchecked")
    public TGJdbcConfig(String config_file_json)
    {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(config_file_json))
        {
            //Read JSON file
            Object Obj = jsonParser.parse(reader);
            JSONObject confObj = (JSONObject) Obj;
            //Get config one by one: for RDBMS
            this.jdbc_driver = (String) confObj.get("JDBC_DRIVER");
            this.jdbc_url = (String) confObj.get("JDBC_URL");
            this.db_user = (String) confObj.get("DB_USER");
            this.db_password = (String) confObj.get("DB_PASSWORD");
            this.db_query = (String) confObj.get("DB_QUERY");

            //Get config one by one: for TG
            this.tg_url = (String) confObj.get("TG_URL");
            this.tg_graph = (String) confObj.get("TG_GRAPH");
            this.tg_loadjob = (String) confObj.get("TG_LOADJOB");
            this.tg_separator_ascii = (char)((long)confObj.get("TG_SEPARATOR_ASCII"));
            this.tg_separator_url = (String) confObj.get("TG_SEPARATOR_URL");
            this.tg_eol_ascii = (char)((long)confObj.get("TG_EOL_ASCII"));
            this.tg_eol_url = (String) confObj.get("TG_EOL_URL");
            this.tg_batch_size = (long) confObj.get("TG_BATCH_SIZE");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}