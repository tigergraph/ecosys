# How to upgrade TigerGraph Kubernetes Operator

This document provides step-by-step instructions for upgrading the TigerGraph Kubernetes Operator using the kubectl-tg plugin.

- [How to upgrade TigerGraph Kubernetes Operator](#how-to-upgrade-tigergraph-kubernetes-operator)
  - [Install Operator 0.0.7 and TigerGraph 3.9.2](#install-operator-007-and-tigergraph-392)
    - [Install Operator 0.0.7](#install-operator-007)
    - [Install TigerGraph 3.9.2](#install-tigergraph-392)
  - [Upgrade Operator and CRD](#upgrade-operator-and-crd)
    - [Install the latest kubectl-tg plugin](#install-the-latest-kubectl-tg-plugin)
    - [Upgrade CRD to the latest version](#upgrade-crd-to-the-latest-version)
    - [Upgrade Operator to the latest version](#upgrade-operator-to-the-latest-version)
  - [How to upgrade for mandatory change](#how-to-upgrade-for-mandatory-change)
  - [How to upgrade for optional change](#how-to-upgrade-for-optional-change)
  - [How to upgrade for a specific breaking change](#how-to-upgrade-for-a-specific-breaking-change)
    - [Key considerations for upgrading Operator to 0.1.0 and TigerGraph to 3.10.0](#key-considerations-for-upgrading-operator-to-010-and-tigergraph-to-3100)
      - [Exposing Nginx Service Instead of Exposing RESTPP and GST (Tools,GUI) Services](#exposing-nginx-service-instead-of-exposing-restpp-and-gst-toolsgui-services)
      - [Performing a Full Backup Before Running HA, Shrink, and Expand Operations](#performing-a-full-backup-before-running-ha-shrink-and-expand-operations)
      - [Upgrade TG Cluster](#upgrade-tg-cluster)
        - [Upgrading a legacy TG Cluster](#upgrading-a-legacy-tg-cluster)
        - [Upgrading to a TG Cluster with Multiple PVCs](#upgrading-to-a-tg-cluster-with-multiple-pvcs)

## Install Operator 0.0.7 and TigerGraph 3.9.2

If you have previously installed an older version of the Operator and TigerGraph cluster, you can skip this section. This section is only for verifying operator upgrading.

### Install Operator 0.0.7

```bash
curl https://dl.tigergraph.com/k8s/0.0.7/kubectl-tg -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/

kubectl tg init --operator-size 1 --operator-cpu 1000m  --operator-memory 1024Mi -n tigergraph
```

### Install TigerGraph 3.9.2

- Create private ssh key name of TigerGraph pod

```bash
# create a new private keys
echo -e 'y\\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''

# Create a Secret of K8s with above ssh key files
kubectl create secret generic ssh-key-secret --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace YOUR_NAME_SPACE
```

- Create TG cluster with the above secret name

```bash
kubectl tg create --cluster-name test-cluster --private-key-secret ssh-key-secret --license xxxxxxxxxxxxxxxxxxxxxxxxx \
--size 3 --ha 2 --version 3.9.2 \
--storage-class standard --storage-size 100G --cpu 4000m --memory 8Gi -n tigergraph
```

You can also load graph data and verify it after upgrading the Operator.

## Upgrade Operator and CRD

Starting from Operator version 0.0.4, you can upgrade the Operator. The latest available Operator version is 0.0.7; please update it according to your requirements.

### Install the latest kubectl-tg plugin

```bash
curl https://dl.tigergraph.com/k8s/latest/kubectl-tg -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### Upgrade CRD to the latest version

Use the following command to upgrade the Custom Resource Definition (CRD) to version latest:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/latest/tg-operator-crd.yaml
```

### Upgrade Operator to the latest version

The following command will upgrade the operator version to 0.0.9:

```bash
kubectl tg upgrade --namespace ${YOUR_NAMESPACE} --operator-version 0.0.9
```

If you only need to update the Operator's configuration without changing its version, use the following command:

```bash
kubectl tg upgrade --version ${OPERATOR_VERSION} --operator-size 3 --operator-watch-namespace ${YOUR_NAMESPACE} --operator-cpu 1000m  --operator-memory 1024Mi --namespace ${YOUR_NAMESPACE}
```

## How to upgrade for mandatory change

In case of mandatory changes, such as adding a new mandatory option for the secret name of the private SSH key in Operator 0.0.4,
After upgrading the operator from 0.0.3 to 0.0.4, it will throw the following error if you do some update operation.

```bash
kubectl tg update --cluster-name test-cluster --memory 5Gi -n tigergraph
The CR of cluster test-cluster is exported to /home/graphsql/e2e/test-cluster_backup_1679469286.yaml before update cluster, you can use this file for recovery
Warning: resource tigergraphs/test-cluster is missing the kubectl.kubernetes.io/last-applied-configuration annotation which is required by kubectl apply. kubectl apply should only be used on resources created declaratively by either kubectl create --save-config or kubectl apply. The missing annotation will be patched automatically.
Error from server (the secret name of private ssh key is required for TG version 3.8.0): error when applying patch:
.......
to:
Resource: "graphdb.tigergraph.com/v1alpha1, Resource=tigergraphs", GroupVersionKind: "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraph"
Name: "test-cluster", Namespace: "tigergraph"
for: "STDIN": admission webhook "vtigergraph.kb.io" denied the request: the secret name of private ssh key is required for TG version 3.8.0
```

Follow these steps after upgrading from  Operator 0.0.3 to 0.0.4 for mandatory changes:

- Delete cluster

> [!WARNING]
> Don't delete the pvc of the cluster manually.

```bash
kubectl tg delete --cluster-name test-cluster -n tigergraph
```

- Create a private ssh key secret

```bash
echo -e 'y\\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''

kubectl create secret generic ssh-key-secret --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace tigergraph
```

- Recreate the cluster with the created private ssh key

> [!WARNING]
> Cluster size, ha and name must be the same.

```bash
kubectl tg create --cluster-name test-cluster --license xxxxxxxxxxxxxxxxxxxxxxxxx \
--size 3 --ha 2 --version 3.8.0 \
--storage-class standard --storage-size 10G --cpu 2000m --memory 6Gi -n tigergraph --private-key-secret ssh-key-secret
```

## How to upgrade for optional change

If you don't require the new optional configuration of the CRD, no extra steps are needed. However,
if you wish to use the new optional configuration, you can simply update the cluster as needed.

## How to upgrade for a specific breaking change

In order to optimize the user experience of the operator, such as improving ease of use and removing some configurations that are no longer used, Operator upgrading and TigerGraph upgrading may bring breaking changes. If you have an old version of Operator and TigerGraph, please follow the key considerations and upgrading steps carefully.

### Key considerations for upgrading Operator to 0.1.0 and TigerGraph to 3.10.0

#### Exposing Nginx Service Instead of Exposing RESTPP and GST (Tools,GUI) Services

- The new Custom Resource Definition (CRD) is applicable starting from TigerGraph 3.9.2. In the TigerGraph Cluster before 3.9.2, after the CRD is updated, the image must be upgraded to 3.9.2 or later.

 ```bash
    kubectl tg update --cluster-name ${cluster_name} --version 3.9.2 -n ${NAMESPACE_OF_YOUR_CLUSTER}
  ```

- If using the new CRD (0.1.0) with the old version TG image, the NGINX service cannot serve correctly on Tools, RESTPP, and informant services.
- If using the old CRD (<=0.0.9), the Tools (GUI), RESTPP, and informant services cannot serve on 3.6.3 but can serve on 3.7.0, 3.8.0, 3.9.1, 3.9.2, and 3.9.3.

#### Performing a Full Backup Before Running HA, Shrink, and Expand Operations

If the HA Update process is broken by an interrupter operation such as the update pod being killed or the update job being killed, then the TG Cluster may be damaged and unrecoverable automatically, even if the HA update pod is recreated and reruns the HA update job. As a customer, it is necessary to prepare a full backup before running the HA update.

> [!WARNING]
> If the license is expired, the shrink/expand process will be stuck for a long time at exporting/importing graph data and will finally fail.

#### Upgrade TG Cluster

##### Upgrading a legacy TG Cluster

- The best and fastest way to upgrade from an old operator + CRD to a new operator + CRD is:
  
  - Uninstall operator
  
    ```bash
    kubectl tg uninstall
    ```

  - Delete all CRDs

    ```bash
    kubectl delete crd tigergraphbackups.graphdb.tigergraph.com
    kubectl delete crd tigergraphbackupschedules.graphdb.tigergraph.com
    kubectl delete crd tigergraphrestores.graphdb.tigergraph.com
    kubectl delete crd tigergraphs.graphdb.tigergraph.com
    ```

  - **DO NOT** delete PVCs of the TigerGraph Cluster
  - Install the new operator, it applies the new CRD
  - Upgrade the TigerGraph version before uninstalling the legacy operator or after installing the new operator.

##### Upgrading to a TG Cluster with Multiple PVCs

Best Practice for Upgrading TigerGraph <=3.9.3 with only one PVC and Operator <=0.0.9 to TigerGraph 3.10.0 with multiple PVCs and Operator 0.1.0:

- In the environment of TigerGraph <=3.9.3 and Operator <=0.0.9, perform a backup of TigerGraph to S3 or local storage.

```bash
kubectl tg backup create
          --namespace $ns \
          --name $name \
          --cluster-name $YOUR_CLUSTER_NAME \
          --destination s3Bucket \
          --s3-bucket $bucket \
          --tag $tag \
          --timeout $time_out \
          --aws-secret $aws_secret
```

- If using local backup, ensure the local backup is stored outside the Pod for persistent storage.
- Remove the legacy TG and delete TigerGraph's PVC.

```bash
kubectl tg delete --cluster-name $YOUR_CLUSTER_NAME --namespace $YOUR_NAMESPACE --cascade
kubectl delete pvc --namespace default -l tigergraph.com/cluster-name=$YOUR_CLUSTER_NAME
```

- Uninstall the legacy Operator and delete all TigerGraph CRDs.

```bash
kubectl tg uninstall
kubectl delete crd tigergraphbackups.graphdb.tigergraph.com
kubectl delete crd tigergraphbackupschedules.graphdb.tigergraph.com
kubectl delete crd tigergraphrestores.graphdb.tigergraph.com
kubectl delete crd tigergraphs.graphdb.tigergraph.com
```

- Install the new Operator 0.1.0, which will concurrently install new CRDs.

```bash
curl https://dl.tigergraph.com/k8s/0.1.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/

kubectl tg init --namespace $YOUR_NAMESPACE --docker-registry docker.io --docker-image-repo tigergraph --image-pull-policy Always \
--operator-version 0.1.0 --operator-size 3 --cluster-scope true
```

- Create a new TG with the same version as before and customize by adding `additionalStorage` and `customVolume`.
  
```bash
kubectl tg restore \
          --namespace $ns \
          --name "${name}-restore" \
          --cluster-name $YOUR_CLUSTER_NAME \
          --source s3Bucket \
          --s3-bucket $bucket \
          --tag $tag \
          --aws-secret $aws_secret
```

- Upon completion of the restore process, upgrade the TigerGraph to 3.10.0 using the appropriate command.
  
```bash
kubectl tg update --cluster-name $YOUR_CLUSTER_NAME --version 3.10.0 --namespace $YOUR_NAMESPACE
```
