<h1> Deploy TigerGraph on AWS EKS </h1>

This user manual provides detailed instructions on deploying a TigerGraph cluster on AWS EKS (Elastic Kubernetes Service).

- [Prerequisites](#prerequisites)
- [Deploy TigerGraph Operator](#deploy-tigergraph-operator)
  - [Install cert-manager for EKS](#install-cert-manager-for-eks)
  - [Install the kubectl-tg Plugin](#install-the-kubectl-tg-plugin)
  - [Optional: Install CRDs Independently](#optional-install-crds-independently)
  - [Install TigerGraph Operator](#install-tigergraph-operator)
- [Deploy a TigerGraph Cluster](#deploy-a-tigergraph-cluster)
  - [Providing a Private SSH Key Pair for Enhanced Security](#providing-a-private-ssh-key-pair-for-enhanced-security)
  - [Specify the StorageClass Name](#specify-the-storageclass-name)
  - [Create a TigerGraph Cluster with Specific Options](#create-a-tigergraph-cluster-with-specific-options)
- [Connect to a TigerGraph Cluster](#connect-to-a-tigergraph-cluster)
  - [Connect to a TigerGraph Cluster Pod](#connect-to-a-tigergraph-cluster-pod)
  - [Access TigerGraph Suite](#access-tigergraph-suite)
  - [Access RESTPP API Service](#access-restpp-api-service)
- [Upgrade a TigerGraph Cluster](#upgrade-a-tigergraph-cluster)
- [Scale a TigerGraph Cluster](#scale-a-tigergraph-cluster)
- [Update Resources (CPU and Memory) of the TigerGraph Cluster](#update-resources-cpu-and-memory-of-the-tigergraph-cluster)
- [Destroy the TigerGraph Cluster and the Kubernetes Operator](#destroy-the-tigergraph-cluster-and-the-kubernetes-operator)
  - [Destroy the TigerGraph Cluster](#destroy-the-tigergraph-cluster)
  - [Uninstall TigerGraph Operator](#uninstall-tigergraph-operator)
  - [Uninstall the Custom Resource Definitions (CRDs)](#uninstall-the-custom-resource-definitions-crds)
- [See also](#see-also)

## Prerequisites

Before proceeding with the deployment, make sure you have the following prerequisites in place:

- [Helm](https://helm.sh/docs/intro/install/) installed, with a version equal to or greater than 3.7.0. The TigerGraph Kubernetes Operator is packaged as a Helm chart and requires Helm for installation.

- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) installed, with a version equal to or greater than 1.23. The `kubectl-tg` plugin depends on `kubectl` for managing Kubernetes clusters.

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) installed with the latest version. This will be used to install the EBS CSI driver `aws-ebs-csi-driver` if necessary.

- An existing [EKS cluster](https://docs.aws.amazon.com/eks/latest/userguide/create-cluster.html) with admin role permissions.

## Deploy TigerGraph Operator

To deploy the TigerGraph Operator, follow these steps:

### Install cert-manager for EKS

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

### Install the kubectl-tg Plugin

The `kubectl-tg` plugin allows you to deploy and manage the Operator and TigerGraph clusters imperatively. Before installing the plugin, ensure the following requirements are met:

- [helm](https://helm.sh/docs/helm/helm_install/) with a version equal to or greater than 3.7.0.
- [jq](https://jqlang.github.io/jq/download/) with a version equal to or greater than 1.6.
- [yq](https://github.com/mikefarah/yq) with a version equal to or greater than 4.18.1.

Here's an example of installing the latest kubectl-tg, you can change the latest to your desired version, such as 0.0.9:

```bash
wget https://dl.tigergraph.com/k8s/latest/kubectl-tg -O kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

To check the `kubectl-tg` version, use:

```bash
kubectl tg version
```

For help information, use:

```bash
kubectl tg help
```

### Optional: Install CRDs Independently

This step is optional and can be skipped if you have privileged permissions in your Kubernetes environment. The necessary CustomResourceDefinitions (CRDs) are automatically installed during the Operator installation. If you prefer to install CRDs independently, use the following command:

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

Now, you can install the TigerGraph Operator using the following commands:

A namespace-scoped operator watches and manages resources in a single Namespace, whereas a cluster-scoped operator watches and manages resources cluster-wide.

- For a namespace-scoped Operator:

    ```bash
    kubectl tg init --cluster-scope false --namespace ${YOUR_NAMESPACE}
    ```

- For a cluster-scoped Operator (default behavior):

    ```bash
    # This is a defulat behavior if you don't specific the --cluster-scope option.
    kubectl tg init --cluster-scope true --namespace ${YOUR_NAMESPACE}
    ```

- For custom installation options:

  You can customize the installation by specifying options like the Operator version, deployment size, CPU, memory, and the namespace to watch, among others. Here's an example:

    ```bash
    kubectl tg init --cluster-scope false --version ${OPERATOR_VERSION} --operator-size 3 --operator-watch-namespace ${YOUR_NAMESPACE} --operator-cpu 1000m  --operator-memory 1024Mi --namespace ${YOUR_NAMESPACE}
    ```

  For a comprehensive list of options, refer to the output of the `kubectl tg init` --help command.

To verify the successful deployment of the Operator, use the following command:

  ```bash
  kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${YOUR_NAMESPACE}
  ```

## Deploy a TigerGraph Cluster

This section explains how to deploy a TigerGraph cluster on EKS using the `kubectl-tg` plugin and a CR (Custom Resource) YAML manifest.

### Providing a Private SSH Key Pair for Enhanced Security

Starting from Operator version 0.0.4, users are required to provide their private SSH key pair for enhanced security before creating a cluster. Follow these steps:

- Step 1: create a private SSH key pair file:

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

### Specify the StorageClass Name

Before creating the TigerGraph cluster with the Operator, specify the StorageClass, which defines the various storage options. You can identify the name of the StorageClass with the following command:

> [!NOTE]
> Here the dynamic persistent volume storage is provided by EKS by default, if you want to use static persistent volume or use them from scratch, please refer to [How to use static & dynamic persistent volume storage](../07-reference/static-and-dynamic-persistent-volume-storage.md).

```bash
kubectl get storageclass

NAME            PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
gp2 (default)   kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   false                  62m
```

Choose the appropriate StorageClass (e.g., `gp2`) when creating the TigerGraph cluster, ensuring optimized storage provisioning and management.

> [!WARNING]
> For specific EKS version, you may encounter the following problems, TigerGraph pods will be in pending state because of the PVC pending state.

- TigerGraph Pod status

  ```bash
  kubectl get pod -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  
  NAME                READY   STATUS    RESTARTS   AGE
  test-tg-cluster-0   0/1     Pending   0          5m27s
  test-tg-cluster-1   0/1     Pending   0          5m27s
  test-tg-cluster-2   0/1     Pending   0          5m27s
  ```

- TigerGraph PVC status

  ```bash
  kubectl get pvc -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  
  NAME                        STATUS    VOLUME   CAPACITY   ACCESS MODES   STORAGECLASS   AGE
  tg-data-test-tg-cluster-0   Pending                                      gp2            110s
  tg-data-test-tg-cluster-1   Pending                                      gp2            110s
  tg-data-test-tg-cluster-2   Pending                                      gp2            110s
  ```

- Checking the PVC Events of one Pod

  ```bash
  kubectl describe pvc tg-data-test-tg-cluster-0 -n ${YOUR_NAMESPACE}
  
  Name:          tg-data-test-tg-cluster-0
  Namespace:     tigergraph
  StorageClass:  gp2
  Status:        Pending
  Volume:
  Labels:        tigergraph.com/cluster-name=test-tg-cluster
                tigergraph.com/cluster-pod=test-tg-cluster
  Annotations:   volume.beta.kubernetes.io/storage-provisioner: ebs.csi.aws.com
                volume.kubernetes.io/selected-node: ip-172-31-22-5.us-west-1.compute.internal
                volume.kubernetes.io/storage-provisioner: ebs.csi.aws.com
  Finalizers:    [kubernetes.io/pvc-protection]
  Capacity:
  Access Modes:
  VolumeMode:    Filesystem
  Used By:       test-tg-cluster-0
  Events:
    Type    Reason                Age                    From                         Message
    ----    ------                ----                   ----                         -------
    Normal  WaitForFirstConsumer  7m54s                  persistentvolume-controller  waiting for first consumer to be created before binding
    Normal  ExternalProvisioning  115s (x25 over 7m54s)  persistentvolume-controller  waiting for a volume to be created, either by external provisioner "ebs.csi.aws.com" or manually created by system administrator
  ```

  If you encounter the above issues, please resolve it using the following steps:

  1. Make sure that the EKS cluster has been installed EBS CSI driver

      ```bash
      kubectl get deployment ebs-csi-controller -n kube-system
      ```

  2. If not, install EBS CSI driver through the following commands

  > [!WARNING]
  > Please ensure that the IAM role for the Amazon EBS CSI driver has been created. You can refer to the official AWS documentation [Creating the Amazon EBS CSI driver IAM role](https://docs.aws.amazon.com/eks/latest/userguide/csi-iam-role.html) for detailed instructions.

  ```bash
      aws eks create-addon --cluster-name $YOUR_EKS_CLUSTER_NAME --addon-name aws-ebs-csi-driver
  ```

### Create a TigerGraph Cluster with Specific Options

You can create a new TigerGraph cluster with specific options, such as size, high availability, version, license, and resource specifications. Here's an example:

- Get and export free license

  ```bash
  export LICENSE=$(curl -L "ftp://ftp.graphtiger.com/lic/license3.txt" -o "/tmp/license3.txt" 2>/dev/null && cat /tmp/license3.txt)
  ```

- Create TigerGraph cluster with kubectl-tg plugin

  ```bash
  kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --private-key-secret ${YOUR_SSH_KEY_SECRET_NAME} --size 3 --ha 2 --version 3.9.3 --license ${LICENSE} \
  --storage-class gp2 --storage-size 100G --cpu 6000m --memory 16Gi --namespace ${YOUR_NAMESPACE}
  ```

- Create TigerGraph cluster with CR(Custom Resource) YAML manifest
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
        storageClassName: gp2
  EOF
  ```

To ensure the successful deployment of the TigerGraph cluster, use the following commands:

```bash
kubectl wait pods -l tigergraph.com/cluster-pod=${YOUR_CLUSTER_NAME} --for condition=Ready --timeout=15m --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=10m  job/${YOUR_CLUSTER_NAME}-init-job --namespace ${YOUR_NAMESPACE}
```

## Connect to a TigerGraph Cluster

This section outlines how to connect to a TigerGraph cluster pod and access the `RESTPP` and `GUI` services.

### Connect to a TigerGraph Cluster Pod

To connect to a single container within the TigerGraph cluster and execute commands like `gadmin status`, use the following command:

```bash
kubectl tg connect --cluster-name ${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}
```

### Access TigerGraph Suite

To access the TigerGraph Suite, perform the following steps:

- Query the external service address:

  ```bash
  export GUI_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-gui-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].hostname}')

  echo $GUI_SERVICE_ADDRESS
  af05164a18dc74453be7a62dfcadab8a-894761266.us-west-1.elb.amazonaws.com
  ```

- Verify the API service:

  ```bash
  curl http://${GUI_SERVICE_ADDRESS}:14240/api/ping

  {"error":false,"message":"pong","results":null}
  ```

Access the TigerGraph Suite in your browser using the following URL: http://${GUI_SERVICE_ADDRESS}:14240, replacing `${GUI_SERVICE_ADDRESS}` with the actual service address.

### Access RESTPP API Service

To access the RESTPP API service, follow these steps:

- Query the external service address:

  ```bash
  export RESTPP_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-rest-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].hostname}')

  echo $RESTPP_SERVICE_ADDRESS
  a9b7d5343abd94a1b85048d5373161c4-491617925.us-west-1.elb.amazonaws.com
  ```

- Verify the availability of the RESTPP API service:

  ```bash
  curl http://${RESTPP_SERVICE_ADDRESS}:9000/echo

  {"error":false, "message":"Hello GSQL"}
  ```

## Upgrade a TigerGraph Cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during upgrading operations. Currently, TigerGraph does not provide dedicated high-availability upgrade support, and some downtime is involved.

Upgrading a TigerGraph cluster is supported from a lower version to a higher version.

> [!WARNING]
> For TigerGraph 3.9.3 and later versions, the use of passwords to log in to Pods is disabled, which enhances security. If you plan to upgrade your TigerGraph cluster to version 3.9.3, it is essential to first upgrade the Operator to version 0.0.9.

> [!WARNING]
> Operator 0.0.9 has disabled TG downgrades from a higher version (e.g., 3.9.3) to any lower version (e.g., 3.9.2). Therefore, the upgrade job will fail if you attempt to downgrade.

You can upgrade a TigerGraph cluster from a lower version to a higher version. Assuming the current version is 3.9.2 and you want to upgrade to 3.9.3, use the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --version 3.9.3  --namespace ${YOUR_NAMESPACE}
```

If you prefer to upgrade the cluster using a CR (Custom Resource) YAML manifest, simply update the `spec.initTGConfig.version` and `spec.image` field, and then apply it.

Ensure the successful upgrade with these commands:

```bash
kubectl rollout status --watch --timeout=900s statefulset/${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=15m  job/${YOUR_CLUSTER_NAME}-upgrade-job --namespace ${YOUR_NAMESPACE}
```

## Scale a TigerGraph Cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during scaling operations. Currently, TigerGraph does not provide dedicated high-availability scale support, and some downtime is involved.

Before scaling out the cluster, ensure that the corresponding node pool is scaled out to provide sufficient resources for the new instances. Use the following command to scale the TigerGraph cluster:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --size 6 --ha 2  --namespace ${YOUR_NAMESPACE}
```

The above command scales the cluster to a size of 6 with a high availability factor of 2. If you prefer to use a CR (Custom Resource) YAML manifest for scaling, update the `spec.replicas` and `spec.initTGConfig.ha` fields accordingly.

## Update Resources (CPU and Memory) of the TigerGraph Cluster

To update the CPU and memory resources of the TigerGraph cluster, use the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --cpu 8 --memory 16Gi  --cpu-limit 8 --memory-limit 16Gi  --namespace ${YOUR_NAMESPACE}
```

Alternatively, if you want to update the cluster using a CR (Custom Resource) YAML manifest, update the spec.resources.requests and spec.resources.limits fields accordingly.

## Destroy the TigerGraph Cluster and the Kubernetes Operator

### Destroy the TigerGraph Cluster

To delete a TigerGraph cluster, use the following command. Note that this command does not remove Persistent Volume Claims (PVCs) and Persistent Volumes (PVs) associated with the cluster. To delete these components, manually delete the PVCs.

- Delete the TigerGraph cluster and remain the Persistent Volumes (PVs)

  ```bash
  kubectl tg delete --cluster-name ${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  ```

- To delete the PVCs related to the specified cluster, use the following commands:

  ```bash
  # to figure out the pvcs you want to delete by specific labels of pvc.
  kubectl get pvc -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}

  # delete the pvcs related to the specified cluster
  kubectl delete pvc -l tigergraph.com/cluster-name=${YOUR_CLUSTER_NAME} -n ${YOUR_NAMESPACE}
  ```

### Uninstall TigerGraph Operator

To uninstall the TigerGraph Kubernetes Operator within a specified namespace, use the following command:

```bash
kubectl tg uninstall -n ${YOUR_NAMESPACE}
```

### Uninstall the Custom Resource Definitions (CRDs)

> [!NOTE]
> Replace the variable `${OPERATOR_VERSION}` to the Operator version you installed.

```bash
kubectl delete -f https://dl.tigergraph.com/k8s/${OPERATOR_VERSION}/tg-operator-crd.yaml
```

## See also

If you are interested in the details of deploying a TigerGraph cluster using the CR (Custom Resource) YAML manifest, refer to the following document:

- [Configuring TigerGraph Clusters on K8s using TigerGraph CR](../07-reference/configure-tigergraph-cluster-cr-with-yaml-manifests.md)
