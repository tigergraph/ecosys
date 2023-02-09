import duckdb
import argparse
import os
import glob

parser = argparse.ArgumentParser()
parser.add_argument('--tool', type=str, help='Name of the SUT, e.g. PostgreSQL', required=True)
parser.add_argument('--timings_dir', type=str, help='Directory containing the timings.csv file', required=True)
parser.add_argument('--throughput_min_time', type=str, help='Minimum total execution time for throughput batches. Executions with a lower total throughput time are invalid.', default=3600)

args = parser.parse_args()
timings_dir = args.timings_dir
tool = args.tool
throughput_min_time = args.throughput_min_time

# cleanup
db_file = "bi.duckdb"
if os.path.exists(db_file):
    os.remove(db_file)

con = duckdb.connect("bi.duckdb")
con.execute(f"""
    CREATE OR REPLACE TABLE load_time(time float);
    CREATE OR REPLACE TABLE benchmark_time(time float);
    CREATE OR REPLACE TABLE timings(tool string, sf float, day string, batch_type string, q string, parameters string, time float);
    CREATE OR REPLACE TABLE power_test_individual(qid int, q string, time float);

    COPY benchmark_time FROM '{timings_dir}/benchmark.csv' (HEADER, DELIMITER '|');
    COPY load_time      FROM '{timings_dir}/load.csv'      (HEADER, DELIMITER '|');
    COPY timings        FROM '{timings_dir}/timings.csv'   (HEADER, DELIMITER '|');

    INSERT INTO power_test_individual
        SELECT regexp_replace('0' || q, '\D','','g')::int AS qid, q, time
        FROM timings
        WHERE batch_type = 'power';

    CREATE OR REPLACE TABLE power_test_stats AS
        SELECT
            qid,
            q,
            count(time) AS count,
            min(time) AS min_time,
            max(time) AS max_time,
            avg(time) AS avg_time,
            sum(time) AS total_time,
            percentile_disc(0.50) WITHIN GROUP (ORDER BY time) AS p50_time,
            percentile_disc(0.90) WITHIN GROUP (ORDER BY time) AS p90_time,
            percentile_disc(0.95) WITHIN GROUP (ORDER BY time) AS p95_time,
            percentile_disc(0.99) WITHIN GROUP (ORDER BY time) AS p99_time,
        FROM power_test_individual
        GROUP BY qid, q;

    CREATE OR REPLACE TABLE throughput_test AS
        SELECT *
        FROM timings
        WHERE batch_type = 'throughput'
          AND q IN ('reads', 'writes');

    CREATE OR REPLACE TABLE throughput_batches AS
        SELECT count(day)/2 AS n_batches, sum(time) AS t_batches -- /2 is needed because writes+reads are counted separately 
        FROM throughput_test
        -- we drop the days executed after the minimum throughput time passed
        WHERE day <= (
            SELECT day
            FROM
            (
                SELECT DISTINCT day, sum(time) OVER (ORDER BY day) AS throughput_runtime_running_total
                FROM throughput_test
                WHERE q IN ('reads', 'writes')
            )
            WHERE throughput_runtime_running_total >= {throughput_min_time}
            LIMIT 1
        );

    CREATE OR REPLACE TABLE all_throughput_batches AS
        SELECT count(day)/2 AS n_batches, sum(time) As t_batches
        FROM throughput_test;

    CREATE OR REPLACE TABLE throughput_score AS
        SELECT
            CASE WHEN n_batches = 0
                THEN null
                ELSE (24 - (SELECT time/3600 FROM load_time)) * (n_batches / (t_batches/3600))
                END
              AS throughput
        FROM throughput_batches;
    """)

con.execute("""SELECT sf FROM timings LIMIT 1;""");
sf = con.fetchone()[0]
sf_string = str(round(sf, 3)).rstrip('0').rstrip('.')
print(f"SF: {sf_string}")

# cleanup old .tex files
for f in glob.glob(f'./*-{tool}-sf{sf_string}.tex'):
    os.remove(f)

# calculate the geometric mean of runtimes for the power score
# only include writes (w) and queries (q1, q2a, ...)
con.execute("""
    CREATE OR REPLACE TABLE power_score AS
        SELECT 3600 / ( exp(sum(ln(total_time::real)) * (1.0/count(total_time))) ) AS power
        FROM power_test_stats
        WHERE q IN ('writes', '1', '2a', '2b', '3', '4', '5', '6', '7', '8a', '8b', '9', '10a', '10b', '11', '12', '13', '14a', '14b', '15a', '15b', '16a', '16b', '17', '18', '19a', '19b', '20a', '20b');
    """)
con.execute("""SELECT power FROM power_score""")
p = con.fetchone()[0]
print(f"power score:    {p:.02f}")

con.execute("""
    CREATE OR REPLACE TABLE power_at_sf_score AS
        SELECT (SELECT power FROM power_score) * (SELECT sf FROM timings LIMIT 1) AS power_at_sf;
    """)
con.execute("""SELECT power_at_sf FROM power_at_sf_score""")
p_at_sf = con.fetchone()[0]
print(f"power@SF score: {p_at_sf:.02f}")

con.execute("""
    SELECT time
    FROM timings
    WHERE batch_type = 'power'
      AND q = 'reads';
    """)
r = con.fetchone()[0]
print(f"power test's read block time: {r:.01f}")
print()

# calculate and print throughput statistics
con.execute("""SELECT n_batches FROM throughput_batches""")
s = con.fetchone()
if s[0] == 0:
    print(f"throughput score: n/a (the throughput run was <{throughput_min_time}s)")
    con.execute("""
        CREATE OR REPLACE TABLE throughput_score AS
            SELECT NULL AS throughput_at_sf
        """)
    con.execute("""
        CREATE OR REPLACE TABLE throughput_at_sf_score AS
            SELECT NULL AS throughput_at_sf
        """)
else:
    con.execute("""
        CREATE OR REPLACE TABLE throughput_score AS
            SELECT (24 - (SELECT time/3600 FROM load_time)) * (n_batches / (t_batches/3600)) AS throughput
            FROM throughput_batches;
        """)

    con.execute("""SELECT throughput FROM throughput_score""")
    t = con.fetchone()[0]
    print(f"throughput score: {t:.02f}")

    con.execute("""
        CREATE OR REPLACE TABLE throughput_at_sf_score AS
            SELECT (SELECT throughput FROM throughput_score) * (SELECT sf FROM timings LIMIT 1) AS throughput_at_sf
        """)
    con.execute("""SELECT throughput_at_sf FROM throughput_at_sf_score""")
    t_at_sf = con.fetchone()[0]
    print(f"throughput@SF score: {t_at_sf:.02f}")

    con.execute(f"""
        COPY
            (SELECT
                (SELECT printf('\\numprint{{%.2f}} minutes', time/60      ) FROM benchmark_time        ),
                (SELECT printf('\\numprint{{%.2f}}', power                ) FROM power_score           ),
                (SELECT printf('\\numprint{{%.2f}}', power_at_sf          ) FROM power_at_sf_score     ),
                (SELECT printf('\\numprint{{%.2f}}', throughput           ) FROM throughput_score      ),
                (SELECT printf('\\numprint{{%.2f}} \\\\', throughput_at_sf) FROM throughput_at_sf_score),
            )
        TO 'summary-{tool}-sf{sf_string}.tex' (HEADER false, QUOTE '', DELIMITER ' & ');
        """)

con.execute("""SELECT * FROM all_throughput_batches""")
tb = con.fetchone()
print()
print(f"total throughput batches executed: {tb[0]} batch(es) in {tb[1]:.2f}s")

# produce the table in the VLDB paper
con.execute(f"""
    -- order as t_load, w, r, followed by q_1, ..., q_20
    CREATE OR REPLACE TABLE results_table_sorted AS
        SELECT *
        FROM (
          -- -200: power@SF
            SELECT -200 AS qid, NULL AS q, printf('\\numprint{{%.2f}}', power_at_sf) AS t
            FROM power_at_sf_score
          UNION ALL
          -- -199: throughput@SF
            SELECT -199 AS qid, NULL AS q, CASE WHEN throughput_at_sf IS NULL THEN 'n/a' ELSE printf('\\numprint{{%.2f}}', throughput_at_sf) END AS t
            FROM throughput_at_sf_score
          UNION ALL
          -- -100...
            SELECT -100 AS qid, NULL AS q, (SELECT printf('\\numprint{{%.2f}}', time) FROM load_time) AS t
          UNION ALL
            SELECT -99 AS qid, NULL AS q, printf('\\numprint{{%.2f}}', avg_time) AS t
            FROM power_test_stats
            WHERE q = 'writes'
          UNION ALL
            -- time to apply writes: write time minus the sum of precomputation times
            SELECT -98 AS qid, NULL AS q, printf('\\numprint{{%.2f}}', (SELECT min_time FROM power_test_stats WHERE q = 'writes') - (SELECT sum(min_time) FROM power_test_stats WHERE q LIKE '%precomp%')) AS t
          UNION ALL
          -- -75 (group for precomputations)
            SELECT -75 + regexp_replace('q|precomp' || q, '\D','','g')::int AS qid, q, printf('\\numprint{{%.2f}}', min_time) AS t
            FROM power_test_stats
            WHERE q LIKE '%precomp%'
          UNION ALL
          -- -50 (total precomputation time)
            SELECT -50 AS qid, NULL, printf('\\numprint{{%.2f}}', sum(min_time)) AS t
            FROM power_test_stats
            WHERE q LIKE '%precomp%'
          UNION ALL
            SELECT  -25 AS qid, NULL AS q, printf('\\numprint{{%.2f}}', avg_time) AS t
            FROM power_test_stats
            WHERE q = 'reads'
          UNION ALL
          -- 1..20
            SELECT regexp_replace('0' || q, '\D','','g')::int AS qid, q, printf('\\numprint{{%.2f}}', avg_time) AS t
            FROM power_test_stats
            WHERE regexp_matches(q, '^[0-9]+[ab]?')
          UNION ALL
          -- 50..inf
            SELECT 50 AS qid, NULL AS q, printf('%d batch%s', n_batches, CASE WHEN n_batches > 1 THEN 'es' ELSE '' END) AS t
            FROM all_throughput_batches
          UNION ALL
            SELECT 51 AS qid, NULL AS q, printf('\\numprint{{%.2f}}', t_batches) AS t
            FROM all_throughput_batches
          UNION ALL
            SELECT 52 AS qid, NULL AS q, printf('\\numprint{{%.2f}}', time)
            FROM benchmark_time
        )
        ORDER BY qid, q;
    """)
con.execute(f"""
    COPY 
        (SELECT string_agg(t, ' \n')
        FROM results_table_sorted)
    TO 'runtimes-{tool}-sf{sf_string}.tex' (HEADER false, QUOTE '');
    """)

# produce power test statistics in the Full Disclosure Report
con.execute(f"""
    COPY
        (SELECT
            q,
            count,
            printf('\\numprint{{%.3f}}', min_time) AS min,
            printf('\\numprint{{%.3f}}', max_time) AS max,
            printf('\\numprint{{%.3f}}', avg_time) AS mean,
            printf('\\numprint{{%.3f}}', p50_time) AS p50,
            printf('\\numprint{{%.3f}}', p90_time) AS p90,
            printf('\\numprint{{%.3f}}', p95_time) AS p95,
            printf('\\numprint{{%.3f}} \\\\', p99_time) AS 'p99 \\\\',
        FROM power_test_stats
        WHERE count > 1
        ORDER BY qid, q
        )
    TO 'queries-{tool}-sf{sf_string}.tex' (HEADER false, QUOTE '', DELIMITER ' & ');
    """)

con.execute(f"""
    COPY
        (SELECT
            q,
            -- there is only a **single value** per benchmark execution for each operation (reads, writes, precomputation for query X),
            -- hence we can select min (we could also select the max or any of the statistical columns)
            printf('\\numprint{{%.3f}} \\\\', min_time) AS 'time \\\\',
        FROM power_test_stats
        WHERE count = 1
        ORDER BY qid, q
        )
    TO 'operations-{tool}-sf{sf_string}.tex' (HEADER false, QUOTE '', DELIMITER ' & ');
    """)
