# How to resize persistent volumes of TigerGraph cluster on Kubernetes

This document provides instructions on resizing persistent volumes for a TigerGraph cluster on Kubernetes.

Currently, Kubernetes offers automatic volume resizing for persistent volumes, but not for volumes associated with StatefulSets. The TigerGraph Kubernetes Operator relies on StatefulSets for orchestrating TigerGraph pods. Therefore, when dealing with persistent volumes associated with the TigerGraph Operator, a unique set of manual procedures is required to facilitate volume resizing.

> [!WARNING]
> Resizing PVCs using this method only works your StorageClass supports AllowVolumeExpansion=True.

Follow these steps to resize persistent volumes attached to the StatefulSet of a TigerGraph cluster on Kubernetes:

1. Update the storage class to allow volume expansion.
2. Delete the TigerGraph cluster, but keep the PVC (Persistent Volume Claim).
3. Patch the PVC to the new size.
4. Recreate the TigerGraph cluster with the new volume size.

## Update storageclass to allow volume expansion

### GKE

On GKE, all the preinstalled storage classes have `ALLOWVOLUMEEXPANSION` enabled, so there is no need to change it. You can check it using the following command:

```bash
kubectl get storageclass
NAME                     PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
premium-rwo              pd.csi.storage.gke.io   Delete          WaitForFirstConsumer   true                   5h27m
standard                 kubernetes.io/gce-pd    Delete          Immediate              true                   5h27m
standard-rwo (default)   pd.csi.storage.gke.io   Delete          WaitForFirstConsumer   true                   5h27m
```

### EKS

- Install the EBS CSI (Elastic Block Store Container Storage Interface) driver (optional):

Since some EKS versions do not install aws-ebs-csi-driver plugin by default, if you encounter the following issue when creating TG cluster with the dynamic persistent volume, you need to check it first.

```bash
# please replace the cluster name and namespace with yours.
$ kubectl describe pvc -l tigergraph.com/cluster-name=test-cluster --namespace tigergraph
Name:          tg-data-test-cluster-0
Namespace:     tigergraph
StorageClass:  gp2
Status:        Pending
Volume:
Labels:        tigergraph.com/cluster-name=test-cluster
               tigergraph.com/cluster-pod=test-cluster
Annotations:   volume.beta.kubernetes.io/storage-provisioner: ebs.csi.aws.com
               volume.kubernetes.io/selected-node: ip-172-31-20-181.us-west-1.compute.internal
               volume.kubernetes.io/storage-provisioner: ebs.csi.aws.com
Finalizers:    [kubernetes.io/pvc-protection]
Capacity:
Access Modes:
VolumeMode:    Filesystem
Used By:       test-cluster-0
Events:
  Type    Reason                Age                    From                         Message
  ----    ------                ----                   ----                         -------
  Normal  WaitForFirstConsumer  8m9s                   persistentvolume-controller  waiting for first consumer to be created before binding
  Normal  ExternalProvisioning  2m35s (x25 over 8m9s)  persistentvolume-controller  waiting for a volume to be created, either by external provisioner "ebs.csi.aws.com" or manually created by system administrator
```

- Check and install `aws-ebs-csi-driver` with following commands:

```bash
kubectl get deployment ebs-csi-controller -n kube-system

aws eks create-addon --cluster-name ${YOUR_K8S_CLUSTER_NAME} --addon-name aws-ebs-csi-driver
```

- Update storageclass to allow volume expansion

```bash
$ kubectl get sc gp2
NAME            PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
gp2 (default)   kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   false                  87m

$ kubectl patch sc gp2 -p '{"allowVolumeExpansion": true}'
storageclass.storage.k8s.io/gp2 patched

$ kubectl get sc gp2
NAME            PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
gp2 (default)   kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   true                   88m
```

## Delete the TG cluster and remain the PVC

The following examples assume that the name of the TG cluster is test-cluster and it is installed in namespace tigergraph, please replace them according to your requirements.

You should also specify the correct storageclas(`--storage-class)` according to your K8s enviroment.

- Create TG cluster(Optional)

You can skip this step if there is an existing TigerGraph cluster; it's only used for testing. The following examples assume that the name of the TigerGraph cluster is test-cluster, and it is installed in the tigergraph namespace.

```bash
kubectl tg create --cluster-name test-cluster --license xxxxxxxxx -k ssh-key-secret --size 6 --ha 2 --version 3.9.1 --storage-class gp2 --storage-size 10G --cpu 3000m --memory 8Gi -n tigergraph
```

- Check the volume size (Optional, for verification)

```bash
$ kubectl tg connect --cluster-name test-cluster -n tigergraph
tigergraph@test-cluster-0:~$ df -h
Filesystem      Size  Used Avail Use% Mounted on
overlay         100G   12G   89G  12% /
tmpfs            64M     0   64M   0% /dev
tmpfs            15G     0   15G   0% /sys/fs/cgroup
/dev/xvda1      100G   12G   89G  12% /etc/hosts
tmpfs            27G  8.0K   27G   1% /etc/private-key-volume
shm              64M     0   64M   0% /dev/shm
/dev/xvdaa      9.7G  916M  8.8G  10% /home/tigergraph/tigergraph/data
tmpfs            27G   12K   27G   1% /run/secrets/kubernetes.io/serviceaccount
tmpfs            15G     0   15G   0% /proc/acpi
tmpfs            15G     0   15G   0% /proc/scsi
tmpfs            15G     0   15G   0% /sys/firmware
tigergraph@test-cluster-0:~$
```

- Delete the TigerGraph cluster and keep the PVC:

> [!WARNING]
> Remember to keep the PVC to recreate the cluster; otherwise, all the data in the cluster will be lost.

```bash
kubectl tg delete --cluster-name test-cluster -n tigergraph
```

## Patch the PVC to the new size

In this example, we will demonstrate how to resize the PV from 10Gi to 20Gi using the kubectl patch command to adjust the storage size of the PVC.

We will perform this operation on all PVCs within the cluster by specifying the label `tigergraph.com/cluster-name=test-cluster`. Please ensure you replace `test-cluster` with the name of your cluster.

```bash
kubectl get pvc -l tigergraph.com/cluster-name=test-cluster --namespace tigergraph|sed '1d'|awk '{print $1}'|xargs -I {} kubectl patch pvc {} --namespace tigergraph --type merge --patch '{"spec":{"resources":{"requests":{"storage":"20Gi"}}}}'
```

## Recreate the TG cluster with the new volume size

Because the PVC was retained after deleting the TG cluster, you can quickly recreate it using the same cluster name for a swift recovery. Simultaneously, ensure that you update the volume size of the CR to match the new desired value.

```bash
# change --storage-size 10Gi to --storage-size 20Gi
kubectl tg create --cluster-name test-cluster --license xxxxxxxxx -k ssh-key-secret --size 6 --ha 2 --version 3.9.1 --storage-class gp2 --storage-size 20G --cpu 3000m --memory 8Gi -n tigergraph
```

- Check the volume size again (Optional, for verification)

After patching the PVC to the new size and recreating the cluster with the new storage size, you can find that the storage size has been updated to 20Gi.

```bash
$ kubectl tg connect --cluster-name test-cluster -n tigergraph
Defaulted container "tigergraph" out of: tigergraph, init-tigergraph (init)
tigergraph@test-cluster-0:~$ df -h
Filesystem      Size  Used Avail Use% Mounted on
overlay         100G   12G   89G  12% /
tmpfs            64M     0   64M   0% /dev
tmpfs            15G     0   15G   0% /sys/fs/cgroup
/dev/xvda1      100G   12G   89G  12% /etc/hosts
tmpfs            27G  8.0K   27G   1% /etc/private-key-volume
shm              64M     0   64M   0% /dev/shm
/dev/xvdaa       20G  185M   20G   1% /home/tigergraph/tigergraph/data
tmpfs            27G   12K   27G   1% /run/secrets/kubernetes.io/serviceaccount
tmpfs            15G     0   15G   0% /proc/acpi
tmpfs            15G     0   15G   0% /proc/scsi
tmpfs            15G     0   15G   0% /sys/firmware
```
