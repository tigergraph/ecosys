# Deploying TigerGraph on Red Hat OpenShift

This document provides detailed instructions for deploying a TigerGraph cluster on the Red Hat OpenShift platform.

- [Deploying TigerGraph on Red Hat OpenShift](#deploying-tigergraph-on-red-hat-openshift)
  - [Prerequisites](#prerequisites)
  - [Deploying TigerGraph Operator](#deploying-tigergraph-operator)
    - [Install cert-manager for OpenShift](#install-cert-manager-for-openshift)
    - [Install kubectl-tg plugin](#install-kubectl-tg-plugin)
    - [Install CRDs independently (Optional)](#install-crds-independently-optional)
    - [Install TigerGraph Operator](#install-tigergraph-operator)
  - [Deploy a TigerGraph Cluster](#deploy-a-tigergraph-cluster)
    - [Change the podPidsLimit value of OpenShift](#change-the-podpidslimit-value-of-openshift)
    - [Acquire special permission](#acquire-special-permission)
    - [Providing a Private SSH Key Pair for Enhanced Security](#providing-a-private-ssh-key-pair-for-enhanced-security)
    - [Specify the StorageClass name](#specify-the-storageclass-name)
    - [Create TG cluster with specific options](#create-tg-cluster-with-specific-options)
  - [Connect to a TigerGraph cluster](#connect-to-a-tigergraph-cluster)
    - [Connecting to a TigerGraph cluster Pod](#connecting-to-a-tigergraph-cluster-pod)
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

Before you begin, ensure you have the following prerequisites:

- [Helm](https://helm.sh/docs/intro/install/): version >= 3.7.0 TigerGraph Kubernetes Operator is packaged as a Helm chart, so you need Helm installed.

- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): version >= 1.23 `kubectl-tg` plugin requires kubectl for running commands against Kubernetes clusters.

- [OpenShift CLI](https://docs.openshift.com/container-platform/4.8/cli_reference/openshift_cli/getting-started-cli.html): Install the OpenShift CLI to acquire permissions within OpenShift.

- Create an [OpenShift Kubernetes cluster](https://docs.openshift.com/container-platform/4.10/installing/index.html) with admin role permission. OpenShift Container Platform version requirements are 4 and above.

## Deploying TigerGraph Operator

To deploy the TigerGraph Operator, follow these steps:

### Install cert-manager for OpenShift

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

### Install kubectl-tg plugin

kubectl-tg is a plugin for deploying and managing the Operator and TigerGraph clusters imperatively. Ensure you meet the following requirements before installing the kubectl-tg plugin:

- [helm](https://helm.sh/docs/helm/helm_install/): version >= 3.7.0
- [jq](https://jqlang.github.io/jq/download/): version >= 1.6
- [yq](https://github.com/mikefarah/yq): version >= 4.18.1

Here's an example of installing the latest kubectl-tg, you can change the latest to your desired version, such as 0.0.9:

```bash
wget https://dl.tigergraph.com/k8s/latest/kubectl-tg -O kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

Display kubectl-tg version information:

```bash
kubectl tg version
```

Show help Information

```bash
kubectl tg help
```

### Install CRDs independently (Optional)

This step is optional. You can skip it if you have privileged permissions in your Kubernetes environment. The required component will be automatically installed during the Operator installation process.

CustomResourceDefinitions (CRDs) are non-namespaced entities accessible across all namespaces. Installing CRDs requires privileged permissions from the Kubernetes cluster. You may prefer to install CRDs independently from the Operator installation:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/latest/tg-operator-crd.yaml
```

### Install TigerGraph Operator

To simplify the Operator installation and TigerGraph cluster deployment, define environment variables:

```bash
export YOUR_NAMESPACE="tigergraph"
export YOUR_CLUSTER_NAME="test-tg-cluster"
export YOUR_SSH_KEY_SECRET_NAME="ssh-key-secret"
export SERVICE_ACCOUNT_NAME="tg-service-account"
```

Install TigerGraph Operator using the following command:

A namespace-scoped operator watches and manages resources in a single Namespace, whereas a cluster-scoped operator watches and manages resources cluster-wide.

- Install a namespace-scoped Operator

    ```bash
    kubectl tg init --cluster-scope false --namespace ${YOUR_NAMESPACE}
    ```

- Install a cluster-scoped Operator (default behavior if not specified):

    ```bash
    kubectl tg init --cluster-scope true --namespace ${YOUR_NAMESPACE}
    ```

- Install the operator with specific options:

  Customize options such as operator version, deployment size, CPU, memory, and the namespace to watch:

    ```bash
    kubectl tg init --cluster-scope false --version ${OPERATOR_VERSION} --operator-size 3 --operator-watch-namespace ${YOUR_NAMESPACE} --operator-cpu 1000m  --operator-memory 1024Mi --namespace ${YOUR_NAMESPACE}
    ```

  For the full help manual, see the output of `kubectl tg init` --help.

- Verify the operator's successful deployment:

    ```bash
    kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${YOUR_NAMESPACE}
    ```

## Deploy a TigerGraph Cluster

This section explains how to deploy a TigerGraph cluster on OpenShift using the kubectl-tg plugin and CR (Custom Resource) YAML manifest.

### Change the podPidsLimit value of OpenShift

In a production environment, TigerGraph clusters require setting the podPidsLimit to 1 million. If your OpenShift cluster hasn't set this, use the following commands:

```bash
kubectl label machineconfigpool worker custom-crio=high-pid-limit
kubectl label machineconfigpool worker custom-kubelet=small-pods

eval "cat << EOF
apiVersion: machineconfiguration.openshift.io/v1
kind: KubeletConfig
metadata:
  name: worker-kubeconfig-fix
spec:
  machineConfigPoolSelector:
    matchLabels:
      custom-kubelet: small-pods 
  kubeletConfig:
      podPidsLimit: 1024000
EOF" | kubectl apply -f -

eval "cat << EOF
apiVersion: machineconfiguration.openshift.io/v1
kind: ContainerRuntimeConfig
metadata:
 name: set-pids-limit
spec:
 machineConfigPoolSelector:
   matchLabels:
     custom-crio: high-pid-limit
 containerRuntimeConfig:
   pidsLimit: 1024000 
EOF" | kubectl apply -f -
```

- [Verify the value of podPidsLimit](https://access.redhat.com/solutions/5366631)

1. Monitor /sys/fs/cgroup/pids/pids.current when the application is running to verify java.lang.OutOfMemoryError: unable to create new native thread or similar errors happen when it hits 1024 (or 4096 in OCP 4.11+).
2. For OCP 4.10 and previous releases, check if the CRI-O pids_limit is being set on the node where the application container is running:

    ```bash
    $ crio config | grep pids_limit
    INFO[2022-01-31 12:14:27.407346183Z] Starting CRI-O, version: 1.21.4-4.rhaos4.8.git84fa55d.el8, git: () 
    INFO Using default capabilities: CAP_CHOWN, CAP_DAC_OVERRIDE, CAP_FSETID, CAP_FOWNER, CAP_SETGID, CAP_SETUID, CAP_SETPCAP, CAP_NET_BIND_SERVICE, CAP_KILL 
    pids_limit = 4096
    ```

3. Verify the kubelet podPidsLimit is being set in /etc/kubernetes/kubelet.conf and SupportPodPidsLimit (only in 4.10 and older) is set running the following command:

    ```bash
    $ oc debug node/[node_name] -- cat /host/etc/kubernetes/kubelet.conf | jq '.podPidsLimit, .featureGates'
    Starting pod/[node_name]-debug ...
    To use host binaries, run `chroot /host`
    Removing debug pod ...

    2048
    {
    "LegacyNodeRoleBehavior": false,
    "NodeDisruptionExclusion": true,
    "RotateKubeletServerCertificate": true,
    "SCTPSupport": true,
    "ServiceNodeExclusion": true,
    "SupportPodPidsLimit": true
    }
    ```

    In newer releases, it's not a json file, so use the following command instead:

    ```bash
    $ oc debug node/[node_name] -- cat /host/etc/kubernetes/kubelet.conf | grep podPidsLimit
    podPidsLimit: 4096
    ```

    If not configured, the default (1024) applies (or 4096 in OCP 4.11+).

4. Verify the labels for the ContainerRuntimeConfig (only in OCP 4.10 and previous release) and KubeletConfig were created and applied:

    ```bash
    $ oc get kubeletconfig,containerruntimeconfig
    NAME                                 AGE
    kubeletconfig/worker-kubeconfig-fix  9d

    NAME                                   AGE
    containerruntimeconfig/set-pids-limit  15d

    $ oc get mcp/worker -o json | jq '.metadata.labels'
    {
    "custom-crio": "high-pid-limit",
    "custom-kubelet": "small-pods",
    "machineconfiguration.openshift.io/mco-built-in": "",
    "pools.operator.machineconfiguration.openshift.io/worker": ""
    }

    $ oc get kubeletconfig/worker-kubeconfig-fix -o json | jq '.status.conditions[]'
    {
    "lastTransitionTime": "2022-02-10T04:46:17Z",
    "message": "Success",
    "status": "True",
    "type": "Success"
    }
    ```

### Acquire special permission

Starting from TigerGraph version 3.9.0 and Operator version 0.0.4, significant security enhancements have been introduced in the TigerGraph Kubernetes (k8s) Docker image. These enhancements are designed to reinforce the overall security posture of the TigerGraph deployment. Specifically, two notable changes have been made:

1. **SUDO Permission Removal**: The SUDO permission has been removed from the TigerGraph image. This change aligns with best practices in security by reducing unnecessary privileges within the containerized environment.

2. **Static Private SSH Key Removal**: Static private SSH key files have been eliminated from the TigerGraph image. This removal further enhances the security of your TigerGraph deployment by reducing potential vulnerabilities associated with static keys.

To ensure a seamless deployment of the TigerGraph cluster with these enhanced security measures, it is essential to perform an additional operation within the OpenShift environment. Failure to complete this step may lead to issues where the Operator successfully creates the StatefulSet, but the Pod for TigerGraph fails to generate due to insufficient permissions. Consequently, querying the status of the StatefulSet will yield the following warning events:

```bash
Normal   SuccessfulCreate  119s                 statefulset-controller  create Claim tg-data-test-cluster-0 Pod test-cluster-0 in StatefulSet test-cluster success
Warning  FailedCreate      37s (x15 over 119s)  statefulset-controller  create Pod test-cluster-0 in StatefulSet test-cluster failed error: 
pods "test-cluster-0" is forbidden: unable to validate against any security context constraint: [provider "anyuid": Forbidden: not usable by user or serviceaccount, 
provider restricted: .spec.securityContext.fsGroup: Invalid value: []int64{1000}: 1000 is not an allowed group, 
spec.containers[0].securityContext.runAsUser: Invalid value: 1000: must be in the ranges: [1000680000, 1000689999], 
provider "nonroot": Forbidden: not usable by user or serviceaccount, 
provider "hostmount-anyuid": Forbidden: not usable by user or serviceaccount, 
provider "machine-api-termination-handler": Forbidden: not usable by user or serviceaccount, 
provider "hostnetwork": Forbidden: not usable by user or serviceaccount, 
provider "hostaccess": Forbidden: not usable by user or serviceaccount, 
provider "node-exporter": Forbidden: not usable by user or serviceaccount, 
provider "privileged": Forbidden: not usable by user or serviceaccount]
```

- Create a SecurityContextConstraints
  
  Execute the following command:

  ```bash
  cat <<EOF | kubectl apply -f -
  kind: SecurityContextConstraints
  apiVersion: security.openshift.io/v1
  allowHostDirVolumePlugin: false
  allowHostIPC: false
  allowHostNetwork: false
  allowHostPID: false
  allowHostPorts: false
  allowPrivilegedContainer: true
  metadata:
    name: anyuid-extra
    annotations:
      kubernetes.io/description: anyuid-extra provides all features of the anyuid SCC
          but add SYS_CHROOT and AUDIT_WRITE capabilities.
  priority: 10
  readOnlyRootFilesystem: false
  requiredDropCapabilities:
  - MKNOD
  runAsUser:
    type: RunAsAny
  seLinuxContext:
    type: MustRunAs
  fsGroup:
    type: RunAsAny
  supplementalGroups:
    type: RunAsAny
  allowedCapabilities:
  - AUDIT_WRITE
  - SYS_ADMIN
  - SYS_PTRACE
  - SYSLOG
  groups:
  - system:cluster-admins
  volumes:
  - configMap
  - downwardAPI
  - emptyDir
  - persistentVolumeClaim
  - projected
  - secret
  EOF
  ```

- Create or use an exist service account

  Create a service account as follows:

  ```bash
  kubectl create serviceaccount ${SERVICE_ACCOUNT_NAME} -n ${YOUR_NAMESPACE}
  ```

- Add the service account name `${SERVICE_ACCOUNT_NAME}` to the above SecurityContextConstraints

  Ensure that the service account name ${SERVICE_ACCOUNT_NAME} is added to the previously created SecurityContextConstraints:

  ```bash
  oc adm policy add-scc-to-user -n ${YOUR_NAMESPACE} -z ${SERVICE_ACCOUNT_NAME} anyuid-extra
  ```

With these steps completed, you can now use the service account name `${SERVICE_ACCOUNT_NAME}` to create the TigerGraph cluster on OpenShift.

> [!NOTE]
> If you are using the kubectl-tg plugin to create the TigerGraph cluster, you can specify the service account name using the `--service-account-name` option.
> [!WARNING]
> If you choose to install a cluster-scoped Operator, it is essential to create the aforementioned service account for each namespace in which you intend to deploy the TigerGraph cluster.

### Providing a Private SSH Key Pair for Enhanced Security

Starting from Operator version 0.0.4, users are required to provide their private SSH key pair for enhanced security before creating a cluster. Follow these steps:

- Step 1: create a private SSH key pair file:

  ```bash
  echo -e 'y\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''
  ```

- Step 2: Create a Secret Object

  > [!IMPORTANT]
  > The namespace of the Secret object must be the same as that of the TigerGraph cluster.

  Create a secret object based on the private SSH key file generated in step 1. Ensure that the key name of the secret for the private SSH key is `private-ssh-key`, and the key name for the public SSH key is `public-ssh-key`. Do not alter these key names:

  ```bash
  kubectl create secret generic ${YOUR_SSH_KEY_SECRET_NAME} --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace ${YOUR_NAMESPACE}
  ```

  For Operator versions 0.0.4 and above, when creating a cluster using the `kubectl tg create command`, you must set the `--private-key-secret` option to `${YOUR_SSH_KEY_SECRET_NAME}`.

These steps enhance the security of your cluster by utilizing your private SSH key pair.

### Specify the StorageClass name

Before creating the TigerGraph cluster with the Operator, specify the StorageClass, which defines the various "classes" of storage available. Use the following command to identify the name of the StorageClass:

You can determine the name of the StorageClass using the following command:

```bash
kubectl get storageclass

NAME                 PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
standard (default)   kubernetes.io/gce-pd    Delete          WaitForFirstConsumer   true                   37m
standard-csi         pd.csi.storage.gke.io   Delete          WaitForFirstConsumer   true                   37m
```

With the StorageClass identified, you can proceed to create clusters using the create command. When specifying the --storage-class option, choose standard as its value.

This process ensures that the appropriate StorageClass is assigned to your TigerGraph cluster creation, optimizing storage provisioning and management.

### Create TG cluster with specific options

To create a new TigerGraph cluster with specific options, use either the kubectl-tg plugin or a CR YAML manifest. Below are examples using the kubectl-tg plugin:

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
        cpu: "4"
        memory: 8Gi
      requests:
        cpu: "4"
        memory: 8Gi
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

### Connecting to a TigerGraph cluster Pod

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
