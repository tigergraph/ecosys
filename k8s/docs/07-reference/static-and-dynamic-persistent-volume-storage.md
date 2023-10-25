# How to use static & dynamic persistent volume storage

This document describes how to deploy a TigerGraph on K8s with static or dynamic persistent volume storage.

## GKE

### Static persistent volume storage on GKE

You can follow these steps to set up and use static persistent volume storage for GKE:

1. Provision a Persistent volume using a special storage class name.
2. Deploy TigerGraph with persistent volume.

### Creating Persistent Volumes From Existing Google Compute Disks

- Create disk

Consider a scenario where you are creating a TigerGraph cluster comprising three nodes. To achieve this, you can create three compute disks named tg-pv-1, tg-pv-2, and tg-pv-3, each with a size of 10GB.

```bash
gcloud compute disks create tg-pv-1 --zone=us-central1-a --size=10GB
gcloud compute disks create tg-pv-2 --zone=us-central1-a --size=10GB
gcloud compute disks create tg-pv-3 --zone=us-central1-a --size=10GB

# delete gcd
gcloud compute disks delete tg-pv-1 --zone=us-central1-a
gcloud compute disks delete tg-pv-2 --zone=us-central1-a
gcloud compute disks delete tg-pv-3 --zone=us-central1-a
```

Now you have three disks available to be used as PV (Persistent Volume) in GKE.

- Create static persistent pv

```java
apiVersion: v1
kind: PersistentVolume
metadata:
  name: tg-pv-storage1
spec:
  storageClassName: "tg-pv"
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  gcePersistentDisk:
    pdName: tg-pv-1
    fsType: ext4
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: tg-pv-storage2
spec:
  storageClassName: "tg-pv"
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  gcePersistentDisk:
    pdName: tg-pv-2
    fsType: ext4
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: tg-pv-storage3
spec:
  storageClassName: "tg-pv"
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  gcePersistentDisk:
    pdName: tg-pv-3
    fsType: ext4
```

- Create TG cluster with storage class name tg-pv

```bash
kubectl tg create --namespace tigergraph --cluster-name test-pv-tg-cluster -k ssh-key-secret --license xxxxxx --size 3 --ha 2 --version 3.9.1 --storage-class tg-pv --cpu 2000m --memory 8G --storage-size 10G
```

### Dynamically persistent volume storage

To enable and utilize dynamic persistent volume storage for Google Kubernetes Engine (GKE), follow these steps:

1. **Create a Storage Class:**
   Start by creating a storage class, which serves as a straightforward way to categorize and organize storage options.

2. **Deploy TigerGraph with the Created Storage Class:**
   Once the storage class is in place, proceed to deploy TigerGraph, ensuring you specify the name of the storage class created in the previous step.

A storage class essentially outlines the type of storage to be provisioned. In simpler terms, it defines how the storage behaves and what it's best suited for.

For instance, you can categorize your storage classes as `gold` and `silver`, using names that make sense for your use case. The `gold` storage class might leverage the `pd-ssd` persistent disk type, ideal for high IOPS applications like databases. Meanwhile, the `silver` storage class could utilize the `pd-standard` volume type, suitable for regular disk operations and backups.

These storage class categorizations are entirely tailored to your project's specific requirements, ensuring that the storage resources are optimally utilized based on your application's needs.

> [!NOTE]
> GKE comes with default storage classes that utilize `pd-standard` disks. If you omit specifying a storage class while provisioning a Persistent Volume (PV), the default storage class is automatically considered.

By following these steps, you can efficiently configure and leverage dynamic persistent volume storage within your GKE environment.

- Create a storage class

Save the following manifest as `storage-class.yaml`

```java
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gold
provisioner: kubernetes.io/gce-pd
volumeBindingMode: Immediate
allowVolumeExpansion: true
reclaimPolicy: Delete
parameters:
  type: pd-ssd
  fstype: ext4
  replication-type: none
```

- Create the storage class.

```bash
kubectl apply -f storage-class.yaml
```

A little explanation about the parameters.

1. **Type:**  supports `pd-standard` & `pd-ssd`. If you don’t specify anything, it defaults `pd-standard`
2. **fstype:** supports `ext4` and `xfs`. Defaults to `ext4`.
3. **replication-type:** This decides whether the disk is zonal or regional. If you don’t specify `regional-pd`, it defaults to a zonal disk.
4. **allowVolumeExpansion:** With this parameter, you can expand the persistent volume if required.
5. **volumeBindingMode:** There are two modes. `Immediate` and `WaitForFirstConsumer`. In cases where the storage is not accessible from all the nodes, use `WaitForFirstConsumer` so that volume binding will happen after the pod gets created.
6. [**reclaimPolicy**](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#reclaiming)
    1. **reclaim** policy allows for manual reclamation of the resource. When the PersistentVolumeClaim is deleted, the PersistentVolume still exists and the volume is considered "released". But it is not yet available for another claim because the previous claimant's data remains on the volume.
    2. the `Delete`reclaim policy, deletion removes both the PersistentVolume object from Kubernetes, as well as the associated storage asset in the external infrastructure, such as an AWS EBS, GCE PD, Azure Disk, or Cinder volume.

- Deploy TG with the specific Storage class name

```bash
kubectl tg create --namespace tigergraph --cluster-name dynamic-pv-tg-cluster -k ssh-key-secret --license xxxxxx --size 1 --ha 1 --version 3.9.1 --storage-class gold --cpu 4000m --memory 8G --storage-size 10G
```

## EKS

### Static persistent volume storage on eks

You can follow these steps to set up and use static persistent volume storage for EKS:

1. Provision a Persistent volume using a special storage class name.
2. Deploy TG with persistent volume.

### **Creating ESB Persistent Volumes**

- Create ESB volumes  

Consider a scenario where you are creating a TigerGraph cluster comprising three nodes. To achieve this, you can create three compute disks named tg-pv-1, tg-pv-2, and tg-pv-3, each with a size of 10GB.

```bash
$ aws ec2 create-volume --volume-type gp2 --size 10 --availability-zone us-west-1b
{
    "AvailabilityZone": "us-west-1b",
    "CreateTime": "2023-05-04T09:00:21+00:00",
    "Encrypted": false,
    "Size": 10,
    "SnapshotId": "",
    "State": "creating",
    "VolumeId": "vol-01b4da831ee293eb7",
    "Iops": 100,
    "Tags": [],
    "VolumeType": "gp2",
    "MultiAttachEnabled": false
}

$ aws ec2 create-volume --volume-type gp2 --size 10 --availability-zone us-west-1b
{
    "AvailabilityZone": "us-west-1b",
    "CreateTime": "2023-05-04T09:00:51+00:00",
    "Encrypted": false,
    "Size": 10,
    "SnapshotId": "",
    "State": "creating",
    "VolumeId": "vol-0cf5cb04ce0b30eee",
    "Iops": 100,
    "Tags": [],
    "VolumeType": "gp2",
    "MultiAttachEnabled": false
}

$ aws ec2 create-volume --volume-type gp2 --size 10 --availability-zone us-west-1b
{
    "AvailabilityZone": "us-west-1b",
    "CreateTime": "2023-05-04T09:01:18+00:00",
    "Encrypted": false,
    "Size": 10,
    "SnapshotId": "",
    "State": "creating",
    "VolumeId": "vol-056ddf237f6bfe122",
    "Iops": 100,
    "Tags": [],
    "VolumeType": "gp2",
    "MultiAttachEnabled": false
}

# delete esb volume
aws ec2 delete-volume --volume-id vol-01b4da831ee293eb7
aws ec2 delete-volume --volume-id vol-0cf5cb04ce0b30eee
aws ec2 delete-volume --volume-id vol-056ddf237f6bfe122
```

Now there are three ESB volumes available to be used as PV in GKE.

- Create static persistent pv  

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: tg-pv-storage1
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: "tg-pv"
  awsElasticBlockStore:
    volumeID: vol-01b4da831ee293eb7
    fsType: ext4
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: tg-pv-storage2
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: "tg-pv"
  awsElasticBlockStore:
    volumeID: vol-0cf5cb04ce0b30eee
    fsType: ext4
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: tg-pv-storage3
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: "tg-pv"
  awsElasticBlockStore:
    volumeID: vol-056ddf237f6bfe122
    fsType: ext4
```

- Create TigerGraph cluster with storage class name tg-pv

The ESB volumes are located in zone us-west-1b, configuring the node affinity to ensure the TG pods are scheduled to the nodes of this zone.

creating an affinity configuration file like this:

tg-affinity.yaml

```yaml
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
      - matchExpressions:
        - key: topology.kubernetes.io/zone
          operator: In
          values:
          - us-west-1b
```

```bash
kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --private-key-secret ${YOUR_SSH_KEY_SECRET_NAME} --size 3 --ha 2 --version 3.9.1 --license ${LICENSE} --service-account-name ${SERVICE_ACCOUNT_NAME} \
 --storage-class pv-local --storage-size 10G --cpu 4000m --memory 8Gi --namespace ${YOUR_NAMESPACE} --affinity tg-affinity.yaml
```

- Dynamically persistent volume storage

You can follow these steps to set up and use dynamic persistent volume storage for EKS:

1. Create Storage class
2. Deploy TG with the Storage class name created in step1

Storage class is a simple way of segregating storage options.

To put it simply, a storage class defines what type of storage is to be provisioned.

For example, you can classify our storage class as `gold` and `silver`. These names are arbitrary and use a name that is meaningful to you.

Gold storage class uses the `gp3` persistent disk type for high IOPS applications (to be used with databases). While the silver storage class uses the `gp2` volume type to be used for backups and normal disk operations.

These storage class segregations are completely based on the project requirements.

> [!NOTE]
> There are default storage classes available in EKS which are backed by gp2 (default). If you don’t specify a storage class while provisioning a PV, the default storage class is considered.

- Create a storage class

Save the following manifest as `storage-class.yaml`

```java
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gold
parameters:
  type: gp3
  fsType: ext4 
provisioner: ebs.csi.aws.com
reclaimPolicy: Delete
volumeBindingMode: WaitForFirstConsumer
```

- Create the storage class.

```bash
kubectl apply -f storage-class.yaml
```

## Creating Persistent Volumes using the local file system of the local node

> [!WARNING]
> It doesn’t suggest using the mode for the product system, since it’s only easy to create TigerGraph cluster with a single instance on K8s and also with a single worker node, if not, the data will be lost after rolling update cluster, because the pod may schedule on another worker of K8s.

in addition, if the local filesystem of the node has been deleted after the node restarts, the data will be lost.

### Create a persistent volume with a local filesystem

TigerGraph container will mount data from the path "/home/tigergraph/tigergraph/data", you shouldn’t change it.

You can set the storageClassName to pv-local, if you modify the name, you should use the same when creating TigerGraph cluster

```java
apiVersion: v1
kind: PersistentVolume
metadata:
  name: task-pv-volume-1
  labels:
    type: local
spec:
  storageClassName: pv-local
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: "/home/tigergraph/tigergraph/data"
```

### Create TG cluster with storage class name pv-local

```bash
kubectl tg create --namespace tigergraph --cluster-name local-pv-tg-cluster -k ssh-key-secret --size 1 --ha 1 --version 3.9.1 --image-pull-policy Always --storage-class pv-local --cpu 4000m --memory 8G --storage-size 10G --license ${LICENSE}
```
