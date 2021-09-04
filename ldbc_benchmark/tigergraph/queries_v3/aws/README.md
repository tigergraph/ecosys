# 30TB LDBC SNB on AWS
## Overview
Benchmark on GCP encountered unexpected loading errors. We migrate the benchmark to AWS.

## Table of Contents
## Setup
In `setup_AWS.sh`, `key` need to be the location of your private key, `ip_list` is the list of Private IPv4 addresses of mahcines.
Setup instances 
```sh
# on AWS  
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3/aws
sh setup_AWS.sh 
```
