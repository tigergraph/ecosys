# Expand Storage of TigerGraph cluster

- [Expand Storage of TigerGraph cluster](#expand-storage-of-tigergraph-cluster)
  - [Introduction](#introduction)
  - [Prerequisites](#prerequisites)
  - [Expand storage of TigerGraph](#expand-storage-of-tigergraph)
    - [Expand storage by modifying TigerGraph CR](#expand-storage-by-modifying-tigergraph-cr)
    - [Expand storage by kubectl-tg](#expand-storage-by-kubectl-tg)
    - [Workflows of expanding storage](#workflows-of-expanding-storage)
  - [Troubleshooting](#troubleshooting)
    - [TigerGraph CR is in status `StorageExpandCheck, False`](#tigergraph-cr-is-in-status-storageexpandcheck-false)
    - [TigerGraph CR stuck in status `StorageExpanding, Unknown`](#tigergraph-cr-stuck-in-status-storageexpanding-unknown)
  - [How to shrink the storage of TigerGraph cluster](#how-to-shrink-the-storage-of-tigergraph-cluster)

## Introduction

TigerGraph Operator uses StatefulSet to deploy TigerGraph cluster. The storage of each pod is defined in `.spec.storage` of TigerGraph CR.
TigerGraph Operator will generate `VolumeClaimTemplate` based on `.spec.storage` and put it in StatefulSet.

It is a well-known limitation that K8s does not allow expanding the storage of PVC by modifying `VolumeClaimTemplate` in StatefulSet directly. If you want to expand PVCs of a StatefulSet, you need to modify each PVC created by the StatefulSet manually.

From TigerGraph Operator 1.2.0, we provide a way to expand the storage of TigerGraph cluster by modifying `.spec.storage` in TigerGraph CR directly. TigerGraph Operator will expand the PVCs automatically.

When you want to expand the storage of TigerGraph cluster, you can modify `.spec.storage` in TigerGraph CR and apply it or use `kubectl tg` command to increase the storage size. TigerGraph Operator will expand each PVC of the TigerGraph cluster automatically.

## Prerequisites

To expand the storage of TigerGraph cluster, you need to make sure the following prerequisites are met:

1. You have installed TigerGraph Operator 1.2.0 or later.
2. You have a running TigerGraph cluster.
3. The StorageClass of the PVCs that you want to expand supports volume expansion. You can use following command to check if the StorageClass supports volume expansion:

    ```bash
    kubectl get sc <storage-class-name> -o jsonpath='{.allowVolumeExpansion}'
    ```

    If the output is `true`, the StorageClass supports volume expansion.
    If the output is `false`, it doesn't mean that the CSI used by the StorageClass doesn't support volume expansion. You can refer to the documentation of the CSI driver.
    If the CSI driver supports volume expansion, you can update the StorageClass to allow volume expansion by the following command:

    ```bash
    kubectl patch sc <storage-class-name> -p '{"allowVolumeExpansion": true}'
    ```

## Expand storage of TigerGraph

Before expanding storage of TigerGraph, there are some limitations you need to know:

1. Since K8s only supports **expanding** PVCs, **shrinking** PVCs is forbidden. When you try to decrease the storage size, TigerGraph Operator will reject the request by webhook. If you want to shrink the storage of TigerGraph cluster, you can refer to [How to shrink the storage of TigerGraph cluster](#how-to-shrink-the-storage-of-tigergraph-cluster).
2. You are only allowed to increase the storage size of TigerGraph cluster. You can't change the StorageClass or any other attributes of the PVCs.
3. When TigerGraph Operator is expanding the storage of TigerGraph cluster, the Pods will be deleted and recreated, because some StorageClass does not support online volume expansion. That means at the time of expansion, the Pods will be unavailable. So you can't backup/restore or run gsql query during the expansion.
4. It is possible that the CSI driver of the StorageClass you are using may have an error during the storage expansion process. However, TigerGraph Operator cannot detect this error, and the TigerGraph CR will be stuck in the `StorageExpanding, Unknown` status. Please refer to [Troubleshooting](#troubleshooting) for more information.

> [!IMPORTANT]
> It is highly recommended to backup the cluster before expanding the storage of TigerGraph cluster.
> In case of any failure during the expansion process, you can restore the cluster from the backup.
> Please refer to [Backup and Restore](../backup-restore/README.md) for how to backup the cluster.

### Expand storage by modifying TigerGraph CR

You can expand the storage of TigerGraph cluster by modifying `.spec.storage` in TigerGraph CR directly. For example, assume that you have created a TigerGraph CR:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  license: $YOUR_LICENSE
  replicas: 3
  ha: 1
  resources:
    requests:
      cpu: 6
      memory: 12Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 20Gi
      storageClassName: standard
      volumeMode: Filesystem
    additionalStorages:
      - name: tg-kafka
        storageSize: 15Gi
        storageClassName: standard
```

You can modify the `storage` field in the TigerGraph CR to expand the storage size. For example, you can increase the storage size of the main storage to 30Gi and the storage size of the additional storage `tg-kafka` to 20Gi:

```yaml
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 30Gi
      storageClassName: standard
      volumeMode: Filesystem
    additionalStorages:
      - name: tg-kafka
        storageSize: 20Gi
        storageClassName: standard
```

After you apply the modified TigerGraph CR, TigerGraph Operator will expand the PVCs of the TigerGraph cluster automatically.

### Expand storage by kubectl-tg

You can use `kubectl tg` command to expand the storage of TigerGraph cluster. Assume that you have created a TigerGraph cluster by the following command:

```bash
kubectl tg create -n tigergraph -c test-cluster -k ssh-key-secret -l $YOUR_LICENSE --version 4.1.0 --size 3 \
  --storage-class standard --storage-size 20Gi --additional-storages additional-storage.yaml
```

The file `additional-storage.yaml` contains the additional storage configuration:

```yaml
additionalStorages:
  - name: tg-kafka
    storageSize: 20Gi
    storageClassName: standard
```

If you want to expand the storage size of the main storage to 30Gi and the storage size of the additional storage `tg-kafka` to 25Gi, you can modify the `additional-storage.yaml` file first:

```yaml
additionalStorages:
  - name: tg-kafka
    storageSize: 25Gi
    storageClassName: standard
```

Then you can use the following command to expand the storage of TigerGraph cluster:

```bash
kubectl tg update -n tigergraph -c test-cluster --storage-size 30Gi --additional-storages additional-storage.yaml
```

### Workflows of expanding storage

When you increase the storage size in `.spec.storage` of TigerGraph CR, TigerGraph Operator will do the following steps to expand the PVCs:

1. Transfer the TigerGraph CR to `StorageExpandCheck, Unknown` status.
2. In `StorageExpandCheck, Unknown` status, check if the PVCs of the TigerGraph cluster can be expanded. If the PVCs can be expanded, transfer the TigerGraph CR to `StorageExpandPre, Unknown` status. Otherwise, transfer the TigerGraph CR to `StorageExpandCheck, False` status.
3. In `StorageExpandPre, Unknown` status, delete the Pods of the TigerGraph cluster to pause the cluster. After the Pods are deleted, transfer the TigerGraph CR to `StorageExpanding, Unknown` status.
4. In `StorageExpanding, Unknown` status, expand the PVCs of the TigerGraph cluster by modifying PVCs one by one. After the PVCs are expanded, transfer the TigerGraph CR to `StorageExpandPost, Unknown` status.
5. In `StorageExpandPost, Unknown` status, recreate the Pods of the TigerGraph cluster to resume the cluster. After the Pods are ready, transfer the TigerGraph CR to `Normal, True` status.

Some errors may occur during `StorageExpandCheck` or `StorageExpanding` status. Please see next section [Troubleshooting](#troubleshooting) for how to troubleshoot these errors.

## Troubleshooting

Run the following command to check the status of the TigerGraph CR:

```bash
kubectl get tg -n $NAMESPACE $CLUSTER_NAME'
```

The output is like:

```bash
NAME              REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                              SERVICE-TYPE   REGION-AWARENESS   CONDITION-TYPE      CONDITION-STATUS   AGE
example-cluster   3          3              1            docker.io/tigergraph/tigergraph-k8s:3.10.1   LoadBalancer                      StorageExpandCheck  False              6h41m
```

### TigerGraph CR is in status `StorageExpandCheck, False`

If the status is `StorageExpandCheck, False`, it means that the PVCs of the TigerGraph cluster cannot be expanded. You can check the error message in the `.status.conditions` field of the TigerGraph CR:

```bash
kubectl get tg -n $NAMESPACE $CLUSTER_NAME -o jsonpath='{.status.conditions}'
```

The output is like:

```bash
[{"lastTransitionTime":"2024-06-26T08:39:50Z","message":"storage class sc-not-allow-expansion doesn't support volume expansion","reason":"ClusterStorageExpandCheckFalse","status":"False","type":"StorageExpandCheck"}]
```

It shows that the StorageClass `sc-not-allow-expansion` does not support volume expansion. So you are not allowed to expand PVCs whose StorageClass is `sc-not-allow-expansion`. You have to change the storage size backup to the original size. Then the TigerGraph CR will be transferred to `Normal, True` status.

### TigerGraph CR stuck in status `StorageExpanding, Unknown`

If the status is `StorageExpanding, Unknown` and it has been stuck for a long time, it means that some errors occurred when expanding the PVCs. You need to check the events of the PVCs by the following command:

```bash
kubectl get events -n $NAMESPACE --field-selector involvedObject.name=$PVC_NAME
```

There are many possible errors that may occur during the storage expansion process. Since different CSI drivers have different implementations, it is hard to list all the possible errors here. If you encounter any error reported by specific CSI driver, you can refer to the documentation of the CSI driver for more information.

## How to shrink the storage of TigerGraph cluster

As mentioned before, K8s does not allow to shrink the storage of PVCs. So we do not support shrinking the storage of TigerGraph cluster either. If you really want to shrink the storage of TigerGraph cluster, we have such a workaround:

1. Backup the TigerGraph cluster to S3 bucket. Please refer to [Backup and Restore](../backup-restore/README.md) for how to backup the cluster.
2. Delete the TigerGraph CR and clear the PVCs of the TigerGraph cluster.
3. Create a new TigerGraph CR with the smaller storage size and restore the cluster from the backup.
