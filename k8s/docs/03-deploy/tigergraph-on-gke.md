# Deploy TigerGraph on Google Cloud GKE

This comprehensive document provides step-by-step instructions on deploying a TigerGraph cluster on Google Kubernetes Engine (GKE) using Kubernetes.

- [Deploy TigerGraph on Google Cloud GKE](#deploy-tigergraph-on-google-cloud-gke)
  - [Prerequisites](#prerequisites)
  - [Deploy TigerGraph Operator](#deploy-tigergraph-operator)
    - [Install cert-manager for GKE](#install-cert-manager-for-gke)
    - [Install kubectl-tg plugin](#install-kubectl-tg-plugin)
    - [Install CustomResourceDefinitions (Optional)](#install-customresourcedefinitions-optional)
    - [Install TigerGraph Operator](#install-tigergraph-operator)
  - [Deploy a TigerGraph cluster](#deploy-a-tigergraph-cluster)
    - [Providing Private SSH Key Pair for Enhanced Security](#providing-private-ssh-key-pair-for-enhanced-security)
    - [Specify the StorageClass name](#specify-the-storageclass-name)
    - [Create TG cluster with specific options](#create-tg-cluster-with-specific-options)
  - [Connect to a TigerGraph cluster](#connect-to-a-tigergraph-cluster)
    - [Connect to a TigerGraph cluster Pod](#connect-to-a-tigergraph-cluster-pod)
    - [Access TigerGraph Suite](#access-tigergraph-suite)
    - [Access RESTPP API Service](#access-restpp-api-service)
  - [Upgrade a TigerGraph cluster](#upgrade-a-tigergraph-cluster)
  - [Scale a TigerGraph cluster](#scale-a-tigergraph-cluster)
  - [Update the resources(CPU and Memory) of the TigerGraph cluster](#update-the-resourcescpu-and-memory-of-the-tigergraph-cluster)
  - [Destroy the TigerGraph cluster and the Kubernetes Operator](#destroy-the-tigergraph-cluster-and-the-kubernetes-operator)
    - [Destroy the TigerGraph cluster](#destroy-the-tigergraph-cluster)
    - [Uninstall TigerGraph Operator](#uninstall-tigergraph-operator)
    - [Uninstall CRD](#uninstall-crd)
  - [See also](#see-also)

## Prerequisites

Before proceeding, ensure you have the following prerequisites in place:

- [Helm](https://helm.sh/docs/intro/install/): Helm version >= 3.7.0. TigerGraph Kubernetes Operator is packaged as a Helm chart, so Helm must be installed.

- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): kubectl version >= 1.23. The kubectl-tg plugin relies on kubectl for managing Kubernetes clusters.

- Create [GKE cluster](https://cloud.google.com/kubernetes-engine/docs/how-to/creating-a-zonal-cluster) with admin role permission.

## Deploy TigerGraph Operator

To deploy the TigerGraph Operator, follow these steps:

### Install cert-manager for GKE

The TigerGraph Operator uses [cert-manager](https://github.com/jetstack/cert-manager) for provisioning certificates for the webhook server. Cert-manager enables the Admission Webhooks feature.

Admission webhooks are HTTP callbacks that receive admission requests and do something with them. It is registered with Kubernetes and will be called by Kubernetes to validate or mutate a resource before being stored.

Follow these steps to install cert-manager:

> [!WARNING]
> Please check whether cert-manager has been installed before execute the following command.

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.8.0/cert-manager.yaml 
# Verify installation of cert-manager 
kubectl wait deployment -n cert-manager cert-manager --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-cainjector --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-webhook --for condition=Available=True --timeout=90s
```

### Install kubectl-tg plugin

The `kubectl-tg` plugin simplifies deploying and managing the Operator and TigerGraph clusters. Before installing `kubectl-tg`, ensure you meet the following requirements:

- [helm](https://helm.sh/docs/helm/helm_install/): Helm version >= 3.7.0
- [jq](https://jqlang.github.io/jq/download/): jq version >= 1.6
- [yq](https://github.com/mikefarah/yq): yq version >= 4.18.1

Here's an example of installing the latest kubectl-tg, you can change the latest to your desired version, such as 0.0.9:

```bash
wget https://dl.tigergraph.com/k8s/latest/kubectl-tg -O kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

You can check the `kubectl-tg` version and access help information using the following commands:

```bash
kubectl tg version
kubectl tg help
```

### Install CustomResourceDefinitions (Optional)

This step is optional and can be skipped if you have privileged permissions in your Kubernetes environment. The required components will be automatically installed during the Operator installation process. However, if you prefer to install CustomResourceDefinitions (CRDs) independently, you can use the following command:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/latest/tg-operator-crd.yaml
```

### Install TigerGraph Operator

To simplify the Operator installation and TigerGraph cluster deployment, define environment variables:

```bash
export YOUR_NAMESPACE="tigergraph"
export YOUR_CLUSTER_NAME="test-tg-cluster"
export YOUR_SSH_KEY_SECRET_NAME="ssh-key-secret"
```

Now, you can install the TigerGraph Operator based on your requirements:

A namespace-scoped operator watches and manages resources in a single Namespace, whereas a cluster-scoped operator watches and manages resources cluster-wide.

- Install a namespace-scoped Operator:

    ```bash
    kubectl tg init --cluster-scope false --namespace ${YOUR_NAMESPACE}
    ```

- Install a cluster-scoped Operator (default behavior if not specified):

    ```bash
    kubectl tg init --cluster-scope true --namespace ${YOUR_NAMESPACE}
    ```

- Install the Operator with specific options (e.g., version, deployment size, CPU, memory, and more):

  ```bash
  kubectl tg init --cluster-scope false --version ${OPERATOR_VERSION} --operator-size 3 --operator-watch-namespace ${YOUR_NAMESPACE} --operator-cpu 1000m  --operator-memory 1024Mi --namespace ${YOUR_NAMESPACE}
  ```

  For a comprehensive list of options, refer to the output of the `kubectl tg init` --help command.

  ```bash
  kubectl tg init --help
  ```

- Ensure that the operator has been successfully deployed:

    ```bash
    kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${YOUR_NAMESPACE}
    ```

## Deploy a TigerGraph cluster

This section explains how to deploy a TigerGraph cluster on GKE using the kubectl-tg plugin and a Custom Resource (CR) YAML manifest.

### Providing Private SSH Key Pair for Enhanced Security

Starting from Operator version 0.0.4, users are required to provide their private SSH key pair for enhanced security before creating a cluster. Follow these steps:

- Step 1: create a Private SSH Key Pair File

  To enhance cluster security, create a private SSH key pair file:

  ```bash
  echo -e 'y\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''
  ```

- Step 2: create a Secret Object

  > [!IMPORTANT]
  > The namespace of the Secret object must be the same as that of the TigerGraph cluster.

  Create a secret object based on the private SSH key file generated in step 1. Ensure that the key name of the secret for the private SSH key is `private-ssh-key`, and the key name for the public SSH key is `public-ssh-key`. Do not alter these key names:

  ```bash
  kubectl create secret generic ${YOUR_SSH_KEY_SECRET_NAME} --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace ${YOUR_NAMESPACE}
  ```

  For Operator versions 0.0.4 and above, when creating a cluster using the `kubectl tg create command`, you must set the `--private-key-secret` option to `${YOUR_SSH_KEY_SECRET_NAME}`.

These steps enhance the security of your cluster by utilizing your private SSH key pair.

### Specify the StorageClass name

> [!NOTE]
> Here the dynamic persistent volume storage is provided by GKE by default, if you want to use static persistent volume or use them from scratch, please refer to [How to use static & dynamic persistent volume storage](../07-reference/static-and-dynamic-persistent-volume-storage.md).

Before creating the TigerGraph cluster with the Operator, specify the StorageClass, which defines available storage classes. Identify the name of the StorageClass:

```bash
kubectl get storageclass

NAME                     PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
premium-rwo              pd.csi.storage.gke.io   Delete          WaitForFirstConsumer   true                   173m
standard                 kubernetes.io/gce-pd    Delete          Immediate              true                   173m
standard-rwo (default)   pd.csi.storage.gke.io   Delete          WaitForFirstConsumer   true                   173m
```

Choose the appropriate StorageClass (e.g., `standard`) when creating the TigerGraph cluster, ensuring optimized storage provisioning and management.

### Create TG cluster with specific options

To create a new TigerGraph cluster with specific options, use either the `kubectl-tg` plugin or a CR YAML manifest. Below are examples using the `kubectl-tg` plugin:

You can get all of the TigerGraph docker image version from [tigergraph-k8s](https://hub.docker.com/r/tigergraph/tigergraph-k8s/tags)

The following command will create a new TigerGraph cluster with a free license:

- Get and export free license

  ```bash
  export LICENSE=$(curl -L "ftp://ftp.graphtiger.com/lic/license3.txt" -o "/tmp/license3.txt" 2>/dev/null && cat /tmp/license3.txt)
  ```

- Create TigerGraph cluster with kubectl-tg plugin

  ```bash
  kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --private-key-secret ${YOUR_SSH_KEY_SECRET_NAME} --size 3 --ha 2 --version 3.9.3 --license ${LICENSE} \
  --storage-class standard --storage-size 100G --cpu 6000m --memory 16Gi --namespace ${YOUR_NAMESPACE}
  ```

- Alternatively, create a TigerGraph cluster with a CR YAML manifest:
  > [!NOTE]
  > Please replace the TigerGraph version (e.g., 3.9.3) and the Operator version (e.g., 0.0.9) with your desired versions.

  ```bash
  cat <<EOF | kubectl apply -f -
  apiVersion: graphdb.tigergraph.com/v1alpha1
  kind: TigerGraph
  metadata:
    name: ${YOUR_CLUSTER_NAME}
    namespace: ${YOUR_NAMESPACE}
  spec:
    image: docker.io/tigergraph/tigergraph-k8s:3.9.3
    imagePullPolicy: IfNotPresent
    initJob:
      image: docker.io/tigergraph/tigergraph-k8s-init:0.0.9
      imagePullPolicy: IfNotPresent
    initTGConfig:
      ha: 2
      license: ${LICENSE}
      version: 3.9.3
    listener:
      type: LoadBalancer
    privateKeyName: ${YOUR_SSH_KEY_SECRET_NAME}
    replicas: 3
    resources:
      limits:
        cpu: "6"
        memory: 16Gi
      requests:
        cpu: "6"
        memory: 16Gi
    storage:
      type: persistent-claim
      volumeClaimTemplate:
        resources:
          requests:
            storage: 100G
        storageClassName: standard
  EOF
  ```

To ensure the successful deployment of the TigerGraph cluster, use the following command:

```bash
kubectl wait pods -l tigergraph.com/cluster-pod=${YOUR_CLUSTER_NAME} --for condition=Ready --timeout=15m --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=10m  job/${YOUR_CLUSTER_NAME}-init-job --namespace ${YOUR_NAMESPACE}
```

## Connect to a TigerGraph cluster

This section explains how to connect to a TigerGraph cluster and access the `RESTPP` and `GUI` services.

### Connect to a TigerGraph cluster Pod

To log into a single container within the TigerGraph cluster and execute commands like `gadmin status`, use the following command:

```bash
kubectl tg connect --cluster-name ${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}
```

### Access TigerGraph Suite

- Query the external service address.

  ```bash
  export GUI_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-gui-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].ip}')

  echo $GUI_SERVICE_ADDRESS
  35.225.232.251
  ```

- Verify the API service

  ```bash
  curl http://${GUI_SERVICE_ADDRESS}:14240/api/ping

  {"error":false,"message":"pong","results":null}
  ```

Access the TigerGraph Suite in your browser using the URL: http://${GUI_SERVICE_ADDRESS}:14240 (replace `GUI_SERVICE_ADDRESS` with the actual service address).

### Access RESTPP API Service

- Query the external service address.

  ```bash
  export RESTPP_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-rest-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].ip}')

  echo $RESTPP_SERVICE_ADDRESS
  34.173.210.92
  ```

- Verify the RESTPP API service:

  ```bash
  curl http://${RESTPP_SERVICE_ADDRESS}:9000/echo

  {"error":false, "message":"Hello GSQL"}
  ```

## Upgrade a TigerGraph cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during upgrading operations. Currently, TigerGraph does not provide dedicated high-availability upgrade support, and some downtime is involved.

Upgrading a TigerGraph cluster is supported from a lower version to a higher version.

> [!WARNING]
> For TigerGraph 3.9.3 and later versions, the use of passwords to log in to Pods is disabled, which enhances security. If you plan to upgrade your TigerGraph cluster to version 3.9.3, it is essential to first upgrade the Operator to version 0.0.9.

> [!WARNING]
> Operator 0.0.9 has disabled TG downgrades from a higher version (e.g., 3.9.3) to any lower version (e.g., 3.9.2). Therefore, the upgrade job will fail if you attempt to downgrade.

Assuming the current version of the cluster is 3.9.2, you can upgrade it to version 3.9.3 with the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --version 3.9.3  --namespace ${YOUR_NAMESPACE}
```

If you prefer using a CR YAML manifest, update the `spec.initTGConfig.version` and `spec.image` field, and then apply it.

Ensure the successful upgrade with these commands:

```bash
kubectl rollout status --watch --timeout=900s statefulset/${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=15m  job/${YOUR_CLUSTER_NAME}-upgrade-job --namespace ${YOUR_NAMESPACE}
```

## Scale a TigerGraph cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during scaling operations. Currently, TigerGraph does not provide dedicated high-availability scale support, and some downtime is involved.

Before scaling the cluster, scale the corresponding node pool to provide sufficient resources for new instances. Use the following command to scale the TigerGraph cluster:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --size 6 --ha 2  --namespace ${YOUR_NAMESPACE}
```

The above command scales the cluster to a size of 6 with a high availability factor of 2. If you prefer to use a CR (Custom Resource) YAML manifest for scaling, update the `spec.replicas` and `spec.initTGConfig.ha` fields accordingly.

## Update the resources(CPU and Memory) of the TigerGraph cluster

Modify the CPU and memory resources of your TigerGraph cluster using the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --cpu 8 --memory 16Gi  --cpu-limit 8 --memory-limit 16Gi  --namespace ${YOUR_NAMESPACE}
```

For CR YAML manifests, update the `spec.resources.requests` and `spec.resources.limits` fields and apply the changes.

## Destroy the TigerGraph cluster and the Kubernetes Operator

### Destroy the TigerGraph cluster

To delete a TigerGraph cluster, use the following command. Note that this command does not remove Persistent Volume Claims (PVCs) and Persistent Volumes (PVs) associated with the cluster. To delete these components, manually delete the PVCs.

- Delete the TigerGraph cluster and retain the PVs:

  ```bash
  kubectl tg delete --cluster-name ${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  ```

- Delete the PVCs related to the specified cluster:

  ```bash
  # Identify the PVCS to delete by specific labels of PVC.
  kubectl get pvc -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}

  # Delete the PVCS related to the specified cluster.
  kubectl delete pvc -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  ```

### Uninstall TigerGraph Operator

Uninstall the TigerGraph Kubernetes Operator within a specified namespace:

```bash
kubectl tg uninstall -n ${YOUR_NAMESPACE}
```

### Uninstall CRD

Uninstall CRDs if needed:

> [!NOTE]
> Replace the variable `${OPERATOR_VERSION}` to the Operator version you installed.

```bash
kubectl delete -f https://dl.tigergraph.com/k8s/${OPERATOR_VERSION}/tg-operator-crd.yaml
```

## See also

If you are interested in the details of deploying a TigerGraph cluster using the CR (Custom Resource) YAML manifest, refer to the following document:

- [Configuring TigerGraph Clusters on K8s using TigerGraph CR](../07-reference/configure-tigergraph-cluster-cr-with-yaml-manifests.md)
