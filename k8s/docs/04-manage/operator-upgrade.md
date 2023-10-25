# How to upgrade TigerGraph Kubernetes Operator

This document provides step-by-step instructions for upgrading the TigerGraph Kubernetes Operator using the kubectl-tg plugin.

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
