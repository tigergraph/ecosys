# How to upgrade TigerGraph Kubernetes Operator

This document provides step-by-step instructions for upgrading the TigerGraph Kubernetes Operator using the kubectl-tg plugin.

- [How to upgrade TigerGraph Kubernetes Operator](#how-to-upgrade-tigergraph-kubernetes-operator)
  - [Upgrading from TigerGraph Operator 1.0.0 and later versions to version 1.3.0](#upgrading-from-tigergraph-operator-100-and-later-versions-to-version-130)
    - [Upgrading kubectl-tg plugin](#upgrading-kubectl-tg-plugin)
      - [Upgrading TigerGraph Operator](#upgrading-tigergraph-operator)
  - [Upgrading from TigerGraph Operator versions prior to 1.0.0 to version 1.0.0 and above](#upgrading-from-tigergraph-operator-versions-prior-to-100-to-version-100-and-above)
    - [Backup the TigerGraph CR for an Old TigerGraph Cluster Version](#backup-the-tigergraph-cr-for-an-old-tigergraph-cluster-version)
    - [Delete the TigerGraph cluster but retain the PVC of the TigerGraph Pods](#delete-the-tigergraph-cluster-but-retain-the-pvc-of-the-tigergraph-pods)
    - [Ensure that the PVC of the TigerGraph cluster still exists](#ensure-that-the-pvc-of-the-tigergraph-cluster-still-exists)
    - [Install the latest or target version of `kubectl-tg`](#install-the-latest-or-target-version-of-kubectl-tg)
    - [Uninstall the old version of TigerGraph Operator and TigerGraph CRDs](#uninstall-the-old-version-of-tigergraph-operator-and-tigergraph-crds)
      - [Uninstall the old version of TigerGraph Operator](#uninstall-the-old-version-of-tigergraph-operator)
      - [Delete the old version of TigerGraph CRDs](#delete-the-old-version-of-tigergraph-crds)
    - [Install the new version of TigerGraph Operator](#install-the-new-version-of-tigergraph-operator)
    - [Deploy TigerGraph cluster with the same cluster configuration](#deploy-tigergraph-cluster-with-the-same-cluster-configuration)
  - [How to upgrade for optional change](#how-to-upgrade-for-optional-change)
  - [How to upgrade for a specific breaking change](#how-to-upgrade-for-a-specific-breaking-change)
    - [Key considerations for upgrading to TigerGraph Operator 1.0.0 or above and TigerGraph 3.10.0 or above](#key-considerations-for-upgrading-to-tigergraph-operator-100-or-above-and-tigergraph-3100-or-above)
      - [Exposing Nginx Service Instead of Exposing RESTPP and GST (Tools and GUI) Services](#exposing-nginx-service-instead-of-exposing-restpp-and-gst-tools-and-gui-services)
      - [Performing a Full Backup Before Running HA Update, Shrink, and Expand Operations](#performing-a-full-backup-before-running-ha-update-shrink-and-expand-operations)
      - [Upgrading a TigerGraph Cluster to Use Multiple PVCs](#upgrading-a-tigergraph-cluster-to-use-multiple-pvcs)
  - [Troubleshooting](#troubleshooting)
    - [Successfully upgraded the operator from versions above 1.0.0 to 1.2.0, but still can’t create a TigerGraph cluster with the new features released in 1.2.0](#successfully-upgraded-the-operator-from-versions-above-100-to-120-but-still-cant-create-a-tigergraph-cluster-with-the-new-features-released-in-120)
    - [Successfully upgraded the operator from version 0.0.9 to version 1.2.0 and earlier, but still encountered some errors when creating a TigerGraph cluster](#successfully-upgraded-the-operator-from-version-009-to-version-120-and-earlier-but-still-encountered-some-errors-when-creating-a-tigergraph-cluster)
    - [Failed to upgrade the operator from version 0.0.9 to version 1.3.0 and above](#failed-to-upgrade-the-operator-from-version-009-to-version-130-and-above)

## Upgrading from TigerGraph Operator 1.0.0 and later versions to version 1.3.0

### Upgrading kubectl-tg plugin

To upgrade the kubectl-tg plugin for TigerGraph Operator 1.3.0, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.3.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

Ensure you have installed the correct version of kubectl-tg:

```bash
kubectl tg version

Version: 1.3.0
Default version of TigerGraph cluster: 4.1.1
```

> [!WARNING]
> If you have upgraded to the new version of kubectl-tg, you must also upgrade the TigerGraph Operator and TigerGraph CRDs. Otherwise, you may encounter unexpected errors when operating the cluster with `kubectl-tg`.

#### Upgrading TigerGraph Operator

There are no breaking changes in the TigerGraph CRDs for version 1.3.0 compared to versions 1.0.0 and above. You can upgrade the TigerGraph Operator by following these steps if you have an older version (1.0.0 or above) installed.

> [!IMPORTANT]
> There is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss. It is necessary to upgrade TigerGraph CRDs manually for the operator version prior to 1.3.0. However, for operator version 1.3.0, we use [Helm chart’s pre-upgrade hook](https://helm.sh/docs/topics/charts_hooks/) to upgrade the CRDs automatically. You can ignore the first step if you upgrade the operator to version 1.3.0 or above.

- Upgrade the TigerGraph CRDs to the latest version(It's required for the operator version prior to 1.3.0)

  ```bash
  kubectl apply -f https://dl.tigergraph.com/k8s/${OPERATOR_VERSION}/tg-operator-crd.yaml
  ```

- Upgrade the TigerGraph Operator to the specific version using the `kubectl-tg` plugin

  ```bash
  kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version ${OPERATOR_VERSION}
  ```
  
  Ensure TigerGraph Operator has been upgraded successfully:

  ```bash
  helm list -n ${YOUR_NAMESPACE_OF_OPERATOR}
  
  NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                           APP VERSION      
  tg-operator     tigergraph      2               2024-06-24 10:34:23.185036124 +0000 UTC deployed        tg-operator-1.2.0                1.2.0
  ```

## Upgrading from TigerGraph Operator versions prior to 1.0.0 to version 1.0.0 and above

Starting with TigerGraph Operator 1.0.0, the fields `spec.initTGConfig.license` and `spec.initTGConfig.ha` have been moved to `spec.license` and `spec.ha`, respectively. Additionally, `the spec.initTGConfig` field has been removed. You may encounter the error below if you only upgrade kubectl-tg to the latest version without upgrading TigerGraph Operator and CRDs.

```bash
kubectl tg create --namespace tigergraph --cluster-name test-cluster --license  ${YOUR_LICENSE} \ 
-k ssh-key-secret --size 3 --ha 2  --storage-class standard --storage-size 100G --cpu 6000m --memory 12Gi

Error from server (BadRequest): error when creating "STDIN": TigerGraph in version "v1alpha1" cannot be handled as a TigerGraph: strict decoding error: unknown field "spec.ha", unknown field "spec.license"
```

> [!WARNING]
> To ensure a successful upgrade, it is essential to upgrade the TigerGraph Operator, TigerGraph CRDs, and `kubectl-tg` simultaneously. Failing to do so may result in the upgrade process failing entirely.

For breaking changes, such as upgrading the TigerGraph Operator from version 0.0.9 to 1.0.0 or above, you need to upgrade the TigerGraph Operator, CRDs, and the TigerGraph cluster by following the steps below.

> [!NOTE]
> In the following examples, we assume that the TigerGraph Operator and a TigerGraph cluster named `test-cluster` are installed in the `tigergraph` namespace. Please update these names according to your needs.

### Backup the TigerGraph CR for an Old TigerGraph Cluster Version

If you are using a YAML CR to deploy a TigerGraph cluster, please keep the old version before you upgrade. Here, we use `kubectl-tg` for cluster deployment as an example.

Back up the TigerGraph CR for the old cluster version using `kubectl-tg`; it can be used for recreating the cluster after upgrading the TigerGraph Operator to the new version:

```bash
kubectl tg export --cluster-name test-cluster -n tigergraph

The CR of cluster test-cluster is exported to test-cluster_backup_1719223812.yaml
```

### Delete the TigerGraph cluster but retain the PVC of the TigerGraph Pods

> [!WARNING]
> You must not delete the PVC of the TigerGraph cluster Pods; otherwise, you will be unable to recreate the cluster with the existing graph data.

```bash
kubectl tg delete --cluster-name test-cluster -n tigergraph

tigergraph.graphdb.tigergraph.com "test-cluster" deleted
you can delete all related backup/restore/backup-schedule resources by:
  kubectl tg delete --cluster-name test-cluster --namespace tigergraph --cascade
or you can delete them by running:
  kubectl delete tgbackup --namespace tigergraph -l tigergraph.com/backup-cluster=test-cluster
  kubectl delete tgbackupsch --namespace tigergraph -l tigergraph.com/backup-cluster=test-cluster
  kubectl delete tgrestore --namespace tigergraph -l tigergraph.com/restore-cluster=test-cluster
Notice: you can delete all pvc of this cluster by running:
  kubectl delete pvc --namespace tigergraph -l tigergraph.com/cluster-name=test-cluster
```

### Ensure that the PVC of the TigerGraph cluster still exists

```bash
kubectl get pvc -l tigergraph.com/cluster-name=test-cluster -n tigergraph

NAME                     STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE
tg-data-test-cluster-0   Bound    pvc-b60a11ce-da18-45ca-8f9d-721dda07fdef   10G        RWO            standard       <unset>                 11m
tg-data-test-cluster-1   Bound    pvc-1f0ea4f4-94f0-4169-9101-29aa7ed640a5   10G        RWO            standard       <unset>                 11m
tg-data-test-cluster-2   Bound    pvc-73d58df7-206e-4c58-aa91-702df9761fac   10G        RWO            standard       <unset>                 11m
```

### Install the latest or target version of `kubectl-tg`

```bash
curl https://dl.tigergraph.com/k8s/1.3.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

Ensure you have installed the correct version of kubectl-tg:

```bash
kubectl tg version

Version: 1.3.0
Default version of TigerGraph cluster: 4.1.1
```

### Uninstall the old version of TigerGraph Operator and TigerGraph CRDs

#### Uninstall the old version of TigerGraph Operator
  
```bash
kubectl tg uninstall -n tigergraph
```

Ensure TigerGraph Operator has been uninstalled:

```bash
helm list -n tigergraph
```

#### Delete the old version of TigerGraph CRDs

> [!IMPORTANT]
> There is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss. It is necessary to upgrade or delete TigerGraph CRDs manually.

```bash
kubectl delete crd tigergraphbackups.graphdb.tigergraph.com
kubectl delete crd tigergraphbackupschedules.graphdb.tigergraph.com
kubectl delete crd tigergraphrestores.graphdb.tigergraph.com
kubectl delete crd tigergraphs.graphdb.tigergraph.com
```

### Install the new version of TigerGraph Operator

```bash
kubectl tg init --namespace tigergraph
```

Ensure TigerGraph Operator has been installed successfully:

```bash
helm list -n tigergraph

NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                           APP VERSION      
tg-operator     tigergraph      1               2024-09-10 10:34:23.185036124 +0000 UTC deployed        tg-operator-1.3.0                1.3.0
```

```bash
kubectl wait deployment tigergraph-operator-controller-manager --for condition=Available=True --timeout=120s -n tigergraph

deployment.apps/tigergraph-operator-controller-manager condition met
```

### Deploy TigerGraph cluster with the same cluster configuration

If you don't remember the cluster configuration, you can check the previously saved TigerGraph CR. You must ensure the configurations below are the same as before

If you want to use the TigerGraph CR YAML file to deploy the cluster, you need to check the new version [TigerGraph cluster CR sample example](../09-samples/deploy/tigergraph-cluster.yaml) and update the cluster configuration according to your old version TigerGraph CR.

- Create a private ssh key secret if necessary

  ```bash
  echo -e 'y\\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''

  kubectl create secret generic ssh-key-secret --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace tigergraph
  ```

- Recreate the cluster with the created private ssh key

> [!WARNING]
> Cluster name, size, ha, and storage configuration must be the same.

  ```bash
  kubectl tg create --namespace tigergraph --cluster-name test-cluster --version 3.9.3 --license ${YOUR_LICENSE} \
  -k ssh-key-secret --size 3 --ha 2  --storage-class standard --storage-size 100G --cpu 6000m --memory 12Gi
  ```

  Ensure TigerGraph cluster version and status:

  ```bash
  kubectl get tg test-cluster -n tigergraph

  NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                                             SERVICE-TYPE   REGION-AWARENESS   CONDITION-TYPE   CONDITION-STATUS   AGE
  test-cluster   3          3              2            docker.io/tigergraph/tigergraph-k8s:3.9.3                   LoadBalancer                      Normal           True               15m
  ```

  Then, you can upgrade the TigerGraph cluster to a new version, here we upgrade the cluster version to 4.1.0

  ```bash
  kubectl tg update  --cluster-name test-cluster --version 4.1.0 --namespace tigergraph
  ```

## How to upgrade for optional change

If you don't require the new optional configuration of the TigerGraph CRD, no extra steps are needed. However, if you wish to use the new optional configuration, you can simply update the cluster as needed.

## How to upgrade for a specific breaking change

In order to optimize the user experience of the TigerGraph Operator, such as improving ease of use and removing some configurations that are no longer used, upgrading the TigerGraph Operator and TigerGraph cluster may introduce breaking changes. If you have an older version of the TigerGraph Operator and TigerGraph cluster, please follow the key considerations and upgrade steps carefully.

### Key considerations for upgrading to TigerGraph Operator 1.0.0 or above and TigerGraph 3.10.0 or above

#### Exposing Nginx Service Instead of Exposing RESTPP and GST (Tools and GUI) Services

- The new TigerGraph cluster CRD is applicable starting from TigerGraph 3.9.2. In TigerGraph clusters before 3.9.2, after the CRD is updated, the image must be upgraded to 3.9.2 or above.

  ```bash
    kubectl tg update --cluster-name ${cluster_name} --version 4.1.0 -n ${NAMESPACE_OF_YOUR_CLUSTER}
  ```

- If using the new TigerGraph cluster CRD (1.0.0 and above) with an older version of the TigerGraph image, the NGINX service cannot serve correctly on the Tools, RESTPP, and Informant services.
- If using the old TigerGraph CRDs (<=0.0.9), the Tools (GUI), RESTPP, and Informant services cannot serve on 3.6.3 but can serve on 3.7.0 and above.

#### Performing a Full Backup Before Running HA Update, Shrink, and Expand Operations

If the HA update process is interrupted by an operation such as the update pod being killed or the update job being terminated, the TigerGraph cluster may be damaged and unrecoverable automatically, even if the HA update pod is recreated and the HA update job is rerun. Therefore, it is necessary to prepare a full backup before running the HA update.

> [!WARNING]
> If the license has expired, the shrink/expand process will be stuck for a long time at exporting/importing graph data and will eventually fail.

#### Upgrading a TigerGraph Cluster to Use Multiple PVCs

Best practice for upgrading TigerGraph <=3.9.3 with a single PVC and TigerGraph Operator <=0.0.9 to TigerGraph 3.10.0 or above with Multiple PVCs and TigerGraph Operator 1.0.0 or Above:

- In an environment with TigerGraph <=3.9.3 and TigerGraph Operator <=0.0.9, perform a backup of TigerGraph to S3 or local storage first.

> [!WARNING]
> If you use local backup, ensure it is stored outside the Pod for persistent storage.

  For instructions on how to perform a backup of TigerGraph to S3 or local storage, please refer to the following documents for details:

> [!IMPORTANT]
> Please set `cleanPolicy` to `Retain` when performing a backup of TigerGraph to S3 or local storage,
> otherwise, the backup package will be deleted when deleting TigerGraph CRDs or TigerGraph cluster with `--cascade` option.

  [Backup & Restore cluster by CR](../04-manage/backup-and-restore/backup-restore-by-cr.md)

  [Backup & Restore cluster kubectl-tg plugin](../04-manage/backup-and-restore/backup-restore-by-kubectl-tg.md)

- Remove the legacy TigerGraph Cluster and delete TigerGraph's Persistent Volume Claims (PVC).

  ```bash
  kubectl tg delete --cluster-name $YOUR_CLUSTER_NAME --namespace $YOUR_NAMESPACE --cascade
  kubectl delete pvc --namespace default -l tigergraph.com/cluster-name=$YOUR_CLUSTER_NAME
  ```

- Uninstall the legacy TigerGraph Operator and delete all TigerGraph CRDs.

  ```bash
  kubectl tg uninstall
  kubectl delete crd tigergraphbackups.graphdb.tigergraph.com
  kubectl delete crd tigergraphbackupschedules.graphdb.tigergraph.com
  kubectl delete crd tigergraphrestores.graphdb.tigergraph.com
  kubectl delete crd tigergraphs.graphdb.tigergraph.com
  ```

- Install the new TigerGraph Operator 1.0.0 or above, which will simultaneously install the new CRDs.

  ```bash
  curl https://dl.tigergraph.com/k8s/1.2.0/kubectl-tg  -o kubectl-tg
  sudo install kubectl-tg /usr/local/bin/

  kubectl tg init --namespace $YOUR_NAMESPACE
  ```

- Create a new TigerGraph Cluster with the same version as before and customize it by adding `additionalStorage` and `customVolume`.

  Refer to the documentation [Multiple persistent volumes mounting](../03-deploy/multiple-persistent-volumes-mounting.md) for details.
  
- Restore the old TigerGraph cluster using the backup package you created previously.

  For instructions on how to restore a TigerGraph cluster from S3 or local storage, please refer to the following documents for details:

  [Backup & Restore cluster by CR](../04-manage/backup-and-restore/backup-restore-by-cr.md)

  [Backup & Restore cluster kubectl-tg plugin](../04-manage/backup-and-restore/backup-restore-by-kubectl-tg.md)

- After completing the restore process, upgrade the TigerGraph cluster to version 4.1.0 using the appropriate command.
  
  ```bash
  kubectl tg update --cluster-name $YOUR_CLUSTER_NAME --version 4.1.0 --namespace $YOUR_NAMESPACE
  ```

## Troubleshooting

### Successfully upgraded the operator from versions above 1.0.0 to 1.2.0, but still can’t create a TigerGraph cluster with the new features released in 1.2.0

Starting from Helm V3, there is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss.

This means that even if we successfully upgrade the TigerGraph Operator, we still need to manually upgrade the TigerGraph CRDs. Otherwise, you may encounter the following issues:

```bash
error: error validating "sidecar-loadbanalance.yaml": error validating data: ValidationError(TigerGraph.spec): unknown field "sidecarListener" in com.tigergraph.graphdb.v1alpha1.TigerGraph.spec; if you choose to ignore these errors, turn validation off with --validate=false
```

If you encounter the issue of an unknown field, you can manually upgrade the TigerGraph CRDs using the following command:

```bash
  kubectl apply -f https://dl.tigergraph.com/k8s/${OPERATOR_VERSION}/tg-operator-crd.yaml
```

> [!NOTE]
> Starting from operator version 1.3.0, we use the Helm chart’s pre-upgrade hook to automatically upgrade the CRDs, provided there are no breaking changes during the CRD upgrade.

### Successfully upgraded the operator from version 0.0.9 to version 1.2.0 and earlier, but still encountered some errors when creating a TigerGraph cluster

Starting with TigerGraph Operator 1.0.0, the fields `spec.initTGConfig.license` and `spec.initTGConfig.ha` have been moved to `spec.license` and `spec.ha`, respectively. Additionally, `the spec.initTGConfig` field has been removed. You may encounter the error below if you only upgrade kubectl-tg to the latest version without upgrading TigerGraph Operator and CRDs.

```bash
kubectl tg create --namespace tigergraph --cluster-name test-cluster --license  ${YOUR_LICENSE} \ 
-k ssh-key-secret --size 3 --ha 2  --storage-class standard --storage-size 100G --cpu 6000m --memory 12Gi

Error from server (BadRequest): error when creating "STDIN": TigerGraph in version "v1alpha1" cannot be handled as a TigerGraph: strict decoding error: unknown field "spec.ha", unknown field "spec.license"
```

If you encounter the above issue, please refer to the section [Upgrading from TigerGraph Operator versions prior to 1.0.0 to version 1.0.0 and above](#upgrading-from-tigergraph-operator-versions-prior-to-100-to-version-100-and-above).

### Failed to upgrade the operator from version 0.0.9 to version 1.3.0 and above

Starting from operator version 1.3.0, we use the Helm chart’s pre-upgrade hook to upgrade the CRDs automatically, provided there are no breaking changes during the CRD upgrade. However, the pre-upgrade hook will fail the upgrade if there is a schema difference, such as upgrading from version 0.0.9 to 1.3.0. The examples of errors are as follows:

- Upgrade Operator failed

```bash
$ kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version ${OPERATOR_VERSION}
Error: UPGRADE FAILED: pre-upgrade hooks failed: job failed: BackoffLimitExceeded
Major version has changed from 0 to 1

$ helm list -n tigergraph
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS  CHART                           APP VERSION  
tg-operator     tigergraph      2               2024-09-09 08:42:19.063457181 +0000 UTC failed  tg-operator-1.3.0               1.3.0
```

- Check the error logs of pre-upgrade job

```bash
$ export YOUR_NAMESPACE=tigergraph
$ kubectl logs $(kubectl get pods --namespace ${YOUR_NAMESPACE} | grep tg-operator-pre-upgrade | awk 'NR==1{print $1}') --namespace ${YOUR_NAMESPACE}
Processing file: /crds/tigergraphbackups.graphdb.tigergraph.com.yaml
time="2024-09-09T08:42:26Z" level=info msg="The GroupResource of the validated CR graphdb.tigergraph.com, tigergraphbackups"
time="2024-09-09T08:42:26Z" level=info msg="GroupVersionResource graphdb.tigergraph.com/v1alpha1, Resource=tigergraphbackups"
2024/09/09 08:42:26 CRD tigergraphbackups.graphdb.tigergraph.com compatibility validation passed
Processing file: /crds/tigergraphbackupschedules.graphdb.tigergraph.com.yaml
time="2024-09-09T08:42:26Z" level=info msg="The GroupResource of the validated CR graphdb.tigergraph.com, tigergraphbackupschedules"
time="2024-09-09T08:42:26Z" level=info msg="GroupVersionResource graphdb.tigergraph.com/v1alpha1, Resource=tigergraphbackupschedules"
2024/09/09 08:42:26 CRD tigergraphbackupschedules.graphdb.tigergraph.com compatibility validation passed
Processing file: /crds/tigergraphrestores.graphdb.tigergraph.com.yaml
time="2024-09-09T08:42:27Z" level=info msg="The GroupResource of the validated CR graphdb.tigergraph.com, tigergraphrestores"
time="2024-09-09T08:42:27Z" level=info msg="GroupVersionResource graphdb.tigergraph.com/v1alpha1, Resource=tigergraphrestores"
2024/09/09 08:42:27 CRD tigergraphrestores.graphdb.tigergraph.com compatibility validation passed
Processing file: /crds/tigergraphs.graphdb.tigergraph.com.yaml
time="2024-09-09T08:42:27Z" level=info msg="The GroupResource of the validated CR graphdb.tigergraph.com, tigergraphs"
time="2024-09-09T08:42:27Z" level=info msg="GroupVersionResource graphdb.tigergraph.com/v1alpha1, Resource=tigergraphs"
time="2024-09-09T08:42:27Z" level=info msg="CR Name: test-cluster, namespace: tigergraph"
2024/09/09 08:42:27 Error walking through directory: error validating existing CRs against new CRD's schema for "tigergraphs.graphdb.tigergraph.com": error validating graphdb.tigergraph.com/v1alpha1, 
Kind=TigerGraph "tigergraph/test-cluster": updated validation is too restrictive: [[].spec.ha: Required value, [].spec.license: Required value, 
[].status.conditions[0].lastTransitionTime: Required value, [].status.conditions[0].message: Required value, [].status.conditions[0].reason: Required value, 
[].status.conditions[1].lastTransitionTime: Required value, [].status.conditions[1].reason: Required value, 
[].status.conditions[2].lastTransitionTime: Required value, [].status.conditions[2].reason: Required value, 
[].status.conditions[3].lastTransitionTime: Required value, [].status.conditions[3].reason: Required value]
the schema compatibility check for the CRDs failed, there may be breaking changes during the operator upgrade. Please refer to the operator upgrade documentation for details.
```

If you encounter the above issue, please refer to the section [Upgrading from TigerGraph Operator versions prior to 1.0.0 to version 1.0.0 and above](#upgrading-from-tigergraph-operator-versions-prior-to-100-to-version-100-and-above).
