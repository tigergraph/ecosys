# Deploy TigerGraph on AWS EKS

This user manual provides detailed instructions on deploying a TigerGraph cluster on AWS EKS (Elastic Kubernetes Service).

- [Deploy TigerGraph on AWS EKS](#deploy-tigergraph-on-aws-eks)
  - [Prerequisites](#prerequisites)
  - [Deploy TigerGraph Operator](#deploy-tigergraph-operator)
    - [Install cert-manager for EKS](#install-cert-manager-for-eks)
    - [Install the kubectl-tg Plugin](#install-the-kubectl-tg-plugin)
    - [Optional: Install CRDs Independently](#optional-install-crds-independently)
    - [Install TigerGraph Operator](#install-tigergraph-operator)
  - [Deploy a TigerGraph Cluster](#deploy-a-tigergraph-cluster)
    - [Install EBS CSI driver](#install-ebs-csi-driver)
    - [Providing a Private SSH Key Pair for Enhanced Security](#providing-a-private-ssh-key-pair-for-enhanced-security)
    - [Specify the StorageClass Name](#specify-the-storageclass-name)
    - [Specify the additional Storage for mounting multiple PVs(Optional)](#specify-the-additional-storage-for-mounting-multiple-pvsoptional)
    - [Customize configurations for the TigerGraph system (Optional)](#customize-configurations-for-the-tigergraph-system-optional)
    - [Create a TigerGraph Cluster with Specific Options](#create-a-tigergraph-cluster-with-specific-options)
    - [Troubleshooting TigerGraph cluster deployment on EKS](#troubleshooting-tigergraph-cluster-deployment-on-eks)
      - [EBS CSI driver not installed](#ebs-csi-driver-not-installed)
      - [Node IAM role missing policy AmazonEBSCSIDriverPolicy](#node-iam-role-missing-policy-amazonebscsidriverpolicy)
  - [Connect to a TigerGraph Cluster](#connect-to-a-tigergraph-cluster)
    - [Connect to a TigerGraph Cluster Pod](#connect-to-a-tigergraph-cluster-pod)
    - [Access TigerGraph Services](#access-tigergraph-services)
      - [Verify the API service](#verify-the-api-service)
      - [Verify the RESTPP API service](#verify-the-restpp-api-service)
      - [Verify the Metrics API service](#verify-the-metrics-api-service)
  - [Upgrade a TigerGraph Cluster](#upgrade-a-tigergraph-cluster)
  - [Update system configurations and license of the TigerGraph cluster](#update-system-configurations-and-license-of-the-tigergraph-cluster)
  - [Scale a TigerGraph Cluster](#scale-a-tigergraph-cluster)
    - [Change the HA factor of the TigerGraph cluster](#change-the-ha-factor-of-the-tigergraph-cluster)
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

- An existing [EKS cluster](https://docs.aws.amazon.com/eks/latest/userguide/create-cluster.html) with appropriate IAM permissions:
  
  - The EKS cluster requires an IAM role with the following AWS-managed IAM policies attached:
    - arn:aws:iam::aws:policy/AmazonEKSClusterPolicy
    - arn:aws:iam::aws:policy/AmazonEKSServicePolicy
  - The EKS node group requires an IAM role with the following AWS-managed IAM policies attached:
    - arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy
    - arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy
    - arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
    - arn:aws:iam::aws:policy/AmazonEKSClusterPolicy
    - arn:aws:iam::aws:policy/AmazonEKSVPCResourceController

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

> [!IMPORTANT]
> Namespace-scoped operators require the same operator version to be installed for different namespaces.

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

> [!NOTE]
> You can set the concurrent reconciliation value to a larger value during installation or update it to a suitable value afterward. The default maximum concurrent reconciliation value of 2 is sufficient for most cases. However, you may need to customize it if you use one operator to manage numerous TigerGraph clusters within a Kubernetes cluster.

  For a comprehensive list of options, refer to the output of the `kubectl tg init` --help command.

- Ensure that the operator has been successfully deployed:

    ```bash
    kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n ${YOUR_NAMESPACE}
    ```

## Deploy a TigerGraph Cluster

This section explains how to deploy a TigerGraph cluster on EKS using the `kubectl-tg` plugin and a CR (Custom Resource) YAML manifest.

### Install EBS CSI driver

For specific EKS version, EBS CSI is not installed by default, please refer to the below official documents install the driver manually.

The Amazon Elastic Block Store (Amazon EBS) Container Storage Interface (CSI) driver manages the lifecycle of Amazon EBS volumes as storage for the Kubernetes Volumes that you create. [Official AWS Documentation](https://docs.aws.amazon.com/eks/latest/userguide/ebs-csi.html)

- Install EBS CSI driver

  ```bash
  aws eks create-addon --cluster-name ${YOUR_CLUSTER_NAME} --addon-name aws-ebs-csi-driver
  ```

- Wait for the Amazon EBS CSI Driver deployment (ebs-csi-controller) to become available in the EKS cluster
  
  ```bash
  kubectl wait --for=condition=Available=True deployment/ebs-csi-controller -n kube-system
  ```

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

You can create a new TigerGraph cluster with specific options, such as size, high availability, version, license, and resource specifications. Here's an example:

You must provide your license key when creating cluster. Contact TigerGraph support for help finding your license key.

- Export license key as an environment variable

  ```bash
  export LICENSE=<LICENSE_KEY>
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
        storageClassName: gp2
  EOF
  ```

To ensure the successful deployment of the TigerGraph cluster, use the following commands:

```bash
kubectl wait pods -l tigergraph.com/cluster-pod=${YOUR_CLUSTER_NAME} --for condition=Ready --timeout=15m --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=10m  job/${YOUR_CLUSTER_NAME}-init-job --namespace ${YOUR_NAMESPACE}
```

### Troubleshooting TigerGraph cluster deployment on EKS

#### EBS CSI driver not installed

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

If you're facing the issues above, please check the following:

- Verify if the EBS CSI driver is correctly installed as an EKS add-on. [EBS CSI driver Installation](#install-ebs-csi-driver)
- Confirm that the EKS cluster and EKS node group have the necessary permissions. [See prerequisites](#prerequisites)

#### Node IAM role missing policy AmazonEBSCSIDriverPolicy

> [!WARNING]
> If Node IAM role is missing policy AmazonEBSCSIDriverPolicy, you may encounter the following issues:

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

  $ kubectl describe pvc tg-data-test-cluster-0
  Name:          tg-data-test-cluster-0
  Namespace:     tigergraph
  StorageClass:  gp2
  Status:        Pending
  Volume:
  Labels:        tigergraph.com/cluster-name=test-cluster
                tigergraph.com/cluster-pod=test-cluster
  Annotations:   volume.beta.kubernetes.io/storage-provisioner: ebs.csi.aws.com
                volume.kubernetes.io/selected-node: ip-10-0-181-24.us-east-2.compute.internal
                volume.kubernetes.io/storage-provisioner: ebs.csi.aws.com
  Finalizers:    [kubernetes.io/pvc-protection]
  Capacity:
  Access Modes:
  VolumeMode:    Filesystem
  Used By:       test-cluster-0
  Events:
    Type     Reason                Age    From                                                                                      Message
    ----     ------                ----   ----                                                                                      -------
    Normal   WaitForFirstConsumer  6m52s  persistentvolume-controller                                                               waiting for first consumer to be created before binding
    Warning  ProvisioningFailed    6m12s  ebs.csi.aws.com_ebs-csi-controller-86b5857975-f7j7b_1cf8c563-3cd2-42bd-ad71-47d411105041  failed to provision volume with StorageClass "gp2": rpc error: code = Internal desc = Could not create volume "pvc-9d232f25-9360-4335-bad4-f1a091e8ccf0": could not create volume in EC2: UnauthorizedOperation: You are not authorized to perform this operation. User: arn:aws:sts::441097962705:assumed-role/eksNodeLimitedRole/i-0c69801855e08d732 is not authorized to perform: ec2:CreateVolume on resource: arn:aws:ec2:us-east-2:441097962705:volume/* because no identity-based policy allows the ec2:CreateVolume action. Encoded authorization failure message: 0KuL0Fl2KA-d3PhJPmCXVs-XvZ-pfJbLVWrtU9P0OORbH-X2i0L9mJHocI9HI1fmUmnlrvTS9TK7tC3bLtxobfFp1HGSnDe7km-TejKGvtXQeyGY8unpyKcZe-BqgKoZ9k5J9hm0ZOPSMZDi3HTfnsrCvjIXl9B8gFFrrjIjrUTAeOobUhdEN0jDgDziwrZXZRFhFzy_EVStH8rn0u1QVPpTFGkfbc9ZdltO0sUzvY_Jaombb1TQ3z7t0mf3G4CUOE2Qwb77-oD1sTmNRrH0F-ZdAeH_RIuK01GNntsv9CbXXN0aHukmY50bv1pPOUJ7oh52LgODUgTfQ2ZZhmi4YqcvfBOnOGBFl1cSXAgLka52-nwzczAVeLR72Q1hVxkqsEfhH2dMJzopWeKE_tqGMOw4XuwZvY87TO5-yVzqJCwkJsOR3XebXIYOlZhhErkuVeLdN5tfy5eFs1WBH50bOvLVBYl7YGSWlz3r6q655SMrNnAugb4zHWOYne7SsZfHBqfft023g_j80S5gaaZJkjAErPGh3TY7W5Efu6aiQ2EOr0KJ3m6veACR8R6ShGxptLaTfWKbsXCExnfvZHAJlCE5AGMelm1ELxYH_gM5ZP3VcEin
            status code: 403, request id: 2e7db24a-dafd-4e82-ade8-f14e33f16626
    ...
    Warning  ProvisioningFailed  4m5s  ebs.csi.aws.com_ebs-csi-controller-86b5857975-f7j7b_1cf8c563-3cd2-42bd-ad71-47d411105041  failed to provision volume with StorageClass "gp2": rpc error: code = Internal desc = Could not create volume "pvc-9d232f25-9360-4335-bad4-f1a091e8ccf0": could not create volume in EC2: UnauthorizedOperation: You are not authorized to perform this operation. User: arn:aws:sts::441097962705:assumed-role/eksNodeLimitedRole/i-0c69801855e08d732 is not authorized to perform: ec2:CreateVolume on resource: arn:aws:ec2:us-east-2:441097962705:volume/* because no identity-based policy allows the ec2:CreateVolume action. Encoded authorization failure message: zLRp-92JZXZ6JRpECwhOZcQeb7MATfO4mWl05nCmO5JDwyCHzpNwoV2WOKBIlpwvNub2-guuLcrIQ3ZHrdScNTpPytr52ijuEN3Rvm88LCNlodW2ubYjQul4CJvPp1bTvqzoBpkxoRK2VRbFW1CYlLobrZ-wRl33agGax9i0W5oSGzuNlbj7UZ-90heHpNRcZoUQJ3JOVRXzxsaaABZOeHerO3eAo94SuICDc7MdmoQALU6ax3IsluRsi15URDeIqfgtvwBTrlMmi5SbIg3W_dl67hHoOlm4sJUyuvcfPFWyMId4kffv0j3P3GLFbYL-HRfXUqi92Nb5-xwHta4zYm7QxgLQjeY1tfEezqx7uNSvpui9Yqi9ZQ3LGOe32X6CUBU5HeI8EjQvzya7bGuSEKr5RM2z1fCxeQ9EWdVep-kWXbhb5S4H84KIAthElopYq1UzBbQwyrGxMi_l4SfI4bCJ7daFOX4mssFCw-D5WR0avswSOffW0lX2vl0ZAVcEWFMKYcVHXywqWuYWAEvmyvBXIXwd0sRUnfkROAmA3yWbLELObLgMN-n_m1l9kfnXotP6mnZADJhygW9huMnqNA0Z7KREknw-9SVWNIuoG8ofDDqA
            status code: 403, request id: d1fc4bd9-9e18-4bb2-a143-d22f2a442003
    Normal   Provisioning        117s (x9 over 6m13s)  ebs.csi.aws.com_ebs-csi-controller-86b5857975-f7j7b_1cf8c563-3cd2-42bd-ad71-47d411105041  External provisioner is provisioning volume for claim "tigergraph/tg-data-test-cluster-0"
    Warning  ProvisioningFailed  117s                  ebs.csi.aws.com_ebs-csi-controller-86b5857975-f7j7b_1cf8c563-3cd2-42bd-ad71-47d411105041  failed to provision volume with StorageClass "gp2": rpc error: code = Internal desc = Could not create volume "pvc-9d232f25-9360-4335-bad4-f1a091e8ccf0": could not create volume in EC2: UnauthorizedOperation: You are not authorized to perform this operation. User: arn:aws:sts::441097962705:assumed-role/eksNodeLimitedRole/i-0c69801855e08d732 is not authorized to perform: ec2:CreateVolume on resource: arn:aws:ec2:us-east-2:441097962705:volume/* because no identity-based policy allows the ec2:CreateVolume action. Encoded authorization failure message: eusQ92uMXEtArfGIRp0xHNr89QmUtyOimvECDSppNxKUZ_Cd0cAZhbeBokCkzXY5MJPZRAS9q8K1F1mHahhdlbXrwB2V_d4l0UmdMH-70TQJo_L7XKtTe4vi-Z7fKX94StTcwmuLsvJx8G2fBrykYo_T70D8yfqGnLXtLa8Q2wxtMwma5EWogONArcAe8r0sksforrbM2tcznHVKFM3QDVnvgJ5ZtMIMNPssJInfEP7FOxEKagFJn6s3X7MvdPI06PTCy9opbmSltZX9BWyGRZTWcfOddqQKOqeasQCSsWYTZyYEBU8iYh1vgm43_hRRI0ConbNPbh3O6pplYItQODY8DkOVzcRvLnr4BcaDnYevIYRp_xDSHtSeMisPa5U-1ambf3Jktv0-_BUFMW7VavWMHuEFQ5WlbwIyQey1nmMR3ZQL9v1Dxpe8VV9z5_8b-oOBHz0al3GU_4epEiioSmiVIig9F6hdHBk9gQwiVwFbPkaIVEKMzDDJN2XmmGDMzWSMMGAYbXg2G0yNdL99Y3xaohIg-tsnf6NvNeE-aZkqovUwfo-Y91M63bgExupvmF9KlpSqYbO4Iok0MgdBoACoHpZ-gLZM-FRYQNeo7RtbrsnF
            status code: 403, request id: 1301427a-adec-4e11-badd-f0ba02b4a375
    Normal   ExternalProvisioning  61s (x25 over 6m52s)  persistentvolume-controller  Waiting for a volume to be created either by the external provisioner 'ebs.csi.aws.com' or manually by the system administrator. If volume creation is delayed, please verify that the provisioner is running and correctly registered.
  ```

If you're facing the issues above, please check the following:

- Make sure that EKS node group IAM role has the policy AmazonEBSCSIDriverPolicy.
- Confirm that the EKS cluster and EKS node group have the necessary permissions. [See prerequisites](#prerequisites)

## Connect to a TigerGraph Cluster

This section outlines how to connect to a TigerGraph cluster pod and access the `RESTPP` and `GUI` services.

### Connect to a TigerGraph Cluster Pod

To connect to a single container within the TigerGraph cluster and execute commands like `gadmin status`, use the following command:

```bash
kubectl tg connect --cluster-name ${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}
```

### Access TigerGraph Services

Query the external service address:

  ```bash
  export EXTERNAL_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-nginx-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].hostname}')
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

If you prefer to upgrade the cluster using a CR (Custom Resource) YAML manifest, simply update the `spec.image` field, and then apply it.

Ensure the successful upgrade with these commands:

```bash
kubectl rollout status --watch --timeout=900s statefulset/${YOUR_CLUSTER_NAME} --namespace ${YOUR_NAMESPACE}

kubectl wait --for=condition=complete --timeout=15m  job/${YOUR_CLUSTER_NAME}-upgrade-job --namespace ${YOUR_NAMESPACE}
```

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

## Scale a TigerGraph Cluster

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during scaling operations. Currently, TigerGraph does not provide dedicated high-availability scale support, and some downtime is involved.

Before scaling out the cluster, ensure that the corresponding node pool is scaled out to provide sufficient resources for the new instances. Use the following command to scale the TigerGraph cluster:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --size 6 --ha 2  --namespace ${YOUR_NAMESPACE}
```

The above command scales the cluster to a size of 6 with a high availability factor of 2. If you prefer to use a CR (Custom Resource) YAML manifest for scaling, update the `spec.replicas` and `spec.ha` fields accordingly.

### Change the HA factor of the TigerGraph cluster

From Operator version 1.0.0, you can change the HA factor of the TigerGraph cluster without updating size by using the following command:

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --ha ${NEW_HA} --namespace ${YOUR_NAMESPACE}
```

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
