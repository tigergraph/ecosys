/**
 * Example code to demonstrate how to ingest data from Spark to TG via loading job.
 * Demo graph Social_Net, run `gsql -g Social_Net ls` to check the loading job definition:
 *   https://docs.tigergraph.com/gsql-ref/current/appendix/example-graphs
 * Ref:
 *   https://docs.tigergraph.com/gsql-ref/current/ddl-and-loading/creating-a-loading-job
 *   https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_a_loading_job
 */

/*******************************************************************************
 *                               CUSTOMIZATIONS                                *
 *******************************************************************************/
var FILE_PATH = "path/to/files"
var CONNECTION_URL = "jdbc:tg:http://localhost:14240"
var USERNAME = "tigergraph"
var PASSWORD = "tigergraph"
var GRAPH = "Social_Net"
var LOADING_JOB = "load_member"
var LOADING_FILENAME = "f"

/** 1. read data to Spark DataFrame.
*     - Any data format: CSV, Parquet, Avro, etc.
*     - Local files, files in HDFS, SQL on files, etc.
*   Ref: https://spark.apache.org/docs/latest/sql-data-sources.html
*/

// val df = spark.read.parquet("path/to/hdfs/files");
// val df = spark.read.json("path/to/local/files");
val df = spark.read.option("delimiter", ",").option("header", "false").csv(FILE_PATH)
df.show(5)

/** 2. write the data from Spark DataFrame to TG via JDBC loading job.
 * Demo graph Social_Net, run `gsql -g Social_Net ls` to check the loading job definition:
 *   https://docs.tigergraph.com/gsql-ref/current/appendix/example-graphs
 */

/*******************************************************************************
 *                              TG VERSION < 3.9                               *
 *******************************************************************************/
df.write.mode("overwrite").format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> CONNECTION_URL,
    "username" -> USERNAME,
    "password" -> PASSWORD,
    "graph" -> GRAPH,
    "dbtable" -> "job ".concat(LOADING_JOB), // loading job name
    "filename" -> LOADING_FILENAME, // filename defined in the loading job
    "sep" -> "|", // separator used to concat the columns of the dataframe
    "eol" -> "\n",  // end-of-line charactor used to concat rows from the dataframe
    "batchsize" -> "1000",
    "debug" -> "2")).save()

/*******************************************************************************
 *                              TG VERSION >= 3.9                              *
 *******************************************************************************/

// Step1: generate a job id using the `graph name` and `loading job name`
// NOTE: when ingesting to other graphs/loading jobs, pls initialize a new generator
// i.e. `JobIdGenerator("graph_A", "job_1")` can't work for (graph_A,job_2), (graph_B,job_1)
var generator = new com.tigergraph.jdbc.JobIdGenerator(GRAPH, LOADING_JOB)
println("JobId: ".concat(generator.getJobId()))

// Step2: run loading job with the generated job id
df.write.mode("overwrite").format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> CONNECTION_URL,
    "username" -> USERNAME,
    "password" -> PASSWORD,
    "graph" -> GRAPH,
    /*  <---  new features in TG 3.9.0 */
    "jobid" -> generator.getJobId(),           // jobid for statistics aggregation
    "max_num_error" -> "100",                  // threshold of the error objects count, jobid must be given
    "max_percent_error" -> "50",               // threshold of the error objects percentage, jobid must be given
    /* new features in TG 3.9.0  --->  */
    "dbtable" -> "job ".concat(LOADING_JOB),   // loading job name
    "filename" -> LOADING_FILENAME,            // filename defined in the loading job
    "sep" -> "|",                              // separator used to concat the columns of the dataframe
    "eol" -> "\n",                             // end-of-line charactor used to concat rows from the dataframe
    "batchsize" -> "1000",
    "debug" -> "2")).save()

// Step3: query the aggregated loading stats
var statistics = spark.read.format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> CONNECTION_URL,
    "username" -> USERNAME,
    "password" -> PASSWORD,
    "graph" -> GRAPH,
    "dbtable" -> "jobid ".concat(generator.getJobId()),   // query the loading stats by job id
    "debug" -> "2")).load()

// Step4: either directly print the stats or store to disk
//   when writing to disk, it's better to store to a distributed file system(e.g. HDFS), otherwise,
//   if writing to local disk, it will randomly pick up an worker node to store.

// statistics.show(false) // directly print the stats
// +--------------------+                                                          
// |          statistics|
// +--------------------+
// |{"overall":{"comp...|
// +--------------------+
statistics.coalesce(1).write.mode("overwrite").text("hdfs:/path/to/store/stats")



// You can keep loading other data with new job id and repeat above steps
generator = new com.tigergraph.jdbc.JobIdGenerator("other_graph", "other_job")
println("JobId: ".concat(generator.getJobId()))