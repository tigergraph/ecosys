# TigerGraph LDBC SNB Benchmark
## Table of Contents
* [Overview](#Overview)
* [Pre-requisites](#Pre-requisite)
* [Download LDBC SNB data](#Donwload-LDBC-SNB-Data)
* [BI Workload](#BI-Workload)
  * [Donwload and Decompress Data](#Donwload-and-Decompress-Data)   
  * [Load Data](#Load-Data)   
  * [[optional] Run queries](#optional-Run-queries)   
  * [[optional] Compare the results](#optional-Compare-the-results)   
  * [Refresh and Evaluate](#Refresh-and-Evaluate)
  * [Summary](#Summary)
* [Interactive Workload](#Interactive-Workload)
  * [Download and Load](#Download-and-Load)
  * [Interactive Updates](#Interactive-Updates)
* [Usage of driver.py](#Usage-of-driverpy)
* [Considerations in writing GSQL queries](#Considerations-in-writing-GSQL-queries)

&nbsp;
## Overview
Repository for LDBC SNB 30TB benchmark by TigerGraph in 2021-10-01. 
The benchmark runs the Business Intelligence (BI) workloads in [LDBC Social Network Benchmark v.0.4.0](https://github.com/ldbc/ldbc_snb_docs). The repository contains the following folders
* [`queries`](./queries) IC IS BI GSQL read queries.
* [`refreshes`](./refreshes) Micro-batch refreshes in BI workload.
* [`updates`](./updates) Interactive updates in Interactive workload.
* [`driver.py`](./driver.py) the utility script to run the benchmark
* [`schema.gsql`](./schema.gsql) the LDBC SNB schema and loading job
* [`load.gsql`](./load.gsql) loading job for the initial snapshot
* [`cypher`](./cypher) the same benchmark using Neo4j
* [`LDBC_10TB`](./LDBC_10TB) scripts for downloading and processing data of 10TB and 30TB
* [`gcp`](./gcp) scripts for setting up environment on google cloud platform (GCP)
* [`aws`](./aws) scripts for setting up environment on AWS, not used in our benchmark

Related links
* [LDBC_BI by Garbor](https://github.com/ldbc/ldbc_snb_bi) contains the benchmark using Cypher and postgres
* [(TierGraph internal) JIRA page](https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/2352251355/LDBC-SNB)
* [(TierGraph internal) benchmark results in Google Sheet](https://docs.google.com/spreadsheets/d/1NVdrOQtYBZl3g2B_jxYozo2pV-8B0Zzf50XDVw0JzTg/edit?ts=60b84592#gid=1034343597) contains internal benchmark results
* [(TierGraph internal) V2 syntax: Best practices and Case study](https://docs.google.com/presentation/d/1f5nYGFGabQjGlcWuo3RKFnJNu4GMmFB8J3UWIWy7YX4/edit?usp=sharing)
The query performance depends on the data structure, the choice of parameters and cluster configuration (e.g. number of nodes). A discusion on how to write queries can be found here.
* [SF-30k benchmark slide in Graph AI summit](https://docs.google.com/presentation/d/1BbjXb6udkwUM7RvYsNBMw7QXdbWtifrTy8ukolxAILU/edit?usp=sharing)

&nbsp;
## Pre-requisite 
* `TigerGraph` (at least 3.1.0, pacakges between 3.2.0, 3.2.1, 3.3.0 have ZK timeout issue on 40 nodes) and its pre-requisites 
  * `sshpass` only required for the host
  * `net-tools`
* `Python` (at least 3.6) 
  * Python package `requests`

```sh
sudo yum install wget git tar python3-pip sshpass gcc
sudo python3 -m pip install requests
```
Install tigergraph-3.1.3. For internal test, you can find the latest build from (http://192.168.11.192/download.html). 
```sh
wget https://dl.tigergraph.com/enterprise-edition/tigergraph-3.1.3-offline.tar.gz
tar -xf tigergraph-3.1.3-offline
cd tigergraph-3.1.3-offline/
./install.sh
# following the instruction to install
su tigergraph
gadmin status
# check if TigerGraph is running or not.
```
&nbsp;
## Config Tigergraph 
For small data on a small number of nodes, the default setting works. 
For 30TB benchmark on 40 nodes, the GPE request time can be larger than the default timeout setting.
```sh
gadmin config group timeout 
# Change FileLoader.Factory.DefaultQueryTimeoutSec: 16 -> 6000
# Change KafkaLoader.Factory.DefaultQueryTimeoutSec: 16 -> 6000
# Change RESTPP.Factory.DefaultQueryTimeoutSec: 16 -> 6000
```

Enterprise edition also store a backup copy be default. You can turn off this backup copy to save disk space.
The loading performance can be tuned by changing the line batch size, number of concurrent requests and number of loading handler.
```sh
gadmin config entry GPE.BasicConfig.Env
# add MVExtraCopy=0; (default is 1)
# [optional] add ConcurrentRequest=[value]; (default value is 16)   

gadmin config group RESTPP-LOADER
# [optional] change FileLoader.Factory.HandlerCount

gadmin config apply -y
gadmin restart all -y
```
&nbsp;
## BI Workload
### Donwload and Decompress Data  
For data larger than 1TB, refer to [LDBC_10TB](./LDBC_10TB). SF-1 and SF-100 data are available at GSC buckets `gs://ldbc_small/sf1.tar.gz` and `gs://ldbc_small/sf100.tar.gz`. 
*For these smaller dataset, the data has headers.* 

```sh
gsutil -m cp gs://ldbc_small/sf1.tar.gz .
tar -xf sf1.tar.gz
```
&nbsp;
### Load Data
Checkout ldbc branch of the current repository
```sh
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3
```
Load schema, data, and query. Usage of `driver.py` can be shown using `-h` option. For example, to check how to load the data use `./driver.py load data -h`. The data directory should contain 31 folders in name of the vertex and edge type names. The CSV files inside these folders are loaded. 
```sh
./driver.py load schema
./driver.py load data ~/sf1/csv/bi/composite-projected-fk/ --header
./driver.py load query
```
This is equivalent to
```sh
./driver.py load all ~/sf1/csv/bi/composite-projected-fk/ --header
```

To check the data size, use
```sh
gstatusgraph
```

The directory can include the machine. Default setting is `ANY`, which loads any data on each on the machine. If you want to load data on the node m1, use
```sh
./driver.py load data m1:~/initial_snapshot --header
``` 
After loading, you can checkout the number of vertices and edges using the following command. For SF1, there are 1116485 Comment vertices in the initial snapshot.
```sh
./driver.py stat
```
&nbsp;
### [optional] Run queries
Usage of `./driver.py` can be found using `./driver.py run -h`. The basic usage is `./driver.py run -q [queries] -n [number of runs] -p [parameter file]`. 
```sh
./driver.py run  -n 3 -q reg:bi*
```

Default setting runs all BI, IC, and IS quereis. The option `-p` is for choosing the quereis. For example, `-q bi1,bi2` run bi1 and bi2, `-q reg:i[cs]*` use regular expression and run IC and IS queries. `-q not:bi19` run all queries except bi19.

The command also runs `./driver.py gen_para -p auto` at the start. This generate parameters in `param.json` if the file does not exist in the results folder. If the `param.json` exists in the results folder, the parameters in the file will be used. The format of parameter file is shown in [./paramters/sf1.json](./paramters/sf1.json). You can modify it and use it by using `./driver.py run -p paramters/sf1.json`.

```sh
# run all queries other than bi19 and each is performed for 3 times
./driver.py run -q not:bi19 -n 3
# To run all the queries
./driver.py run 
```
&nbsp;
### [optional] Compare the results
The starting time of LDBC SNB graph is 2012-09-13. The documented GSQL results are in `results_sf[scale factor]/initial`. The documented  Cypher results are in `cypher/results_sf[scale factor]/initial`. To compare the results with the documented GSQL results.
```sh
 ./driver.py compare -s results -t results_sf1/initial
```
The script can be also used to compare the GSQL and cypher results. 
```sh
# this is also the default parameter setting
 ./driver.py compare -s results -t cypher/results 
```
&nbsp;
### Refresh and Evaluate
Regresh the data with batch insert and delete operations. The queries are evaluated at the start and after certain number of batches (default value is 30). 
```sh
./driver.py refresh ~/sf1/csv/bi/composite-projected-fk/ --header -b [begin_date] -e [end_date] -r [read_interval]
```
The workflow is as follows: 
1. run queries
2. perform insert and delete starting from the begin date, each operation is batchd by a day 
3. after a number of batches specified by `read_interval`, evaluate query again
4. repeat step 2 and 3 

The results and timelog are output to `results/`. The summary of statistics, insert/delete/query time are in `results/timelog.csv`.
Since, the queries are performed during the refresh operations, there is no need to run queries again. The options for running queries also work here for choosing queries and parameters. After runnning Neo4j benchmark, you can compare the results
```sh
./driver.py compare 
```
&nbsp;
### Summary
If you are familiar with the procedures above, you can run `./driver.py bi`.
This command first runs `./driver.py load all [dara_dir]` and then `./driver.py refresh [dara_dir]`. These are all the jobs for BI workload.

For scale factors larger than 100, the workload usually takes many hours. I prefer to use `nohup` to allow the process continue after I log out. 
Because releasing memory also takes time, we also add sleep time equal to 0.5 of the running time between query runs. Without sleep time, out-of-memory issue sometimes occurs.
```sh
nohup python3 -u ./driver.py bi ~/sf100/csv/bi/composite-projected-fk/ -s 0.5 --header > foo.out 2>&1 < /dev/null &  
```
&nbsp;
## Interactive Workload
### Download and Load
The interactive data are in the GCS bucket `gs://ldbc_interactive/composite_projectecd/sf1.tar.zst`. Available scale factors are 0.1, 0.3, 1, 3, 10, 30, 100, 300, 1000. The data format is composite projected (see LDBC Specification for details).

```
cd ~
gsutil cp gs://ldbc_interactive/composite_projectecd/sf1.tar.zst .
unzstd sf1.tar.zst
tar -xf sf1.tar
# go to ./queries_v3 directory
cd ecosys/ldbc_benchmark/tigergraph/queries_v3
./driver.py load all /home/tigergraph/social_network-csv_composite-sf1/ --format ic
```

### Interactive Updates
The are two update streams `updateStream_0_0_forum.csv` and `updateStream_0_0_person.csv`. To pass each line of these two csv files into insert queries (ins1~ins8)
```
./driver.py update /home/tigergraph/social_network-csv_composite-sf1/ 
```

&nbsp;
## Usage of driver.py
Option `--help` can be used to check for usage. The structure of the 

* `./driver.py bi [dara_dir]` - run all BI Workloads, run the following in sequence
  * `./driver.py load all [dara_dir]` will runs
    * `./driver.py load schema` 
    * `./driver.py load data [dara_dir]`
    * `./driver.py load query` 
  * `./driver.py refress [dara_dir]` - check the statistics, perform batch refresh and run queries after several batches (default is 7)
    * `./driver.py stat` - display the statistics of the data
    * `./driver.py run` - run queries 
      * `./driver.py gen_para` generate paremters if parameter files are not found
   


