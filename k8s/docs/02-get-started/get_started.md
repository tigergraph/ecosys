<h1> Getting Started TigerGraph on Kubernetes </h1>

- [Step 1: Create a Test Kubernetes Cluster](#step-1-create-a-test-kubernetes-cluster)
  - [Create a Kubernetes Cluster Using kind](#create-a-kubernetes-cluster-using-kind)
  - [Install MetalLB to Enable Load Balancing Services](#install-metallb-to-enable-load-balancing-services)
- [Step 2: Deploy TigerGraph Operator](#step-2-deploy-tigergraph-operator)
  - [Install cert-manager for Kubernetes](#install-cert-manager-for-kubernetes)
  - [Install kubectl-tg plugin](#install-kubectl-tg-plugin)
  - [Install CRDs independently (Optional)](#install-crds-independently-optional)
  - [Install TigerGraph Operator](#install-tigergraph-operator)
- [Step 3: Deploy a TigerGraph Cluster](#step-3-deploy-a-tigergraph-cluster)
  - [Providing a Private SSH Key Pair for Enhanced Security](#providing-a-private-ssh-key-pair-for-enhanced-security)
  - [Specify the StorageClass Name](#specify-the-storageclass-name)
  - [Specify the additional Storage for mounting multiple PVs(Optional)](#specify-the-additional-storage-for-mounting-multiple-pvsoptional)
  - [Customize configurations for the TigerGraph system (Optional)](#customize-configurations-for-the-tigergraph-system-optional)
  - [Create a TigerGraph Cluster with Specific Options](#create-a-tigergraph-cluster-with-specific-options)
- [Step 4: Connect to a TigerGraph Cluster](#step-4-connect-to-a-tigergraph-cluster)
  - [Connect to a TigerGraph Cluster Pod](#connect-to-a-tigergraph-cluster-pod)
  - [Access TigerGraph Services](#access-tigergraph-services)
    - [Verify the API service](#verify-the-api-service)
    - [Verify the RESTPP API service](#verify-the-restpp-api-service)
    - [Verify the Metrics API service](#verify-the-metrics-api-service)
- [Step 5: Operate a TigerGraph Cluster](#step-5-operate-a-tigergraph-cluster)
  - [Update the Resources (CPU and Memory) of the TigerGraph Cluster](#update-the-resources-cpu-and-memory-of-the-tigergraph-cluster)
  - [Update system configurations and license of the TigerGraph cluster](#update-system-configurations-and-license-of-the-tigergraph-cluster)
  - [Scale a TigerGraph Cluster](#scale-a-tigergraph-cluster)
    - [Change the HA factor of the TigerGraph cluster](#change-the-ha-factor-of-the-tigergraph-cluster)
  - [Upgrade a TigerGraph Cluster](#upgrade-a-tigergraph-cluster)
- [Step 6: Destroy the TigerGraph Cluster and the Kubernetes Operator](#step-6-destroy-the-tigergraph-cluster-and-the-kubernetes-operator)
  - [Destroy the TigerGraph Cluster](#destroy-the-tigergraph-cluster)
  - [Uninstall TigerGraph Operator](#uninstall-tigergraph-operator)
  - [Uninstall CRD](#uninstall-crd)
- [Step 7: Destroy the Kubernetes Cluster](#step-7-destroy-the-kubernetes-cluster)
- [See also](#see-also)

This document provides a step-by-step guide on creating a simple Kubernetes cluster and using it to deploy a basic test TigerGraph cluster using TigerGraph Operator.

To deploy TigerGraph Operator and a TigerGraph cluster, follow these structured steps:

1. Create a Test Kubernetes Cluster: Start by creating a test Kubernetes cluster to serve as the foundation for your TigerGraph deployment.
2. Deploy TigerGraph Operator: Next, deploy the TigerGraph Operator, which is essential for managing TigerGraph clusters within your Kubernetes environment.
3. Deploy a TigerGraph Cluster: Once the Operator is in place, deploy your TigerGraph cluster, setting the stage for your data and graph processing needs.
4. Connect to a TigerGraph Cluster: Learn how to establish a connection to your newly deployed TigerGraph cluster.
5. Operate a TigerGraph Cluster: Explore the various operations you can perform on your TigerGraph cluster, from data management to analytics.
6. Destroy the TigerGraph Cluster and the Kubernetes Operator: When you no longer require your TigerGraph resources, follow proper procedures to safely remove the TigerGraph cluster and the associated Kubernetes Operator.
7. Destroy the Kubernetes Cluster: Finally, if needed, you can dismantle the entire Kubernetes cluster, ensuring efficient resource utilization.

For a visual demonstration of these steps, you can watch the following video:

[Demo slides](https://docs.google.com/presentation/d/1aUpgHnJaz9qhlFqg6sPmLMrPMk2CR0ij4qqktbcZZQQ/edit?usp=sharing)

[Demo video](https://drive.google.com/file/d/1-h70zlrGEYAQRadG_Pfq4HfmXkvEPt8s/view?usp=sharing)

This comprehensive guide and accompanying resources will help you kickstart your journey with TigerGraph Operator, enabling you to harness the power of TigerGraph within your Kubernetes environment.

## Step 1: Create a Test Kubernetes Cluster

This section provides detailed instructions on creating a straightforward Kubernetes cluster using kind. Establishing this Kubernetes cluster serves as a foundational step, enabling you to conduct testing of TigerGraph clusters managed by TigerGraph Operator.

### Create a Kubernetes Cluster Using kind

This section provides a step-by-step guide on deploying a Kubernetes cluster with [kind](https://kind.sigs.k8s.io/).

kind is a widely recognized tool for setting up local Kubernetes clusters, leveraging Docker containers as cluster nodes. For available tags, see [Docker Hub](https://hub.docker.com/r/kindest/node/tags). The default configuration employs the latest version of kind.

Before initiating the deployment process, please ensure that you meet the following prerequisites:

- [Docker](https://docs.docker.com/install/): version >= 20.10
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): version >= 1.23
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/): version >= 0.12.0
- For Linux users, confirm that the sysctl parameter [net.ipv4.ip_forward](https://linuxconfig.org/how-to-turn-on-off-ip-forwarding-in-linux) is set to 1.

Here's an illustrative example utilizing **kind** version 0.20.0:

```shell
kind create cluster
```

<details>
  <summary>Expected output</summary>
Creating cluster "kind" ...<br>
 ✓ Ensuring node image (kindest/node:v1.27.3) 🖼<br>
 ✓ Preparing nodes<br>
 ✓ Writing configuration 📜<br>
 ✓ Starting control-plane 🕹️<br>
 ✓ Installing CNI 🔌<br>
 ✓ Installing StorageClass 💾<br>
Set kubectl context to "kind-kind"<br>
You can now use your cluster with:<br>
<br>
kubectl cluster-info --context kind-kind  
</details>

To verify if the cluster has been successfully created, run:

```shell
kubectl cluster-info
```

<details>
  <summary>Expected output</summary>
Kubernetes control plane is running at https://127.0.0.1:33671<br>
CoreDNS is running at https://127.0.0.1:33671/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy<br>
<br>
To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
</details>

### Install MetalLB to Enable Load Balancing Services

To enable load balancing services, follow these steps to install MetalLB:

```bash
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.7/config/manifests/metallb-native.yaml
kubectl wait --namespace metallb-system \
            --for=condition=ready pod \
            --selector=app=metallb \
            --timeout=120s

GATEWAY_IP=$(docker network inspect kind |  jq -r '.[].IPAM.Config[0].Gateway')
IFS=. read -a ArrIP<<<"${GATEWAY_IP}"

cat <<EOF | kubectl apply -f -
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: example
  namespace: metallb-system
spec:
  addresses:
  - ${ArrIP[0]}.${ArrIP[1]}.255.200-${ArrIP[0]}.${ArrIP[1]}.255.250
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: empty
  namespace: metallb-system
EOF
```

This installation process will enable MetalLB for load balancing services within your Kubernetes cluster.

With your Kubernetes cluster successfully set up, you are now prepared to deploy TigerGraph Operator.

## Step 2: Deploy TigerGraph Operator

To deploy TigerGraph Operator, carefully follow these steps:

### Install cert-manager for Kubernetes

TigerGraph Operator leverages the Admission Webhooks feature and relies on [cert-manager](https://github.com/jetstack/cert-manager) for provisioning certificates for the webhook server.

Admission webhooks are essential components in Kubernetes that receive admission requests and take actions based on them. They are registered with Kubernetes and are called upon to validate or modify a resource before it's stored.

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.13/cert-manager.yaml 
# Verify installation of cert-manager 
kubectl wait deployment -n cert-manager cert-manager --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-cainjector --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-webhook --for condition=Available=True --timeout=90s
```

### Install kubectl-tg plugin

The kubectl-tg plugin simplifies the deployment and management of both Operator and TigerGraph clusters through imperative commands.

Before installing the kubectl-tg plugin, make sure you meet the following requirements:

- [helm](https://helm.sh/docs/helm/helm_install/): version >= 3.7.0
- [jq](https://jqlang.github.io/jq/download/): version >= 1.6
- [yq](https://github.com/mikefarah/yq): version >= 4.18.1

Here's an example of installing the latest kubectl-tg, you can change the latest to your desired version, such as 0.0.9:

```bash
wget https://dl.tigergraph.com/k8s/latest/kubectl-tg -O kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

To verify the kubectl-tg version, use the following command:

```bash
kubectl tg version
```

Show help Information

```bash
kubectl tg help
```

### Install CRDs independently (Optional)

This step is optional and can be skipped if you have privileged permissions in your Kubernetes environment. The required components will be automatically installed during the Operator installation process.

CustomResourceDefinitions (CRDs) are non-namespaced entities accessible across all namespaces. Installing CRDs requires privileged permissions from the Kubernetes cluster. If you prefer to install CRDs independently from the Operator installation, use the following commands:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/latest/tg-operator-crd.yaml
```

### Install TigerGraph Operator

If you want to install the TigerGraph Operator with Helm, you can refer to [Deploy TigerGraph Operator with Helm](../03-deploy/deploy-operator-with-helm.md).

To streamline the installation of the Operator and the deployment of a TigerGraph cluster, start by defining some environment variables:

```bash
export YOUR_NAMESPACE="tigergraph"
export YOUR_CLUSTER_NAME="test-tg-cluster"
export YOUR_SSH_KEY_SECRET_NAME="ssh-key-secret"
```

Next, install the TigerGraph Operator using the following command:

```bash
kubectl tg init -n ${YOUR_NAMESPACE}
```

To ensure the successful deployment of the operator, use this command:

```bash
kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${YOUR_NAMESPACE}
```

For comprehensive guidance, refer to the output from `kubectl tg init --help`:

``` bash
kubectl tg init --help
Install the operator

Examples:
  # install the operator in the current namespace
  kubectl tg init
  # install the operator in the specified namespace
  kubectl tg init --namespace tg-tenant1
  # install the operator in the specified namespace, with specified helm repo and image pull secret
  kubectl tg init --namespace tg-tenant1 --helm-repo https://yourhelmrepo.com --image-pull-secret yoursecret
  # install the operator in the specified namespace, with specified operator version, watch name namespace, cpu and memory
  kubectl tg init --version OPERATOR_VERSION --operator-size 3 --operator-watch-namespace tigergraph --operator-cpu 1000m  --operator-memory 1024Mi --namespace tg-tenant1

Usage:
  kubectl tg init [options]

Options:
  -n, --namespace :            set namespace to deploy TG cluster, if not set, use the default namespace in context
  --helm-repo     :            set the specified helm repo to install operator, default as https://dl.tigergraph.com/charts
  --docker-registry :          set docker registry to download tigergraph image, default as docker.io
  --docker-image-repo :        set docker image repo for image name, default as tigergraph.
  -p, --image-pull-secret :    set imagePullSecret of docker registry, default as tigergraph-operator-image-pull-secret
  --image-pull-policy:         set pull policy of image, available policy: IfNotPresent, Always, and Never, default is IfNotPresent
  --operator-version:          set TG K8S operator version
  --operator-size :            set the replicas of operator's deployment for high availability, default is 3
  --operator-cpu :             set request cpu of operator, default as 1000m
  --operator-cpu-limit :       limit cpu size of operator
  --operator-memory :          set request memory of operator, default as 1024Mi
  --operator-memory-limit :    limit memory size of operator
  --operator-watch-namespace : set watch namespaces of operator, blank string as default indicate all namespace, multiple namespaces are separated by commas, as ns1\,ns2
  --operator-node-selector:    set the nodeSelector for operator pods, your input should be like 'k1=v1,k2=v2'
  --cluster-scope :            set true to deploy operator with ClusterRole, set false to deploy with Role,
                               so that you can deploy mutiple operators in one cluster, default as true
  --max-tg-concurrent-reconciles : set the max concurrent reconciles of TigerGraph cluster controller, default is 2
  --max-backup-concurrent-reconciles : set the max concurrent reconciles of TigerGraph backup controller, default is 2
  --max-backup-schedule-concurrent-reconciles : set the max concurrent reconciles of TigerGraph backup-schedule controller, default is 2
  --max-restore-concurrent-reconciles : set the max concurrent reconciles of TigerGraph restore controller, default is 2
```

## Step 3: Deploy a TigerGraph Cluster

This section provides instructions on deploying a TigerGraph cluster using the kubectl-tg plugin.

### Providing a Private SSH Key Pair for Enhanced Security

Starting from Operator version 0.0.4, users are required to provide their private SSH key pair for enhanced security before creating a cluster. Follow these steps:

- Create a Private SSH Key Pair File

  Generate a private SSH key pair file with the following command:

  ```bash
  echo -e 'y\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''
  ```

- Create a Secret Object

  Create a secret object based on the private SSH key file generated in Step 1. Ensure that the key name of the secret for the private SSH key is private-ssh-key, and the key name for the public SSH key is public-ssh-key. **Do not modify these key names**.

> [!IMPORTANT]
> The namespace of the Secret object must be the same as that of the TigerGraph cluster.

  ```bash
  kubectl create secret generic ${YOUR_SSH_KEY_SECRET_NAME} --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace ${YOUR_NAMESPACE}
  ```

> [!IMPORTANT]
> For Operator versions 0.0.4 and above, when creating a cluster using the `kubectl tg create command`, you must set the `--private-key-secret` option to `${YOUR_SSH_KEY_SECRET_NAME}`.

These steps enhance the security of your cluster by utilizing your private SSH key pair.

### Specify the StorageClass Name

Before creating the TigerGraph cluster with the Operator, it's necessary to specify the StorageClass, which defines various "classes" of storage available.

You can determine the name of the StorageClass using the following command:

```bash
kubectl get storageclass
NAME                 PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
standard (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  144m
```

Identify the StorageClass name, and when specifying the `--storage-class` option, use `standard` as its value. This ensures that the appropriate StorageClass is assigned during TigerGraph cluster creation, optimizing storage provisioning and management.

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

### Create a TigerGraph Cluster with Specific Options

You can obtain the TigerGraph Docker image versions from [tigergraph-k8s](https://hub.docker.com/r/tigergraph/tigergraph-k8s/tags)

You must also provide your license key with the `--license` command. Contact TigerGraph support for help finding your license key.

- Export license key as an environment variable

  ```bash
  export LICENSE=<LICENSE_KEY>
  ```

- Create TigerGraph cluster with kubectl-tg plugin

  ```bash
  kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --private-key-secret ${YOUR_SSH_KEY_SECRET_NAME} --size 3 --ha 2 --version 3.9.1 --license ${LICENSE} \
  --storage-class standard --storage-size 10G --cpu 2000m --memory 6Gi --namespace ${YOUR_NAMESPACE}
  ```

  To ensure the TigerGraph cluster has been successfully deployed, use the following commands:

  ```bash
  kubectl wait pods -l tigergraph.com/cluster-pod=${YOUR_CLUSTER_NAME} --for condition=Ready --timeout=15m --namespace ${YOUR_NAMESPACE}

  kubectl wait --for=condition=complete --timeout=10m  job/${YOUR_CLUSTER_NAME}-init-job --namespace ${YOUR_NAMESPACE}
  ```

## Step 4: Connect to a TigerGraph Cluster

This section explains how to log into a TigerGraph cluster pod and access the `RESTPP`,`GUI`, and `Metrics` services.

### Connect to a TigerGraph Cluster Pod

To log into a single container within the TigerGraph cluster and execute commands like `gadmin status`, use the following command:

```bash
kubectl tg connect --cluster-name ${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}
```

### Access TigerGraph Services

Query the external service address:

  ```bash
  export EXTERNAL_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-nginx-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].ip}')
  ```

#### Verify the API service

  ```bash
  curl http://${EXTERNAL_SERVICE_ADDRESS}:14240/api/ping

  {"error":false,"message":"pong","results":null}
  ```

To access the TigerGraph Suite, open it in your browser using the following URL: http://${EXTERNAL_SERVICE_ADDRESS}:14240, replacing `EXTERNAL_SERVICE_ADDRESS` with the actual service address.

#### Verify the RESTPP API service

  ```bash
  curl http://${EXTERNAL_SERVICE_ADDRESS}:14240/restpp/echo

  {"error":false, "message":"Hello GSQL"}
  ```

#### Verify the Metrics API service

  ```bash
curl http://${EXTERNAL_SERVICE_ADDRESS}/informant/metrics/get/network -d '{"LatestNum":"1"}'

{"NetworkMetrics":[{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"1ebeaf2a380f4941b371efaaceb3467b","TimestampNS":"1703666521019463773","Source":{"ServiceName":"EXE","Partition":2}},"HostID":"m2","CollectTimeStamps":"1703666521008230613","Network":{"IP":"10.244.0.79","TCPConnectionNum":89,"IncomingBytesNum":"1654215","OutgoingBytesNum":"1466486"}},{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"2c54ed5d6ba14e789db03fd9e023219c","TimestampNS":"1703666521020024563","Source":{"ServiceName":"EXE","Partition":3}},"HostID":"m3","CollectTimeStamps":"1703666521011409133","Network":{"IP":"10.244.0.78","TCPConnectionNum":90,"IncomingBytesNum":"1637413","OutgoingBytesNum":"1726712"}},{"EventMeta":{"Targets":[{"ServiceName":"IFM"}],"EventId":"c3478943ca134530bcd3aa439521c626","TimestampNS":"1703666521019483903","Source":{"ServiceName":"EXE","Partition":1}},"HostID":"m1","CollectTimeStamps":"1703666521009116924","Network":{"IP":"10.244.0.77","TCPConnectionNum":107,"IncomingBytesNum":"1298257","OutgoingBytesNum":"1197920"}}]}
  ```

## Step 5: Operate a TigerGraph Cluster

### Update the Resources (CPU and Memory) of the TigerGraph Cluster

Use the following command to update the CPU and memory resources of the TigerGraph cluster:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --cpu 3 --memory 8Gi  --cpu-limit 3--memory-limit 8Gi  --namespace ${YOUR_NAMESPACE}
```

### Update system configurations and license of the TigerGraph cluster

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

### Scale a TigerGraph Cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during scaling operations. Currently, TigerGraph does not provide dedicated high-availability scale support, and some downtime is involved.

Before scaling out the cluster, ensure you scale out the corresponding node pool to provide enough resources for the new instances.

Use the following command to scale the TigerGraph cluster:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --size 4 --ha 2  --namespace ${YOUR_NAMESPACE}
```

The above command scales the cluster to a size of 4 with an HA factor of 2.

#### Change the HA factor of the TigerGraph cluster

From Operator version 1.0.0, you can change the HA factor of the TigerGraph cluster without updating size by using the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --ha ${NEW_HA} --namespace ${YOUR_NAMESPACE}
```

### Upgrade a TigerGraph Cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during upgrading operations. Currently, TigerGraph does not provide dedicated high-availability upgrade support, and some downtime is involved.

Upgrading a TigerGraph cluster is supported from a lower version to a higher version.

Assuming the current version of the cluster is 3.9.1, you can upgrade it to version 3.9.2 with the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --version 3.9.2  --namespace ${YOUR_NAMESPACE}
```

To ensure the successful upgrade of the TigerGraph cluster, use these commands:

```bash
kubectl rollout status --watch --timeout=900s statefulset/${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=15m  job/${YOUR_CLUSTER_NAME}-upgrade-job --namespace ${YOUR_NAMESPACE}
```

## Step 6: Destroy the TigerGraph Cluster and the Kubernetes Operator

### Destroy the TigerGraph Cluster

To prevent accidental deletion of a cluster, deleting a cluster will not remove the Persistent Volume Claims (PVCs) and Persistent Volumes (PVs) associated with the cluster. If you intend to delete these components as well, you must manually delete the PVCs.

- Delete the TigerGraph cluster and keep the Persistent Volumes (PVs):

  ```bash
  kubectl tg delete --cluster-name ${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  ```

- Delete PVCs of the specific cluster:

  ```bash
  # to figure out the pvcs you want to delete by specific labels of pvc.
  kubectl get pvc -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}

  # delete the pvcs related to the specified cluster
  kubectl delete pvc -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  ```

### Uninstall TigerGraph Operator

Use the provided command below to uninstall the TigerGraph Kubernetes Operator within a specified namespace:

```bash
kubectl tg uninstall -n ${YOUR_NAMESPACE}
```

### Uninstall CRD

> [!NOTE]
> Replace the variable `${OPERATOR_VERSION}` to the Operator version you installed.

```bash
kubectl delete -f https://dl.tigergraph.com/k8s/${OPERATOR_VERSION}/tg-operator-crd.yaml
```

## Step 7: Destroy the Kubernetes Cluster

If you created the Kubernetes cluster using kind, use the following command to delete it:

```bash
kind delete cluster
```

## See also

If you are interested in deploying a TigerGraph cluster in a production environment, refer to the following documents:

- [Deploy TigerGraph on AWS EKS](../03-deploy/tigergraph-on-eks.md)
- [Deploy TigerGraph on Google Cloud GKE](../03-deploy/tigergraph-on-gke.md)
- [Deploy TigerGraph on Red Hat OpenShift](../03-deploy/tigergraph-on-openshift.md)
- [Deploy TigerGraph on Azure Kubernetes Service (AKS)](../03-deploy/tigergraph-on-aks.md)
