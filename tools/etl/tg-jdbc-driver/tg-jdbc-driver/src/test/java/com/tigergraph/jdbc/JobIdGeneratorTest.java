package com.tigergraph.jdbc;

import junit.framework.TestCase;

/** Unit test for JobIdGenerator */
public class JobIdGeneratorTest extends TestCase {

  private JobIdGenerator jobIdGenerator;

  public JobIdGeneratorTest(String name) {
    super(name);
    this.jobIdGenerator = new JobIdGenerator("social", "load_social");
  }

  // the job id format should be "graph.job.jdbc.timestamp"
  public void testGenerateJobId() throws Exception {
    String expected =
        "social." + "load_social." + "jdbc." + "all." + jobIdGenerator.getLatestRefreshTimestamp();
    assertEquals(expected, jobIdGenerator.getJobId());
  }

  // the timestamp should be refreshed after `refreshJobId()`
  public void testRefresh() throws Exception {
    long timestampBeforeRefresh = jobIdGenerator.getLatestRefreshTimestamp();
    String jobIdBeforeRefresh = jobIdGenerator.getJobId();
    jobIdGenerator.refreshJobId();
    long timestampAfterRefresh = jobIdGenerator.getLatestRefreshTimestamp();
    String jobIdAfterRefresh = jobIdGenerator.getJobId();

    assertTrue(jobIdBeforeRefresh.contains(String.valueOf(timestampBeforeRefresh)));
    assertTrue(jobIdAfterRefresh.contains(String.valueOf(timestampAfterRefresh)));
    assertTrue(timestampAfterRefresh > timestampBeforeRefresh);
  }

  public void testValidate() throws Exception {
    JobIdGenerator jobIdGeneratorNe = new JobIdGenerator("not_social", "not_load_social");
    assertEquals(this.jobIdGenerator.validate(jobIdGeneratorNe.getJobId()), Boolean.FALSE);
  }
}
