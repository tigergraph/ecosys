# Parameter generation

The paramgen implements [parameter curation](https://research.vu.nl/en/publications/parameter-curation-for-benchmark-queries) to ensure predictable performance results that (mostly) correspond to a normal distribution.

## Getting started

1. Install dependencies:

    ```bash
    scripts/install-dependencies.sh
    ```

1. **Generating the factors entities with the Datagen:** In Datagen's directory (`ldbc_snb_datagen_spark`), issue the following commands.
We assume that the Datagen project is built and the `${LDBC_SNB_DATAGEN_MAX_MEM}`, `${LDBC_SNB_DATAGEN_JAR}` environment variables are set correctly.

    ```bash
    export SF=desired_scale_factor
    export LDBC_SNB_DATAGEN_MAX_MEM=available_memory
    export LDBC_SNB_DATAGEN_JAR=$(sbt -batch -error 'print assembly / assemblyOutputPath')
    ```

    ```bash
    rm -rf out-sf${SF}/
    tools/run.py \
        --cores $(nproc) \
        --memory ${LDBC_SNB_DATAGEN_MAX_MEM} \
        -- \
        --format parquet \
        --scale-factor ${SF} \
        --mode raw \
        --output-dir out-sf${SF} \
        --generate-factors
    ```

1. **Obtaining the factors:** Create the `scratch/factors/` directory and move the factor directories from `out-sf${SF}/factors/csv/raw/composite-merged-fk/` (`cityPairsNumFriends/`, `personDisjointEmployerPairs/`, etc.) into it.
Assuming that your `${LDBC_SNB_DATAGEN_DIR}` and `${SF}` environment variables are set, run:

    ```bash
    scripts/get-factors.sh
    ```

    To download and use the factors of the sample data set, run:

    ```bash
    scripts/get-sample-factors.sh
    export SF=0.003
    ```

1. To run the parameter generator, ensure that `${SF}` is set correctly and issue:

    ```bash
    scripts/paramgen.sh
    ```

1. The parameters will be placed in the `../parameters/parameters-sf${SF}/` directory.

## Memory consumption

Note that the parameter generator uses a significant amount of memory. E.g. SF10000 uses 503.7GiB and took about 22 minutes to run.
