package com.tigergraph.jdbc;

/*
 * Generate jobId for aggregating statistics of batches,
 * the format is consistent with file/kafka loader's jobid.
 *     format: graph_name.job_name.jdbc.all.timestamp
 */
public class JobIdGenerator {
  static final String IDENTIFIER = "jdbc";
  static final String MACHINE = "all";
  private String jobIdPrefix;
  private long latestRefreshTimestamp;

  /**
   * Initialize a jobid generator with a given seed.
   *
   * @param graph the graph name
   * @param job the loading job name
   */
  public JobIdGenerator(String graph, String job) {
    StringBuilder sb = new StringBuilder();
    sb.append(graph)
        .append(".")
        .append(job)
        .append(".")
        .append(IDENTIFIER)
        .append(".")
        .append(MACHINE)
        .append(".");
    this.jobIdPrefix = sb.toString();
    this.latestRefreshTimestamp = System.currentTimeMillis();
  }

  /**
   * Get latest generated jobid
   *
   * @return latest generated jobid
   */
  public String getJobId() {
    return this.jobIdPrefix + String.valueOf(this.latestRefreshTimestamp);
  }

  public long getLatestRefreshTimestamp() {
    return latestRefreshTimestamp;
  }

  /**
   * Re-generate jobId with new timestamp
   *
   * @return re-generated jobid which contains the latest timestamp
   */
  public String refreshJobId() {
    this.latestRefreshTimestamp = System.currentTimeMillis();
    return this.getJobId();
  }

  /**
   * validate if the jobid matches current graph name and loading job
   *
   * @param jobid
   * @return
   */
  public Boolean validate(String jobid) {
    return (jobid != null) && jobid.startsWith(this.jobIdPrefix);
  }
}
