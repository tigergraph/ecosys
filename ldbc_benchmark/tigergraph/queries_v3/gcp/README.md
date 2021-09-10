# 30TB LDBC SNB on GCP
## Overview
I benchmarked for LDBC 30TB using 10 ultramem-160 machine, but I can run the queries but loading and query running are very slow. This is because each machine only run one GPE instance. Then I tried to use 40 ultramem-40 to do the benchamrk. However, the port check failed during TG installation if the number of instances is larger than 20. I finally decided to use our local CentOS machines to do the benchmark. 

## Table of Contents
## Setup
Install SDK and config the default project and region/zone
```
gcloud init
```

Reserve IP address (to be filled)

Create intance template `ultramem-40`. * It is better to create a large number of smaller machines than a small number of large mechines. Each machine only run one instance of GPE. Larger number of machines speed up loading and query running. * 
Machine type is `m1-ultramem-40`. Boot disk system use `CentOS 7` (there are bugs on Ubuntu !!!) and `balanced persistent disk` of `4096 GB`. 

Create instances.
```sh
# on local machine
# reserve internal IP address 
for i in $(seq 0 39)
do
let "ip = $i + 10"
gcloud compute addresses create ip${i} --region us-central1 --subnet default  --addresses  10.128.0.${ip}
done

# create instances with a specific IP
for i in $(seq 0 39)
do
let "m = $i + 1"
let "ip = $i + 10"
gcloud compute instances create  m${m} --private-network-ip 10.128.0.${ip}  --source-instance-template ultramem-40
done
```

log into instances 
```sh
# on local machine
gcloud compute config-ssh
gcloud beta compute ssh m1
```

Setup instances 
```sh
# on GCP m1 
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3/gcp
gcloud init 
# log in 
sh setup_GCP.sh 
# press enter and skip paraphrase
```

download data, replace the ip address with the start ip in your case.
```sh
# on GCP m1 
# log in as tigergraph
su tigergraph 
# password tigergraph
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3/LDBC_10TB
sudo python3 -m pip install --upgrade pip
sudo pip3 install paramiko scp
python3 download_all.py 30t 10.128.0.10 40 -t 10
```

For large number of machines, GCP takes long time (~3 hr) to set up the ports. Upload the tigergraph package, wait some time and install 
```sh
# on local machine
gcloud beta compute scp [pacakge] m1:~
gcloud beta compute scp [install config] m1

# on GCP m1 
./install.sh -n
```