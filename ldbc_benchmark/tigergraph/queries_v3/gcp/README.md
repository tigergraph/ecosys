# 30TB LDBC SNB on GCP
## Table of Contents
* [Overview](#Overview)
* [Setup on GKE](#Setup-on-GKE)
* [Setup on EKS](#Setup-on-EKS)
* [Setup on VM](#Setup-on-VM)

## Overview
I benchmarked for LDBC 30TB using 10 ultramem-160 machine, but I can run the queries but loading and query running are very slow. This is because each machine only run one GPE instance. Then I tried to use 40 ultramem-40 to do the benchamrk. However, the port check failed during TG installation if the number of instances is larger than 20. I finally decided to use our local CentOS machines to do the benchmark. 

## Setup on GKE
This example is for 10T benchmark and also works for 1T and 30T after you change the number of vm and the vm types.
On your desktop, install `gcloud` and `kubectl`. Create a cluster on GKE 
```
gcloud container clusters create cluster1 -m m1-ultramem-40 --num-nodes=12 --disk-size 3000 --disk-type=pd-standard
```

Download the master branch of ecosys, create the manifest 
```
git clone https://github.com/tigergraph/ecosys.git
cd ecosys/k8s
./tg gke kustomize -s 12 --pv 3000 -l [license]
kubectl apply -f ./deploy/tigergraph-gke.yaml
```
Upload the script [../LDBC_10TB/download_k8s.sh](../LDBC_10TB/download_k8s.sh) to all the pods and then run it to download and decompress the data. The usage is `download_k8s.sh [data size] [index] [number of nodes] [threads](optional)` and is `download_k8s.sh 10k $i 12` here. The script clone the ecosys at `/home/tigergraph/data` and then use the scripts in [../LDBC_10TB](../LDBC_10TB) to download and decompress the data.
```
cd ../LDBC_10TB
n=12 #need to be the number of pods
for i in $( seq 0 $((n-1)) ); do
    kubectl cp download_k8s.sh tigergraph-${i}:download_k8s.sh
    kubectl exec tigergraph-${i} -- bash -c "bash download_k8s.sh 10k $i $n > log.download 2> /dev/null &"  
done
```

To load run queries and update the graph, refer to section `Load, Run and Update` in [../LDBC_10TB/README.md](../LDBC_10TB/README.md).

## Setup on EKS
This is the setup of EKS for 1T benchmark and smaller data. First, create cluster on EKS 
```
eksctl create cluster --name test --region us-east-2 --nodegroup-name tgtest --node-type r5.8xlarge --nodes 4 --instance-prefix tg --instance-name eks-test 
```

Pull another copy of ecosys at the master branch, create the manifest 
```
git clone https://github.com/tigergraph/ecosys.git
cd ecosys/k8s
 ./tg eks kustomize -s 4 --pv 1024
kubectl apply -f ./deploy/tigergraph-eks.yaml
```
This generate the manifest at `./deploy/tigergraph-gke.yaml`, the memory need to be modified manually. In 1T benchamrk, I used `200G` for 10T. Because I did not get enough number of instances on AWS, I only stored data on Google Cloud Buckets. The downloading scripts do NOT support AWS !!!

## Setup on VM
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
su - tigergraph 
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