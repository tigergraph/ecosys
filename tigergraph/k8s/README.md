# benchmark on K8S cluster

## Overview

Benchmarks on clusters are performed using [kubernetes (k8s)](https://kubernetes.io). Cluster is created using GKE (Google Kubernetes Engine) on Google Cloud, or EKS on AWS. 
Pre-requisites are
* `kubectl`
* command line tool for GCP or AWS: `gcloud` or `aws-cli`. The default project and region/zone need to be configured. For GCP, is `gcloud init`.

> Benchmark can also be performed on local clusters without k8s. But the setup is susceptible to errors and safety issues. Brief instructions are in [Section: Benchmark without k8s](../benchmark_on_cluster).

## A. Benchmark on AWS EKS
The below instruciton is for the setup of SF-1000 benchmark on a 4-node k8s cluster.

## Create the cluster

create [EKS cluster](https://docs.aws.amazon.com/eks/latest/userguide/create-cluster.html),
```bash
eksctl create cluster --name sf1000 --region us-east-2 --nodegroup-name tgtest --node-type r6a.8xlarge --nodes 4 --instance-prefix tg --instance-name eks-test --node-volume-size=200
```
## Create EBS CSI Add-on
Following the instructions on [AWS documentation](https://docs.aws.amazon.com/eks/latest/userguide/managing-ebs-csi.html) to add EBS CSI add-on before proceed.
Use ```kubectl get pods -n kube-system``` to check if EBS CSI driver is running
```bash
eksctl utils associate-iam-oidc-provider --cluster sf1000 --approve

eksctl create iamserviceaccount \
  --name ebs-csi-controller-sa \
  --namespace kube-system \
  --cluster sf1000 \
  --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy \
  --approve \
  --role-only \
  --role-name AmazonEKS_EBS_CSI_DriverRole

eksctl create addon --name aws-ebs-csi-driver --cluster sf1000 --service-account-role-arn arn:aws:iam::${yourAWSaccountID}:role/AmazonEKS_EBS_CSI_DriverRole --force
```
## Deploy TG containers

Deploy the containers using the script `k8s/tg` from [tigergraph/ecosys](https://github.com/tigergraph/ecosys.git). The recommended value for persistent volume, cpu and memory are ~20% smaller than those of a single machine. Thus, each machine has exactly one pod.

If the ebs csi driver is installed, then install k8s operator

### 1. Prerequisite: install Helm
```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
```
### 2. Install Cert-manager for K8S for every cluster
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.8.0/cert-manager.yaml 
```

#Verify installation of cert-manager
```bash
kubectl wait deployment -n cert-manager cert-manager --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-cainjector --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-webhook --for condition=Available=True --timeout=90s
```
### 3. Create a secret

```bashcat <<EOF | kubectl apply -f -
apiVersion: v1
data:
  .dockerconfigjson: eyJhdXRocyI6eyJodHRwczovL2luZGV4LmRvY2tlci5pby92MS8iOnsiYXV0aCI6ImFtVnljbmxtWVc1NVlXNW5PbFJwWjJWeVozSmhjR2hBTWpBeU1nPT0ifX19
kind: Secret
metadata:
  name: tigergraph-image-pull-secret
type: kubernetes.io/dockerconfigjson
EOF
```
### 4. Install Tigergraph kubectl Plugin
#Ask for kubectl-tg file then
```bash
sudo install kubectl-tg /usr/local/bin/
kubectl tg help
```
### 5. Install Tigergrap Operator 
Install Tigergraph Operator with Helm
```bash
export VERSION=0.0.4
export DOCKER_REGISTRY=docker.io
export DOCKER_IMAGE_REPO=tginternal
export HELM_REPO=http://docker.tigergraph.com
export TG_CLUSTER_VERSION_DEFAULT=3.7.0
kubectl tg init --helm-repo ${HELM_REPO} --docker-registry ${DOCKER_REGISTRY} --docker-image-repo ${DOCKER_IMAGE_REPO} --image-pull-policy Always --operator-version ${VERSION}
```

### 6. Create TG cluster -- install PODS
#please make sure the value of --storage-class
#if you're not sure about the value of --storage-class, 
#you can execute kubectl get storageclass to query it.
``` 
kubectl tg create --cluster-name tigergraph --size 4 --ha 1 --docker-registry ${DOCKER_REGISTRY} --docker-image-repo ${DOCKER_IMAGE_REPO} \
    --version ${TG_CLUSTER_VERSION_DEFAULT} --storage-class gp2 --storage-size 400G --cpu 25000m --memory 200Gi 
```
Note: --cpu 25000m means 25 CPUs


## Verify deployment

Deployment can take several minutes. Use `kubectl get pod` to verify the deployment. An example output is

```
NAME              READY   STATUS    RESTARTS   AGE
installer-cztjf   1/1     Running   0          5m23s
tigergraph-0      1/1     Running   0          5m24s
tigergraph-1      1/1     Running   0          3m11s
...
``` 
Alternative verification commands include:
```
kubectl get all 
kubectl describe pod/tigergraph-0
kubectl describe pvc 

```


## Download data
To download the data, service key json file must be located in ```k8s/``` . The bucket is public now and any service key should work.
1. Fill in the parameters in `vars.sh`.
    * `NUM_NODES` - number of nodes.
    * `SF` - data source, choices are 100, 300, 1000, 3000, 10000.
    * `DOWNLOAD_THREAD` - number of download threads    
         


         
1. Put your own service key file in ```tigergraph/``` folder.
    
    Our bucket is public, and any google cloud service key is able to access the data. To create service key, refer to Google Cloud documentation.
    

1. Run:

    ```bash
    ./download.sh
    ```
    It will start background processes on each pods to download and decompress the data.

1. To check if the data is downloaded successfully, log into the cluster using `kubectl exec -it tigergraph-0 -- bash` and then run

    ```bash
    grun all 'tail ~/log.download' # last line should be 'download and decompress finished'
    grun all 'du -sh  ~/tigergraph/data/sf*/' # The data should be SF / NODE_NUMBER
    ```

## Run Benchmark 

1. Log into the k8s cluster 

    ```bash
    kubectl exec -it tigergraph-0 -- bash
    ```

1. In the container, run the following command. (It is recommended to run scripts in the background because it usualy takes long time for large scale factors.)

    ```bash
    nohup ./k8s/setup.sh > log.setup 2>&1 < /dev/null &
    ```

1. To run benchmark scripts

    ```bash
    nohup ./k8s/benchmark.sh > log.benchmark 2>&1 < /dev/null &
    ```

    The `queries.sh` and `batches.sh` can be run in the similar approach. The outputs are in `~/output`. To download, 
    * Compress using tar `tar -cvf output.tar log.benchmark output/` 
    * On local desktop, `kubectl cp tigergraph-0:output.tar output.tar`

1. To reset TigerGraph database:

    ```bash
    gsql drop all
    nohup ./k8s/setup.sh > log.setup 2>&1 < /dev/null &
    ```

## Release the cluter

```bash
# to delete the K8S pods and volumes
kubectl delete all -l app=tigergraph 
kubectl delete pvc -l app=tigergraph 
kubectl delete namespace 
```
### to delete EKS cluster
```
eksctl delete cluster --name sf1000
```

## B. Benchmark on GKE
## Create the cluster

Create [GKE container cluster](https://cloud.google.com/sdk/gcloud/reference/container/clusters/create) specifying machine type, number of nodes, disk size and disk type. This step can take long time.

```bash
gcloud container clusters create sf1000 --machine-type n2d-highmem-32 --num-nodes=4 --disk-size 700 --disk-type=pd-ssd
```
## Deploy TG containers
Deploy the containers using the script `k8s/tg` from [tigergraph/ecosys](https://github.com/tigergraph/ecosys.git). The recommended value for persistent volume, cpu and memory are ~20% smaller than those of a single machine. Thus, each machine has exactly one pod.

```
git clone https://github.com/tigergraph/ecosys.git
cd ecosys/k8s
./tg gke kustomize -v 3.7.0 -n tigergraph -s 4 --pv 700 --cpu 30 --mem 200 -l [license string]
kubectl apply -f ./deploy/tigergraph-eks-tigergraph.yaml
```
## Verify deployment

Deployment can take several minutes. Use `kubectl get pod -n tigergraph` to verify the deployment. An example output is

```
NAME              READY   STATUS    RESTARTS   AGE
installer-cztjf   1/1     Running   0          5m23s
tigergraph-0      1/1     Running   0          5m24s
tigergraph-1      1/1     Running   0          3m11s
...
``` 
Alternative verification commands include:
```
kubectl get all -n tigergraph
kubectl describe pod/tigergraph-0 -n tigergraph
kubectl describe pvc -n tigergraph

```

## Download data
To download the data, service key json file must be located in ```k8s/``` . The bucket is public now and any service key should work.
1. Fill in the parameters in `vars.sh`.
    * `NUM_NODES` - number of nodes.
    * `SF` - data source, choices are 100, 300, 1000, 3000, 10000.
    * `DOWNLOAD_THREAD` - number of download threads    
         


         
1. Put your own service key file in ```tigergraph/``` folder.
    
    Our bucket is public, and any google cloud service key is able to access the data. To create service key, refer to Google Cloud documentation.
    

1. Run:

    ```bash
    ./download.sh
    ```
    It will start background processes on each pods to download and decompress the data.

1. To check if the data is downloaded successfully, log into the cluster using `kubectl exec -it tigergraph-0 -n tigergraph -- bash` and then run

    ```bash
    grun all 'tail ~/log.download' # last line should be 'download and decompress finished'
    grun all 'du -sh  ~/tigergraph/data/sf*/' # The data should be SF / NODE_NUMBER
    ```

## Run Benchmark 

1. Log into the k8s cluster 

    ```bash
    kubectl exec -it tigergraph-0 -n tigergraph -- bash
    ```

1. In the container, run the following command. (It is recommended to run scripts in the background because it usualy takes long time for large scale factors.)

    ```bash
    nohup ./k8s/setup.sh > log.setup 2>&1 < /dev/null &
    ```

1. To run benchmark scripts

    ```bash
    nohup ./k8s/benchmark.sh > log.benchmark 2>&1 < /dev/null &
    ```

    The `queries.sh` and `batches.sh` can be run in the similar approach. The outputs are in `~/output`. To download, 
    * Compress using tar `tar -cvf output.tar log.benchmark output/` 
    * On local desktop, `kubectl cp tigergraph-0:output.tar output.tar`

1. To reset TigerGraph database:

    ```bash
    gsql drop all
    nohup ./k8s/setup.sh > log.setup 2>&1 < /dev/nul
    ```
    ## Release the cluter
    ```
    # to delete GKE cluster
    gcloud container clusters delete snb-bi-tg
    ```

