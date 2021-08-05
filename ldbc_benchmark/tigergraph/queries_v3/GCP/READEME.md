# 30TB LDBC SNB on GCP 
## Table of Contents

## Setup
1. Install SDK and config the default project and region/zone
```
gcloud init
```

2. Create instances. * It is better to create a large number of smaller machines than a small number of large mechines. Each machine only run one instance of GPE. Larger number of machines speed up loading and query running. * create intance template called `ultramem-40`. Machine type is `m1-ultramem-40`. Boot disk system use `Ubuntu 20LTS` and `balanced persistent disk` of `4000 GB`.

```sh
# on local machine
for i in $(seq 1 40)
do
gcloud compute instances create m${i} --source-instance-template ultramem-40
done
```

3. log into instances 
```sh
# on local machine
gcloud compute config-ssh
gcloud beta compute ssh m1
```

4. Setup instances 
```sh
# on GCP m1 
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3/GCP
sh setup_GCP.sh
```

4. update the tigergraph package and install 
```sh
# on local machine
gcloud beta compute scp [pacakge] m1
gcloud beta compute scp [install config] m1

# on GCP m1 
./install.sh -n
```

5. download data, replace the ip address with the start ip in your case.
```sh
# on GCP m1 
python3 ../LDBC_10TB/download_all.py 30t 10.128.0.4 40 
```
