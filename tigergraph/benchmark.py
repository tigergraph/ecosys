#!/usr/bin/python3
import argparse
from pathlib import Path
from datetime import date, timedelta
from queries import run_queries, run_precompute
from batches import run_batch_update
import time
import re
from itertools import cycle
import csv


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='LDBC TigerGraph BI workload Benchmark')
    parser.add_argument('--scale_factor', type=str, help='Scale factor', required=True)
    parser.add_argument('--data_dir', type=Path, help='The directory to load data from')
    parser.add_argument('--cluster', action='store_true', help='Load concurrently on cluster')
    parser.add_argument('--para', type=Path, default=Path('../parameters'), help='parameter folder')
    parser.add_argument('--test', action='store_true', help='Test mode only run one time')
    parser.add_argument('--validate', action='store_true', help='Validation mode', required=False)
    parser.add_argument('--nruns', '-n', type=int, default=5, help='number of runs')
    parser.add_argument('--endpoint', type=str, default = 'http://127.0.0.1:9000', help='tigergraph rest port')
    parser.add_argument('--queries', action='store_true', help='Only run queries', required=False)
    args = parser.parse_args()

    sf = args.scale_factor
    queries_only = args.queries

    output = Path(f'output/output-sf{sf}')
    output.mkdir(parents=True, exist_ok=True)
    results_file = open(output/'results.csv', 'w')
    timings_file = open(output/'timings.csv', 'w')
    timings_file.write(f"tool|sf|day|batch_type|q|parameters|time\n")
    query_variants = ["1", "2a", "2b", "3", "4", "5", "6", "7", "8a", "8b", "9", "10a", "10b", "11", "12", "13", "14a", "14b", "15a", "15b", "16a", "16b", "17", "18", "19a", "19b", "20a", "20b"]

    parameter_csvs = {}
    for query_variant in query_variants:
        # wrap parameters into infinite loop iterator
        parameter_csvs[query_variant] = cycle(csv.DictReader(open(f'{args.para}/bi-{query_variant}.csv'), delimiter='|'))

    query_nums = [int(re.sub("[^0-9]", "", query_variant)) for query_variant in query_variants]
    network_start_date = date(2012, 11, 29)
    network_end_date = date(2013, 1, 1)
    test_end_date = date(2012, 12, 2)
    batch_size = timedelta(days=1)
    needClean = False
    batch_date = network_start_date

    benchmark_start = time.time()
    if queries_only:
        batch_type =  "power"
        run_precompute(args, timings_file, sf, batch_date, batch_type)
        run_queries(query_variants, parameter_csvs, sf, results_file, timings_file, batch_date, batch_type, args)
    else:
        current_batch = 1
        while batch_date < network_end_date and \
          (not args.test or batch_date < test_end_date) and \
          (not args.validate or batch_date == network_start_date):
            if current_batch == 1:
                batch_type = "power"
            else:
                batch_type = "throughput"
            print()
            print(f"----------------> Batch date: {batch_date}, batch type: {batch_type} <---------------")

            batch_id = batch_date.strftime('%Y-%m-%d')

            if current_batch == 2:
                start = time.time()

            writes_time = run_batch_update(batch_date, args)
            precompute_time = run_precompute(args, timings_file, sf, batch_date, batch_type)
            timings_file.write(f"TigerGraph|{sf}|{batch_date}|{batch_type}|writes||{writes_time + precompute_time:.6f}\n")
            reads_time = run_queries(query_variants, parameter_csvs, sf, results_file, timings_file, batch_date, batch_type, args)
            timings_file.write(f"TigerGraph|{sf}|{batch_date}|{batch_type}|reads||{reads_time:.6f}\n")

            # checking if 1 hour (and a bit) has elapsed for the throughput batches
            if current_batch >= 2:
                end = time.time()
                duration = end - start
                if duration > 3605:
                    print("""Throughput batches finished successfully. Termination criteria met:
                        - At least 1 throughput batch was executed
                        - The total execution time of the throughput batch(es) was at least 1h""")
                    break

            current_batch = current_batch + 1
            batch_date = batch_date + batch_size

    benchmark_end = time.time()
    benchmark_duration = benchmark_end - benchmark_start
    benchmark_file = open(output/'benchmark.csv', 'w')
    benchmark_file.write(f"time\n")
    benchmark_file.write(f"{benchmark_duration:.6f}\n")
    benchmark_file.close()

    results_file.close()
    timings_file.close()
