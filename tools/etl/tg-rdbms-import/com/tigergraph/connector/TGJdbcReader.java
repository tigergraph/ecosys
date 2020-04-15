package com.tigergraph.connector.jdbc;

import java.sql.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;

public class TGJdbcReader
{
    private Connection conn = null;
    public ResultSet rs = null;
    private ResultSetMetaData rsmd  = null;
    private TGJdbcConfig tgjdbc_config =  null;
    long Reading_Count;
    long Reading_Start_Time;

    private void find_driver(String JDBC_DRIVER) throws Exception
    {
        Class.forName(JDBC_DRIVER);
    }

    /**
     * Constructor for objects of class TGJdbcReader.
     * @param JDBC_DRIVER, e.g., com.mysql.jdbc.Driver
     */
    public TGJdbcReader(String JDBC_DRIVER) throws Exception
    {
        find_driver(JDBC_DRIVER);
    }

    /**
     * Default TGJdbcReader is using mysql driver
     */
    public TGJdbcReader() throws Exception
    {
        find_driver("com.mysql.cj.jdbc.Driver");
    }

    /**
     * Constructor for objects of class TGJdbcReader.
     * @param TGJdbcConfig
     */
    public TGJdbcReader(TGJdbcConfig tgjdbc_config) throws Exception
    {
        this.tgjdbc_config = tgjdbc_config;
        find_driver(this.tgjdbc_config.jdbc_driver);
    }



    /**
     * @param JDBC_URL, database URL, e.g., jdbc:mysql://localhost/test
     * @param USER, db user, e.g., tigergraph
     * @param PASS, db password, e.g., tigergraph
     * @throw Exception if fails
     */
    public void getConnection(  String JDBC_URL,
                                String USER,
                                String PASS) throws Exception
    {
        this.conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
    }

    /**
     * @throw Exception if fails
     */
    public void getConnection() throws Exception
    {
        this.conn = DriverManager.getConnection(
            this.tgjdbc_config.jdbc_url,
            this.tgjdbc_config.db_user,
            this.tgjdbc_config.db_password);
    }

    /**
     * Constructor for TGResultSet.
     */
    public void getResultSet(String sql_select_string)  throws Exception
    {
        Statement stmt = conn.createStatement();
        this.rs = stmt.executeQuery(sql_select_string);
        this.rsmd = this.rs.getMetaData();
        Reading_Count = 0;
        Reading_Start_Time = System.currentTimeMillis();
    }

    /**
     * Constructor for TGResultSet.
     */
    public void getResultSet()  throws Exception
    {
        this.getResultSet(this.tgjdbc_config.db_query);
    }

    public boolean next()  throws Exception {
        return this.rs.next();
    }

    /* return string from a row record */
    public String record2string(char separator)  throws Exception {
        StringBuilder sb = new StringBuilder();
        int numberOfColumns = rsmd.getColumnCount();
        for (int i = 1; i <= numberOfColumns; i++) {
            sb.append(rs.getString(i));
            if (i < numberOfColumns) {
                sb.append(separator);
            }
        }
        Reading_Count++;
        return sb.toString();
    }

    public String record2string()  throws Exception {
        return record2string(this.tgjdbc_config.tg_separator_ascii);
    }

    public void showProgress() throws Exception {

        if ( Reading_Count % 100000 == 0) {
            System.out.print(".");
            if ( Reading_Count % 5000000 == 0){
                NumberFormat formatter = new DecimalFormat("#0.00");
                System.out.print(" " + Reading_Count + "\t");
                System.out.println(formatter.format(runningTime()) + " seconds");
            }
        }
    }

    public void showStatus() throws Exception {
        NumberFormat formatter = new DecimalFormat("#0.00");
        System.out.println("Nubmer of processed records: " + Reading_Count);
        System.out.println("Running time: " + formatter.format(runningTime()) + " seconds");
    }

    private double runningTime() throws Exception {
        long end = System.currentTimeMillis();
        return (end - Reading_Start_Time) / 1000d;
    }
    /**
     * done and gone
     */
    public void close() throws Exception
    {
        if (rs != null) rs.close();
        if (conn != null) conn.close();
    }

}

