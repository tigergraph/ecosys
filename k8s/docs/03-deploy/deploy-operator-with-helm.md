# Deploy TigerGraph Operator with Helm

- [Deploy TigerGraph Operator with Helm](#deploy-tigergraph-operator-with-helm)
  - [Overview](#overview)
  - [Prerequisites](#prerequisites)
  - [Install the TigerGraph Operator using Helm Charts](#install-the-tigergraph-operator-using-helm-charts)
    - [Install cert-manager for Kubernetes](#install-cert-manager-for-kubernetes)
    - [Add the TigerGraph Operator Repo to Helm](#add-the-tigergraph-operator-repo-to-helm)
    - [Install the Operator](#install-the-operator)
    - [Verify the Operator installation](#verify-the-operator-installation)
  - [Upgrade the TigerGraph Operator using Helm](#upgrade-the-tigergraph-operator-using-helm)
  - [Uninstall the TigerGraph Operator using Helm](#uninstall-the-tigergraph-operator-using-helm)
  - [See also](#see-also)

## Overview

Helm is a tool for automating the deployment of applications to Kubernetes clusters. A Helm chart is a set of YAML files, templates, and other files that define the deployment details. The following procedure uses a Helm Chart to install the TigerGraph Kubernetes Operator to a Kubernetes cluster.

## Prerequisites

Before proceeding, ensure you have the following prerequisites in place:

- [Helm](https://helm.sh/docs/intro/install/): Helm version >= 3.7.0. TigerGraph Kubernetes Operator is packaged as a Helm chart, so Helm must be installed.

- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): kubectl version >= 1.23. The kubectl-tg plugin relies on kubectl for managing Kubernetes clusters.

- An existing Kubernetes cluster with appropriate permissions:
  
## Install the TigerGraph Operator using Helm Charts

The following procedure installs the Operator using the TigerGraph Operator Chart Repository.

### Install cert-manager for Kubernetes

The TigerGraph Operator uses the Admission Webhooks feature and relies on [cert-manager](https://github.com/jetstack/cert-manager) for provisioning certificates for the webhook server.

Admission webhooks are HTTP callbacks that receive admission requests and do something with them. It is registered with Kubernetes and will be called by Kubernetes to validate or mutate a resource before being stored.

Follow these commands to install cert-manager:

> [!WARNING]
> Please check whether cert-manager has been installed before execute the following command.

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.8.0/cert-manager.yaml 
# Verify installation of cert-manager 
kubectl wait deployment -n cert-manager cert-manager --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-cainjector --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-webhook --for condition=Available=True --timeout=90s
```

### Add the TigerGraph Operator Repo to Helm

TigerGraph maintains a Helm-compatible repository at https://dl.tigergraph.com/charts. Add this repository to Helm:

```bash
helm repo add tigergraph-repo https://dl.tigergraph.com/charts 
```

You can validate the repo contents using helm search:

```bash
helm search repo tigergraph-repo
```

The response should resemble the following:

```bash
NAME                            CHART VERSION   APP VERSION     DESCRIPTION                                    
tigergraph-repo/tg-operator     1.3.0           1.3.0           A Helm chart for TigerGraph Kubernetes Operator
```

### Install the Operator

Run the `helm install` command to install the Operator. The following command specifies and creates a dedicated `tigergraph` namespace for installation.

```bash
export SET_VALUE="replicas=3,image=docker.io/tigergraph/tigergraph-k8s-operator:1.3.0,jobImage=docker.io/tigergraph/tigergraph-k8s-init:1.3.0,pullPolicy=Always,resources.requests.cpu=1000m,resources.requests.memory=1024Mi,maxConcurrentReconcilesOfTG=2,maxConcurrentReconcilesOfBackup=2,maxConcurrentReconcilesOfBackupSchedule=2,maxConcurrentReconcilesOfRestore=2"
export NAMESPACE="tigergraph"
export CHART_VERSION="1.3.0"

helm install tg-operator tigergraph-repo/tg-operator --set ${SET_VALUE} --namespace "${NAMESPACE}" --version ${CHART_VERSION} --create-namespace
```

You can customize the operator configuration by changing the option `--set`, all of the configurations are following:

```yaml
# Default values for tg-operator.
# This is a YAML-formatted file.
# Declare variables to be passed into your templates.

# Default values for deployment replicas of operator
replicas: 3
# image is the docker image of operator
image: docker.io/tigergraph/tigergraph-k8s-operator:1.3.0
# jobImage is the docker image of cluster operation(int, upgrade, scale and so on) job
jobImage: docker.io/tigergraph/tigergraph-k8s-init:1.3.0
pullPolicy: IfNotPresent
# imagePullSecret is the docker image pull secret of operator
imagePullSecret: tigergraph-image-pull-secret
# watchNameSpaces are the namespaces which operator watch, multiple namespaces separated by comma, empty indicates watch all namespaces
watchNameSpaces: ""
# clusterScope is whether the operator has ClusterRole
clusterScope: true
# maxConcurrentReconciles of controllers, defaults to 2
maxConcurrentReconcilesOfTG: 2
maxConcurrentReconcilesOfBackup: 2
maxConcurrentReconcilesOfBackupSchedule: 2
maxConcurrentReconcilesOfRestore: 2
# resources are resources requests configuration of operator
resources:
  requests:
    cpu: 1000m
    memory: 1024Mi
  limits:
    cpu: 2000m
    memory: 4096Mi
# nodeSelector is the nodeSelector of operator pods
nodeSelector: null
```

### Verify the Operator installation

Check the contents of the specified namespace to ensure all pods and services have started successfully.

```bash
kubectl get all -n "${NAMESPACE}"
```

The response should resemble the following:

```bash
$ kubectl get all -n tigergraph
NAME                                                          READY   STATUS    RESTARTS   AGE
pod/tigergraph-operator-controller-manager-754fdcf879-9gzxt   2/2     Running   0          17s
pod/tigergraph-operator-controller-manager-754fdcf879-k7sbx   2/2     Running   0          17s
pod/tigergraph-operator-controller-manager-754fdcf879-tz622   2/2     Running   0          17s

NAME                                                             TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
service/tigergraph-operator-controller-manager-metrics-service   ClusterIP   10.96.190.116   <none>        8443/TCP   17s
service/tigergraph-operator-webhook-service                      ClusterIP   10.96.173.217   <none>        443/TCP    17s

NAME                                                     READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/tigergraph-operator-controller-manager   3/3     3            3           17s

NAME                                                                DESIRED   CURRENT   READY   AGE
replicaset.apps/tigergraph-operator-controller-manager-754fdcf879 
```

Ensure that the operator has been successfully deployed:

```bash
kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${NAMESPACE}
```

## Upgrade the TigerGraph Operator using Helm

Run the `helm upgrade` command to upgrade the Operator. This command can be used to upgrade the Operator version and update other Operator configurations.

```bash
export SET_VALUE="replicas=1,image=docker.io/tigergraph/tigergraph-k8s-operator:1.3.0,jobImage=docker.io/tigergraph/tigergraph-k8s-init:1.3.0,pullPolicy=Always,resources.requests.cpu=1000m,resources.requests.memory=1024Mi,maxConcurrentReconcilesOfTG=1,maxConcurrentReconcilesOfBackup=1,maxConcurrentReconcilesOfBackupSchedule=1,maxConcurrentReconcilesOfRestore=1"
export NAMESPACE="tigergraph"
export CHART_VERSION="1.3.0"

helm upgrade tg-operator tigergraph-repo/tg-operator --set ${SET_VALUE} --namespace "${NAMESPACE}" --version ${CHART_VERSION}
```

## Uninstall the TigerGraph Operator using Helm

Run the `helm uninstall` command to uninstall the Operator.

```bash
helm uninstall tg-operator --namespace "${NAMESPACE}"
```

Delete TigerGraph CRDs if necessary:

> [!IMPORTANT]
> There is currently no support for deleting CRDs when uninstalling the TigerGraph Operator due to the risk of unintentional data loss.
> However, you can manually delete them with the following commands. Please note that deleting TigerGraph CRDs will automatically delete the TigerGraph CRs.

```bash
kubectl delete crd tigergraphbackups.graphdb.tigergraph.com
kubectl delete crd tigergraphbackupschedules.graphdb.tigergraph.com
kubectl delete crd tigergraphrestores.graphdb.tigergraph.com
kubectl delete crd tigergraphs.graphdb.tigergraph.com
```

## See also

If you are interested in the details of managing operator using `kubectl-tg` plugin, refer to the following documents:

- [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md)
- [Deploy TigerGraph on AWS EKS](../03-deploy/tigergraph-on-eks.md)
- [Deploy TigerGraph on Google Cloud GKE](../03-deploy/tigergraph-on-gke.md)
- [Deploy TigerGraph on Red Hat OpenShift](../03-deploy/tigergraph-on-openshift.md)
- [Deploy TigerGraph on Azure Kubernetes Service (AKS)](../03-deploy/tigergraph-on-aks.md)
