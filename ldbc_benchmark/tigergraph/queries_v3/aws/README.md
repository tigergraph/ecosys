# 30TB LDBC SNB on AWS
## Overview
Benchmark on GCP encountered unexpected loading errors. We migrate the benchmark to AWS.

## Table of Contents
## Setup
up load your private key and aws credentials
```sh
# on my own laptop 
scp -i yuchen_key.pem yuchen_key.pem ubuntu@3.138.173.107:~/yuchen.pem
scp -i yuchen_key.pem .aws/credentials ubuntu@3.138.173.107:~
```

Setup instances 
```sh
# update the ip lists in setup_AWS.sh
# on master EC2
chmod 400 ~/yuchen.pem
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3/aws
sh setup_AWS.sh 
```

Set the setup and the disk `/home/tigergrah` is mounted
```sh
#check the log at ~/foo.out and swith user to tigergraph
su - tigergraph
```
## Install 
download the package
```sh
#copy aws credentials
mkdir .aws
sudo cp /home/ubuntu/credentials .aws
sudo chown tigergraph:tigergraph .aws/credentials
sudo chmod 400 .aws/credentials
aws s3 cp s3://ldbc30/tigergraph-tg_3.2.0_dev-wip_test_10473-offline.tar.gz .
tar -xf tigergraph-tg_3.2.0_dev-wip_test_10473-offline.tar.gz 
```

update the ip list in `../gcp/install_conf.json` and replace it 
```
./install.sh -n
```

## Download data
```sh
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3/aws

# install paramiko (for password login using tigergraph)
sudo python3 -m pip install --upgrade pip
sudo pip3 install paramiko scp
```

update the `ip_list` in `download_all.py`
```sh
python3 download_all.py
```
