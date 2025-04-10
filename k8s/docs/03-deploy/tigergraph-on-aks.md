# Deploy TigerGraph on Azure Kubernetes Service (AKS)

Starting with version **1.3.0**, the Kubernetes Operator for TigerGraph achieved GA release for AKS. This comprehensive document provides step-by-step instructions for deploying a TigerGraph cluster on Azure Kubernetes Service (AKS).

- [Deploy TigerGraph on Azure Kubernetes Service (AKS)](#deploy-tigergraph-on-azure-kubernetes-service-aks)
  - [Prerequisites](#prerequisites)
  - [Deploy TigerGraph Operator](#deploy-tigergraph-operator)
    - [Install cert-manager for AKS](#install-cert-manager-for-aks)
    - [Install kubectl-tg plugin](#install-kubectl-tg-plugin)
    - [Install CustomResourceDefinitions (Optional)](#install-customresourcedefinitions-optional)
    - [Install TigerGraph Operator](#install-tigergraph-operator)
  - [Deploy a TigerGraph cluster](#deploy-a-tigergraph-cluster)
    - [Providing Private SSH Key Pair for Enhanced Security](#providing-private-ssh-key-pair-for-enhanced-security)
    - [Specify the StorageClass name](#specify-the-storageclass-name)
    - [Specify the additional Storage for mounting multiple PVs(Optional)](#specify-the-additional-storage-for-mounting-multiple-pvsoptional)
    - [Customize configurations for the TigerGraph system (Optional)](#customize-configurations-for-the-tigergraph-system-optional)
    - [Create TG cluster with specific options](#create-tg-cluster-with-specific-options)
  - [Connect to a TigerGraph cluster](#connect-to-a-tigergraph-cluster)
    - [Connect to a TigerGraph cluster Pod](#connect-to-a-tigergraph-cluster-pod)
    - [Access TigerGraph Services](#access-tigergraph-services)
      - [Access TigerGraph Services from inside the Kubernetes cluster](#access-tigergraph-services-from-inside-the-kubernetes-cluster)
      - [Access TigerGraph Services from outside the Kubernetes cluster](#access-tigergraph-services-from-outside-the-kubernetes-cluster)
  - [Upgrade a TigerGraph cluster](#upgrade-a-tigergraph-cluster)
  - [Scale a TigerGraph cluster](#scale-a-tigergraph-cluster)
    - [Change the HA factor of the TigerGraph cluster](#change-the-ha-factor-of-the-tigergraph-cluster)
  - [Update the resources(CPU and Memory) of the TigerGraph cluster](#update-the-resourcescpu-and-memory-of-the-tigergraph-cluster)
  - [Update system configurations and license of the TigerGraph cluster](#update-system-configurations-and-license-of-the-tigergraph-cluster)
  - [Destroy the TigerGraph cluster and the Kubernetes Operator](#destroy-the-tigergraph-cluster-and-the-kubernetes-operator)
    - [Destroy the TigerGraph cluster](#destroy-the-tigergraph-cluster)
    - [Uninstall TigerGraph Operator](#uninstall-tigergraph-operator)
    - [Uninstall CRD](#uninstall-crd)
  - [See also](#see-also)

## Prerequisites

Before proceeding, ensure you have the following prerequisites in place:

- [Helm](https://helm.sh/docs/intro/install/): Helm version >= 3.7.0. TigerGraph Kubernetes Operator is packaged as a Helm chart, so Helm must be installed.

- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): kubectl version >= 1.23. The kubectl-tg plugin relies on kubectl for managing Kubernetes clusters.

- Create [AKS cluster](https://learn.microsoft.com/en-us/azure/aks/learn/quick-kubernetes-deploy-portal?tabs=azure-cli) with admin role permission.

## Deploy TigerGraph Operator

To deploy the TigerGraph Operator, follow these steps:

### Install cert-manager for AKS

The TigerGraph Operator uses [cert-manager](https://github.com/jetstack/cert-manager) for provisioning certificates for the webhook server. Cert-manager enables the Admission Webhooks feature.

Admission webhooks are HTTP callbacks that receive admission requests and do something with them. It is registered with Kubernetes and will be called by Kubernetes to validate or mutate a resource before being stored.

Follow these steps to install cert-manager:

> [!WARNING]
> Please check whether cert-manager has been installed before execute the following command.

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.13/cert-manager.yaml 
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

> [!IMPORTANT]
> The kubectl-tg plugin is only verified on GNU/Linux systems.
>
> If you are using MacOS, you may encounter issues due to the differences between GNU and MacOS commands.
> Please refer to the [troubleshooting document](../05-troubleshoot/kubectl-tg-plugin.md) for more information.
>
> If you are using Windows, please run the commands in a WSL environment.
> Please refer to [Windows Subsystem for Linux Documentation](https://learn.microsoft.com/en-us/windows/wsl/) for more information.

Here's an example of installing the latest kubectl-tg, you can change the latest to your desired version, such as 1.2.0:

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

If you want to install the TigerGraph Operator with Helm, you can refer to [Deploy TigerGraph Operator with Helm](./deploy-operator-with-helm.md).

The example below shows how to install the TigerGraph Operator with the `kubectl-tg` plugin.

To simplify the Operator installation and TigerGraph cluster deployment, define environment variables:

```bash
export YOUR_NAMESPACE="tigergraph"
export YOUR_CLUSTER_NAME="test-tg-cluster"
export YOUR_SSH_KEY_SECRET_NAME="ssh-key-secret"
```

Now, you can install the TigerGraph Operator based on your requirements:

A namespace-scoped operator watches and manages resources in a single Namespace, whereas a cluster-scoped operator watches and manages resources cluster-wide.

Choose a namespace-scoped operator when:

- You need to isolate workloads per team, project, or tenant.

- Security and compliance require limited access.

- You want to deploy multiple instances of an operator for different namespaces.

Choose a cluster-scoped operator when:

- The operator needs to manage resources across namespaces.

- The application requires global policies or configurations.

- You want a single instance of the operator to manage the entire cluster.

> [!IMPORTANT]
> Namespace-scoped operators require the same operator version to be installed for different namespaces.

- Install a namespace-scoped Operator:

    ```bash
    kubectl tg init --cluster-scope false --namespace ${YOUR_NAMESPACE}
    ```

- Install a cluster-scoped Operator (default behavior if not specified):

    ```bash
    kubectl tg init --cluster-scope true --namespace ${YOUR_NAMESPACE}
    ```

- For custom installation options:

  You can customize the installation by specifying options like the Operator version, deployment size, CPU, memory, max concurrent reconciles of controller, and the namespace to watch, among others. Here's an example:

    ```bash
    kubectl tg init --cluster-scope false --version ${OPERATOR_VERSION} --operator-size 3 --operator-watch-namespace ${YOUR_NAMESPACE} \
    --operator-cpu 1000m  --operator-memory 1024Mi \
    --max-tg-concurrent-reconciles 4 \
    --max-backup-concurrent-reconciles 4 \
    --max-backup-schedule-concurrent-reconciles 4 \
    --max-restore-concurrent-reconciles 2 \
    --namespace ${YOUR_NAMESPACE}
    ```

> [!IMPORTANT]
> To ensure high availability of the TigerGraph Operator, set the `--operator-size` option to a value of at least 2.

> [!NOTE]
> You can set the concurrent reconciliation value to a larger value during installation or update it to a suitable value afterward. The default maximum concurrent reconciliation value of 2 is sufficient for most cases. However, you may need to customize it if you use one operator to manage numerous TigerGraph clusters within a Kubernetes cluster.

  For a comprehensive list of options, refer to the output of the `kubectl tg init` --help command.

  ```bash
  kubectl tg init --help
  ```

- Ensure that the operator has been successfully deployed:

    ```bash
    kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${YOUR_NAMESPACE}
    ```

## Deploy a TigerGraph cluster

This section explains how to deploy a TigerGraph cluster on AKS using the kubectl-tg plugin and a Custom Resource (CR) YAML manifest.

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
> Here the dynamic persistent volume storage is provided by AKS by default, if you want to use static persistent volume or use them from scratch, please refer to [How to use static & dynamic persistent volume storage](../07-reference/static-and-dynamic-persistent-volume-storage.md).

Before creating the TigerGraph cluster with the Operator, specify the StorageClass, which defines available storage classes. Identify the name of the StorageClass:

```bash
$ kubectl get storageclass

NAME                    PROVISIONER          RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
azurefile                file.csi.azure.com    Delete          Immediate              true                   6m1s
azurefile-csi            file.csi.azure.com    Delete          Immediate              true                   6m1s
azurefile-csi-premium    file.csi.azure.com    Delete          Immediate              true                   6m1s
azurefile-premium        file.csi.azure.com    Delete          Immediate              true                   6m1s
default (default)       disk.csi.azure.com   Delete          WaitForFirstConsumer   true                   6m1s
managed                 disk.csi.azure.com   Delete          WaitForFirstConsumer   true                   6m1s
managed-csi             disk.csi.azure.com   Delete          WaitForFirstConsumer   true                   6m1s
managed-csi-premium     disk.csi.azure.com   Delete          WaitForFirstConsumer   true                   6m1s
managed-premium         disk.csi.azure.com   Delete          WaitForFirstConsumer   true                   6m1s
```

Choose the appropriate StorageClass (e.g., `default`) when creating the TigerGraph cluster, ensuring optimized storage provisioning and management.

### Specify the additional Storage for mounting multiple PVs(Optional)

You can specify multiple PVs for TigerGraph Pods by specifying the `--additional-storages` option. The value of this option is a YAML file configuration. For example:

> [!NOTE]
> Other parameters required to create a cluster are omitted here.

```bash
kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --additional-storages additional-storage-tg-logs.yaml
```

Example additional storage YAML file:

```YAML
additionalStorages:
    - name: tg-kafka
    storageSize: 5Gi
    - name: tg-log
    storageSize: 5Gi
    - name: tg-sidecar
    storageClassName: efs-sc
    storageSize: 5Gi
    accessMode: ReadWriteMany
    volumeMode: Filesystem
    - name: tg-backup
    storageSize: 5Gi
    mountPath: /home/tigergraph/backup
    accessMode: ReadWriteOnce
    volumeMode: Filesystem
```

You can also specify the multiple PVs using CR configuration, For more information, see [Multiple persistent volumes mounting](../03-deploy/multiple-persistent-volumes-mounting.md)

### Customize configurations for the TigerGraph system (Optional)

You can customize the configurations for the TigerGraph system by specifying the `--tigergraph-config` option. The value of this option should be key-value pairs separated by commas. For example:

```bash
 --tigergraph-config "System.Backup.TimeoutSec=900,Controller.BasicConfig.LogConfig.LogFileMaxSizeMB=40"
```

 The key-value pairs are the same as the configurations that can be set by `gadmin config set` command. For more information, see [Configuration Parameters](https://docs.tigergraph.com/tigergraph-server/current/reference/configuration-parameters). All configurations will be applied to the TigerGraph system when the cluster is initializing.

### Create TG cluster with specific options

To create a new TigerGraph cluster with specific options, use either the `kubectl-tg` plugin or a CR YAML manifest. Below are examples using the `kubectl-tg` plugin:

You can get all of the TigerGraph docker image version from [tigergraph-k8s](https://hub.docker.com/r/tigergraph/tigergraph-k8s/tags)

You must provide your license key when creating cluster. Contact TigerGraph support for help finding your license key.

- Export license key as an environment variable

  ```bash
  export LICENSE=<LICENSE_KEY>
  ```

- Create TigerGraph cluster with kubectl-tg plugin

  ```bash
  kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --private-key-secret ${YOUR_SSH_KEY_SECRET_NAME} --size 3 --ha 2 --version 3.10.1 --license ${LICENSE} \
  --storage-class default --storage-size 100G --cpu 6000m --memory 16Gi --namespace ${YOUR_NAMESPACE}
  ```

- Alternatively, create a TigerGraph cluster with a CR YAML manifest:

> [!NOTE]
> Please replace the TigerGraph docker image version (e.g., 3.10.1) with your desired version.

  ```bash
  cat <<EOF | kubectl apply -f -
  apiVersion: graphdb.tigergraph.com/v1alpha1
  kind: TigerGraph
  metadata:
    name: ${YOUR_CLUSTER_NAME}
    namespace: ${YOUR_NAMESPACE}
  spec:
    image: docker.io/tigergraph/tigergraph-k8s:3.10.1
    imagePullPolicy: IfNotPresent
    ha: 2
    license: ${LICENSE}
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
        storageClassName: default
  EOF
  ```

To ensure the successful deployment of the TigerGraph cluster, use the following command:

```bash
kubectl wait pods -l tigergraph.com/cluster-pod=${YOUR_CLUSTER_NAME} --for condition=Ready --timeout=15m --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=10m  job/${YOUR_CLUSTER_NAME}-init-job --namespace ${YOUR_NAMESPACE}
```

## Connect to a TigerGraph cluster

This section explains how to log into a TigerGraph cluster pod and access the `RESTPP`,`GUI`, and `Metrics` services.

### Connect to a TigerGraph cluster Pod

To log into a single container within the TigerGraph cluster and execute commands like `gadmin status`, use the following command:

```bash
kubectl tg connect --cluster-name ${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}
```

### Access TigerGraph Services

TigerGraph Operator provides two methods for accessing TigerGraph services. You can configure the spec.listener option to create an external service, allowing access to TigerGraph services from outside the Kubernetes cluster.

Additionally, during TigerGraph cluster creation, the operator automatically creates a headless service to facilitate communication between internal TigerGraph pods. This service can be leveraged to access TigerGraph services from within the Kubernetes cluster.

#### Access TigerGraph Services from inside the Kubernetes cluster

Query the headless service:

```bash
kubectl get svc ${YOUR_CLUSTER_NAME}-internal-service --namespace ${YOUR_NAMESPACE}
```

Output example:

```bash
NAME                            TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)                        AGE
test-cluster-internal-service   ClusterIP   None         <none>        9000/TCP,10022/TCP,14240/TCP   6m23s
```

When using the headless service to access the TigerGraph service, you must log into one of the pods in the Kubernetes cluster. The following example demonstrates how to access it from one of the TigerGraph pods.

- Verify the API service

  ```bash
  curl ${YOUR_CLUSTER_NAME}-internal-service.${YOUR_NAMESPACE}.svc.cluster.local:14240/api/ping
  {"error":false,"message":"pong","results":null}
  ```

- Verify the RESTPP API service

  ```bash
  curl ${YOUR_CLUSTER_NAME}-internal-service.${YOUR_NAMESPACE}.svc.cluster.local:14240/restpp/echo
  {"error":false, "message":"Hello GSQL"}
  ```

- Verify the Metrics API service

  ```bash
  curl ${YOUR_CLUSTER_NAME}-internal-service.${YOUR_NAMESPACE}.svc.cluster.local:14240/informant/metrics/get/network -d '{"LatestNum":"1"}'
  {"NetworkMetrics":[{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"702d134a162542ca8109de3dcbc51dea","TimestampNS":"1738829179030264667","Source":{"ServiceName":"EXE","Partition":1}},"HostID":"m1","CollectTimeStamps":"1738829179023049798","Network":{"IP":"10.20.128.210","TCPConnectionNum":382,"IncomingBytesNum":"49794423","OutgoingBytesNum":"48252486"}},{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"dff3d22e6bfd4cf3a2f1d7fb252a4ed6","TimestampNS":"1738829180030864889","Source":{"ServiceName":"EXE","Partition":2}},"HostID":"m2","CollectTimeStamps":"1738829180024734149","Network":{"IP":"10.20.128.74","TCPConnectionNum":284,"IncomingBytesNum":"40035133","OutgoingBytesNum":"38002711"}},{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"e00644e8c3d5414583605e5fccc41e67","TimestampNS":"1738829178029349154","Source":{"ServiceName":"EXE","Partition":3}},"HostID":"m3","CollectTimeStamps":"1738829178022643026","Network":{"IP":"10.20.128.10","TCPConnectionNum":279,"IncomingBytesNum":"36818317","OutgoingBytesNum":"25420121"}}]}
  ```

> [!NOTE]
> You can also access the TigerGraph service through a specific TigerGraph pod by prefixing the pod name. For example: 
> ```bash
> curl ${YOUR_CLUSTER_NAME}-0.${YOUR_CLUSTER_NAME}-internal-service.${YOUR_NAMESPACE}.svc.cluster.local:14240/api/ping
>{"error":false,"message":"pong","results":null}
> ```

#### Access TigerGraph Services from outside the Kubernetes cluster

> [!IMPORTANT]
> Please ensure that the external service is configured before proceeding with the following steps. You can configure the external service using the `spec.listener` field when creating or updating the TigerGraph cluster. Please refer to [External access service](../07-reference/configure-tigergraph-cluster-cr-with-yaml-manifests.md#external-access-service) for more details.

Query the external service address:

  ```bash
  export EXTERNAL_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-nginx-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].ip}')
  ```

- Verify the API service

  ```bash
  curl http://${EXTERNAL_SERVICE_ADDRESS}:14240/api/ping

  {"error":false,"message":"pong","results":null}
  ```

To access the TigerGraph Suite, open it in your browser using the following URL: http://${EXTERNAL_SERVICE_ADDRESS}:14240, replacing `EXTERNAL_SERVICE_ADDRESS` with the actual service address.

- Verify the RESTPP API service

  ```bash
  curl http://${EXTERNAL_SERVICE_ADDRESS}:14240/restpp/echo

  {"error":false, "message":"Hello GSQL"}
  ```

- Verify the Metrics API service

  ```bash
  curl http://${EXTERNAL_SERVICE_ADDRESS}/informant/metrics/get/network -d '{"LatestNum":"1"}'

  {"NetworkMetrics":[{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"1ebeaf2a380f4941b371efaaceb3467b","TimestampNS":"1703666521019463773","Source":{"ServiceName":"EXE","Partition":2}},"HostID":"m2","CollectTimeStamps":"1703666521008230613","Network":{"IP":"10.244.0.79","TCPConnectionNum":89,"IncomingBytesNum":"1654215","OutgoingBytesNum":"1466486"}},{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"2c54ed5d6ba14e789db03fd9e023219c","TimestampNS":"1703666521020024563","Source":{"ServiceName":"EXE","Partition":3}},"HostID":"m3","CollectTimeStamps":"1703666521011409133","Network":{"IP":"10.244.0.78","TCPConnectionNum":90,"IncomingBytesNum":"1637413","OutgoingBytesNum":"1726712"}},{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"c3478943ca134530bcd3aa439521c626","TimestampNS":"1703666521019483903","Source":{"ServiceName":"EXE","Partition":1}},"HostID":"m1","CollectTimeStamps":"1703666521009116924","Network":{"IP":"10.244.0.77","TCPConnectionNum":107,"IncomingBytesNum":"1298257","OutgoingBytesNum":"1197920"}}]}
  ```

## Upgrade a TigerGraph cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during upgrading operations. Currently, TigerGraph does not provide dedicated high-availability upgrade support, and some downtime is involved.

Upgrading a TigerGraph cluster is supported from a lower version to a higher version.

> [!WARNING]
> For TigerGraph 3.9.3 and later versions, the use of passwords to log in to Pods is disabled, which enhances security. If you plan to upgrade your TigerGraph cluster to version 3.9.3, it is essential to first upgrade the Operator to version 0.0.9.

> [!WARNING]
> Operator 0.0.9 has disabled TG downgrades from a higher version (e.g., 3.9.3) to any lower version (e.g., 3.9.2). Therefore, the upgrade job will fail if you attempt to downgrade.

Assuming the current version of the cluster is 3.10.0, you can upgrade it to version 3.10.1 with the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --version 3.10.1  --namespace ${YOUR_NAMESPACE}
```

If you prefer using a CR YAML manifest, update the `spec.image` field, and then apply it.

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

The above command scales the cluster to a size of 6 with a high availability factor of 2. If you prefer to use a CR (Custom Resource) YAML manifest for scaling, update the `spec.replicas` and `spec.ha` fields accordingly.

### Change the HA factor of the TigerGraph cluster

From Operator version 1.0.0, you can change the HA factor of the TigerGraph cluster without updating size by using the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --ha ${NEW_HA} --namespace ${YOUR_NAMESPACE}
```

## Update the resources(CPU and Memory) of the TigerGraph cluster

Modify the CPU and memory resources of your TigerGraph cluster using the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --cpu 8 --memory 16Gi  --cpu-limit 8 --memory-limit 16Gi  --namespace ${YOUR_NAMESPACE}
```

For CR YAML manifests, update the `spec.resources.requests` and `spec.resources.limits` fields and apply the changes.

## Update system configurations and license of the TigerGraph cluster

Use the following command to update the system configurations of the TigerGraph cluster:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --tigergraph-config "System.Backup.TimeoutSec=900,Controller.BasicConfig.LogConfig.LogFileMaxSizeMB=40"  --namespace ${YOUR_NAMESPACE}
```

Use the following command to update the license of the TigerGraph cluster:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --license ${LICENSE}  --namespace ${YOUR_NAMESPACE}
```

If you want to update both the system configurations and license of the TigerGraph cluster, please provide these two options together in one command(**recommanded**) instead of two separate commands:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --tigergraph-config "System.Backup.TimeoutSec=900,Controller.BasicConfig.LogConfig.LogFileMaxSizeMB=40" --license ${LICENSE}  --namespace ${YOUR_NAMESPACE}
```

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
