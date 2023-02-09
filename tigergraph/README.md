# LDBC SNB BI TigerGraph/GSQL implementation
**The instruction is for single-node benchmark.**
We provide two methods for cluster setup.
  1. [k8s/README.md](./k8s) - Deploy TG containers using [kubernetes (k8s)](https://kubernetes.io) 
  1. [benchmark_on_cluster/README.md](./benchmark_on_cluster) - Manually install and configure TigerGraph

[[Old Benchmark link]](https://github.com/tigergraph/ecosys/tree/ldbc/ldbc_benchmark/tigergraph/queries_v3)

## Generating the data set

The TigerGraph implementation expects the data to be in `composite-projected-fk` CSV layout. To generate data that confirms this requirement, run Datagen with the `--explode-edges` option.  In Datagen's directory (`ldbc_snb_datagen_spark`), issue the following commands. We assume that the Datagen project is built and the `${PLATFORM_VERSION}`, `${DATAGEN_VERSION}` environment variables are set correctly.

```bash
export SF=desired_scale_factor # for example SF=1
export LDBC_SNB_DATAGEN_MAX_MEM=available_memory # for example LDBC_SNB_DATAGEN_MAX_MEM=8G
export LDBC_SNB_DATAGEN_JAR=$(sbt -batch -error 'print assembly / assemblyOutputPath')
```

```bash
rm -rf out-sf${SF}/
tools/run.py \
    --cores $(nproc) \
    --memory ${LDBC_SNB_DATAGEN_MAX_MEM} \
    -- \
    --format csv \
    --scale-factor ${SF} \
    --explode-edges \
    --mode bi \
    --output-dir out-sf${SF} \
    --format-options compression=gzip
```

## Load data

1. To download and use the sample data set, run:

    ```bash
    scripts/get-sample-data-set.sh
    ```

1. To use other data sets, adjust the variables in [`scripts/configure-data-set.sh`](scripts/configure-data-set.sh):

    * `TG_DATA_DIR` - a folder containing the `initial_snapshot`, `inserts` and `deletes` directories.
    * `TG_LICENSE` - optional, trial license is used if not specified, sufficient for SF30 and smaller.
    * `SF` - scale factor

1. Run:

    ```bash
    . scripts/configure-data-set.sh
    ```

1. Load the data:

    ```bash
    scripts/load-in-one-step.sh
    ```

    This step may take a while, as it is responsible for defining the schema, loading the data and installing the queries. The TigerGraph container terminal can be accessed via:
    
    ```bash
    docker exec --user tigergraph -it snb-bi-tg bash
    ```

    If a web browser is available, TigerGraph GraphStudio can be accessed via <http://localhost:14240/>.

1. The substitution parameters should be generated using the [`paramgen`](../paramgen).

## Microbatches

Test loading the microbatches:

```bash
scripts/batches.sh
```

:warning: Note that the data in TigerGraph database is modified. Therefore, **the database needs to be reloaded or restored from backup before each run**. Use the provided `scripts/backup-database.sh` and `scripts/restore-database.sh` scripts to achieve this.

## Queries

To run the queries, issue:

```bash
scripts/queries.sh
```

For a test run, use:

```bash
scripts/queries.sh --test
```

Results are written to `output/output-sf${SF}/results.csv` and `output/output-sf${SF}/timings.csv`.

## Benchmarks

To run the benchmark, issue:

```bash
scripts/benchmark.sh
```

## About the TigerGraph Implementation
1. Because the current TigerGraph datetime use ecpoch in seconds but the datetime in LDBC SNB benchmarks is in milliseconds. So we store the datetime as INT64 in the datatime and write user defined functions to do conversion. The dateime value in the dataset is considered as the local time. INT64 datetime in millisecond `value` can be converted to datetime using `datetime_to_epoch(value/1000)`.
1. The user defined function is in `ExprFunctions.hpp` (for query) and `TokenBank.cpp` (for loader).