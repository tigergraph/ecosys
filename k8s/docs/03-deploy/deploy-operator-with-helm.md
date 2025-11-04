# Deploy TigerGraph Operator with Helm

- [Deploy TigerGraph Operator with Helm](#deploy-tigergraph-operator-with-helm)
  - [Overview](#overview)
  - [Prerequisites](#prerequisites)
  - [Install cert-manager for Kubernetes](#install-cert-manager-for-kubernetes)
  - [Install the TigerGraph Operator using Helm Charts](#install-the-tigergraph-operator-using-helm-charts)
    - [Add the TigerGraph Operator Repo to Helm](#add-the-tigergraph-operator-repo-to-helm)
    - [Install the TigerGraph Operator](#install-the-tigergraph-operator)
    - [Verify the Operator installation](#verify-the-operator-installation)
  - [Upgrade the TigerGraph Operator using Helm](#upgrade-the-tigergraph-operator-using-helm)
  - [Uninstall the TigerGraph Operator using Helm](#uninstall-the-tigergraph-operator-using-helm)
  - [See also](#see-also)

## Overview

Helm is a tool for automating the deployment of applications to Kubernetes clusters. A Helm chart is a collection of YAML files, templates, and supporting files that define the deployment details. This guide demonstrates how to use the TigerGraph Operator Helm Chart to install the TigerGraph Kubernetes Operator on your cluster.

## Prerequisites

Before you begin, ensure you have the following:

- [Helm](https://helm.sh/docs/intro/install/): Version >= 3.7.0.
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): Version >= 1.23.
- An existing Kubernetes cluster with appropriate permissions.

## Install cert-manager for Kubernetes

The TigerGraph Operator uses Admission Webhooks and relies on [cert-manager](https://github.com/jetstack/cert-manager) to provision certificates for the webhook server.

> [!NOTE]  
> Please check if cert-manager is already installed before running the following command.

Install cert-manager:

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.17/cert-manager.yaml 
```

Verify the cert-manager installation:

```bash
kubectl wait deployment -n cert-manager cert-manager --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-cainjector --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-webhook --for condition=Available=True --timeout=90s
```

## Install the TigerGraph Operator using Helm Charts

The following steps install the Operator using the TigerGraph Helm Chart repository.

### Add the TigerGraph Operator Repo to Helm

Add the TigerGraph Helm repository:

```bash
helm repo add tigergraph-repo https://dl.tigergraph.com/charts 
```

Validate the repository contents:

```bash
helm search repo tigergraph-repo
```

Expected output:

```bash
NAME                            CHART VERSION   APP VERSION     DESCRIPTION                                    
tigergraph-repo/tg-operator     1.6.0           1.6.0           A Helm chart for TigerGraph Kubernetes Operator
```

### Install the TigerGraph Operator

Install the Operator in a dedicated namespace. You can customize the configuration by editing a `values.yaml` file as shown below:

```yaml
# values.yaml example
replicas: 3
image: docker.io/tigergraph/tigergraph-k8s-operator:1.7.0
jobImage: docker.io/tigergraph/tigergraph-k8s-init:1.7.0
pullPolicy: IfNotPresent
imagePullSecret: tigergraph-image-pull-secret
watchNameSpaces: ""
clusterScope: true
maxConcurrentReconcilesOfTG: 2
maxConcurrentReconcilesOfBackup: 2
maxConcurrentReconcilesOfBackupSchedule: 2
maxConcurrentReconcilesOfRestore: 2
resources:
  requests:
    cpu: 1000m
    memory: 1024Mi
  limits:
    cpu: 2000m
    memory: 4096Mi
nodeSelector: null
```

Install the Operator:

```bash
export NAMESPACE=${YOUR_NAMESPACE}
helm install tg-operator tigergraph-repo/tg-operator -f ./values.yaml --namespace ${NAMESPACE} --create-namespace
```

> [!IMPORTANT]
> If you install a **namespace-scoped** TigerGraph Operator (`clusterScope: false`), you must set the `watchNameSpaces` option to the namespace where the Operator is installed. Otherwise, the Operator will attempt to watch all namespaces and fail due to insufficient permissions.

### Verify the Operator installation

Check the resources in the namespace to ensure all pods and services are running:

```bash
kubectl get all -n "${NAMESPACE}"
```

Example output:

```bash
NAME                                                          READY   STATUS    RESTARTS   AGE
pod/tigergraph-operator-controller-manager-754fdcf879-9gzxt   2/2     Running   0          17s
pod/tigergraph-operator-controller-manager-754fdcf879-k7sbx   2/2     Running   0          17s
pod/tigergraph-operator-controller-manager-754fdcf879-tz622   2/2     Running   0          17s

NAME                                                             TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
service/tigergraph-operator-controller-manager-metrics-service   ClusterIP   10.96.190.116   <none>        8443/TCP   17s
service/tigergraph-operator-webhook-service                      ClusterIP   10.96.173.217   <none>        443/TCP    17s

NAME                                                     READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/tigergraph-operator-controller-manager   3/3     3            3           17s
```

Wait for the deployment to become available:

```bash
kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${NAMESPACE}
```

## Upgrade the TigerGraph Operator using Helm

To upgrade the Operator or update its configuration, use:

```bash
export NAMESPACE=${YOUR_NAMESPACE}
export CHART_VERSION="1.6.0"

helm upgrade tg-operator tigergraph-repo/tg-operator -f ./values.yaml --namespace "${NAMESPACE}" --version ${CHART_VERSION}
```

## Uninstall the TigerGraph Operator using Helm

To uninstall the Operator:

```bash
helm uninstall tg-operator --namespace "${NAMESPACE}"
```

> [!IMPORTANT]  
> To uninstall the TigerGraph Operator, you must use the helm uninstall command. Other methods, such as deleting the namespace where the operator is installed, will not remove all associated Kubernetes resources and may cause unexpected errors during reinstallation.

If needed, manually delete TigerGraph CRDs (Custom Resource Definitions):

> [!IMPORTANT]  
> Uninstalling the Operator does not delete CRDs by default to prevent accidental data loss. Deleting CRDs will also delete all TigerGraph custom resources.

```bash
kubectl get crd | grep tigergraph.com | awk '{print $1}' | xargs -I {} kubectl delete crd "{}"
```

## See also

For more information on managing the Operator in other environments, see:

- [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md)
- [Deploy TigerGraph on AWS EKS](../03-deploy/tigergraph-on-eks.md)
- [Deploy TigerGraph on Google Cloud GKE](../03-deploy/tigergraph-on-gke.md)
- [Deploy TigerGraph on Red Hat OpenShift](../03-deploy/tigergraph-on-openshift.md)
- [Deploy TigerGraph on Azure Kubernetes Service (AKS)](../03-deploy/tigergraph-on-aks.md)
- [Deploy TigerGraph on K8s without internet access](../03-deploy/deploy-without-internet.md)
