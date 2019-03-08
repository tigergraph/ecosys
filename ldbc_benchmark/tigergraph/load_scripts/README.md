# LDBC SNB data loading scripts for TigerGraph

## Prerequisites

* Install TigerGraph. Scripts are tested with TigerGraph 2.3.2 Developer edition.
* Generate LDBC SNB data.

## Run scripts

### Modify one_step_load.sh

* LDBC_SNB_DATA_DIR: The full path to your raw data. Don't forget to include the social_network/ at the end of the path.

### Load data

Now you can load data into TigerGraph:

```
./one_step_load.sh
```

At the beginning of the loading job, you can find the lines similar to the following about the full path to the log file:

```
Starting the following job, i.e.
  JobName: load_ldbc_snb, jobid: ldbc_snb_m1.<START_TIME_EPOCH>
  Loading log: '<TIGERGRAPH_HOME>/logs/restpp/restpp_loader_logs/ldbc_snb/ldbc_snb_m1.<START_TIME_EPOCH>.log'
```

Once the loading job is done, you can check the time spent by running a python script with the full path to the log:

```
python3 time.py <TIGERGRAPH_HOME>/logs/restpp/restpp_loader_logs/ldbc_snb/ldbc_snb_m1.<START_TIME_EPOCH>.log
```

This will print out the time spent to load data if the process is done. If you ran the loading job in background and the process is still running, it will tell you it's not done yet, so you can keep trying to run this script until you see the time.
