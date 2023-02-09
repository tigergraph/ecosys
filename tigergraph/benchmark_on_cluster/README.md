# Benchmark on AWS Cluster
## Pre-requisites
[Install AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) and [configure the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html). The default AWS Access Jey ID/secret and region need to be configured using `aws configure`.
## Set up the cluster
### A. Create EC2 instances via [AWS Management Console](https://aws.amazon.com/console/). 

Launched 72 x r6a.48xlarge instances via AWS EC2.
Each instance attaches 6TB gp3 volumes. 

### B. Configure intances and Set up TigerGraph Cluster
1. Obtian public IP addresses of all instances and save to a file
    ```sh
    aws ec2 describe-instances --filters "Name=instance-type,Values=r6a.48xlarge" --query "Reservations[].Instances[].PublicIpAddress" | sed '1d' | sed '$d' | sed '1,$s/"//g'| sed '1,$s/,//g' > ip_list
    ```
2. Setup all instances.
    ```sh
    vi setup.sh
   #Change `yourpassword` in line 3 in `setup.sh` to your own password. 
    ./setup_AWS.sh ~/benchmark.pem ./ip_list
    ```

3. log into instances
    ```sh
    ssh -i ~/benchmark.pem ec2-user@[publicIpAddress]
    ```
5. Download TigerGraph package and modify `install_conf.json`. Please provide the license, IP address, and sudo username (here is tigergraph) and password.
    ```sh
    # Obtain the NodeList for install_config.json
    awk 'BEGIN {print "{"} {printf "\"m%d:%s\",\n",NR,$0} END {print "}"}' ip_list
    ```
    Then run

        ./install.sh -n
    
## Download Data
Download data, replace the ip address with the start ip in your case.
```sh
# on AWS m1 
# log in as tigergraph
su - tigergraph 
# password yourpassword
sudo python3 -m pip install --upgrade pip
sudo pip3 install paramiko scp
git clone https://github.com/ldbc/ldbc_snb_bi.git
cd ldbc_snb_bi/tigergraph/benchmark_on_cluster
```
Create service key.json file to access the Google Storage Bucket for data downloading and create a file for IP addresses.
```sh
vi key.json
vi ip_list
```

Modify the password of TigerGraph user in `download_all.py`, then run
```sh
python3 download_all.py 30000 ./ip_list -k ./key.json -t 20
```
This script will run `./k8s/download_decompress.sh` on all the machines, the downloaded data is located in `~/sf10000`. Usage of the `download_all.py` is 
```sh
download_all.py [scale factor] [ip address file] -k [service key json file] -t [download threads]`
```

## Load data
Update `TG_DATA_DIR` and `TG_PARAMETER` in `../k8s/vars.sh` and then load the data
```sh
nohup ./k8s/setup.sh > log.setup 2>&1 < /dev/null &
```

## Run benchmark
To run benchmark scripts

```bash
nohup ./k8s/benchmark.sh > log.benchmark 2>&1 < /dev/null &
```

The `queries.sh` and `batches.sh` can be run using a similar approach.

To clear the TigerGraph database

```bash
gsql drop all
```

# Benchmark on Google Cloud Cluster

# Pre-requisites

1. Google Cloud Command line `gcloud`. The default project and region/zone need to be configured using `gcloud init`.

## Set up the cluster

1. Create intance template. The number of machines is dependent on the data size and machine memory. NUMBER_OF_NODES * MEMORY_PER_MACHINE >= SCALE_FACTOR. In SF-10000, we created 48 instances of `n2d-highmem-32`. We created a template `n2d-32` in the [GCP Console](https://cloud.google.com/compute/docs/instance-templates/create-instance-templates):  machine type ``n2d-highmem-32``, boot system `CentOS 7` and `persistent SSD` of `700 GB`. Others are default settings.

1. Reserve IP and create instances

    ```sh
    for i in $(seq 0 47)
    do
    let "ip = $i + 10"
    gcloud compute addresses create ip${i} --region us-central1 --subnet default  --addresses  10.128.0.${ip}
    done

    for i in $(seq 0 47)
    do
    let "m = $i + 1"
    let "ip = $i + 10"
    gcloud compute instances create  m${m} --private-network-ip 10.128.0.${ip}  --source-instance-template n2d-32
    done
    ```

1. Log into instances

    ```sh
    # on local machine
    gcloud compute config-ssh
    gcloud compute ssh m1
    ```

1. Setup instances

    ```sh
    # on GCP m1 
    git clone https://github.com/ldbc/ldbc_snb_bi.git
    cd ldbc_snb_bi/tigergraph/benchmark_on_cluster
    gcloud init --console-only
    ```

1. TigerGraph will be installed under user `tigergraph`. The password need to be modified in `setup.sh`. Run

    ```sh
    ./setup_GCP.sh
    # press enter and skip paraphrase
    ```

1. Download TigerGraph package and modify `install_conf.json`. Please provide the license, IP address, and sudo username (here is tigergraph) and password. Then run

    ```
    ./install.sh -n
    ```

## Download Data
Download data, replace the ip address with the start ip in your case.
```sh
# on GCP m1 
# log in as tigergraph
su - tigergraph 
# password tigergraph
sudo python3 -m pip install --upgrade pip
sudo pip3 install paramiko scp
git clone https://github.com/ldbc/ldbc_snb_bi.git
cd ldbc_snb_bi/tigergraph/benchmark_on_cluster
```
Modify the password of TigerGraph user in `download_all.py`, then run
```sh
python3 download_all.py 10000 10.128.0.10:48 -t 5
```
This script will run `./k8s/download_decompress.sh` on all the machines, the downloaded data is located in `~/sf10000`. Usage of the `download_all.py` is 
```sh
download_all.py [scale factor] [m1 ip address]:[number of nodes] -t [download threads]`
```

## Load data
Update `TG_DATA_DIR` and `TG_PARAMETER` in `../k8s/vars.sh` and then load the data
```sh
nohup ./k8s/setup.sh > log.setup 2>&1 < /dev/null &
```

## Run benchmark
To run benchmark scripts

```bash
nohup ./k8s/benchmark.sh > log.benchmark 2>&1 < /dev/null &
```

The `queries.sh` and `batches.sh` can be run using a similar approach.

To clear the TigerGraph database

```bash
gsql drop all
```
