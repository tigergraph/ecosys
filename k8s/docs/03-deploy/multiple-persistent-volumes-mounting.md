# Multiple persistent volumes mounting

- [Multiple persistent volumes mounting](#multiple-persistent-volumes-mounting)
  - [Mounting PVs for components reserved for the cluster](#mounting-pvs-for-components-reserved-for-the-cluster)
    - [Mounting a dedicated PV for Kafka](#mounting-a-dedicated-pv-for-kafka)
    - [Mounting a dedicated PV for TigerGraph logs](#mounting-a-dedicated-pv-for-tigergraph-logs)
  - [Mounting PVs for custom containers(Init, sidecar containers, and TigerGraph containers)](#mounting-pvs-for-custom-containersinit-sidecar-containers-and-tigergraph-containers)
    - [Rules for Creating Custom PVs](#rules-for-creating-custom-pvs)
    - [YAML Configuration](#yaml-configuration)
    - [kubectl-tg Operation](#kubectl-tg-operation)
  - [Mounting Existing PVs to Customize Volume Mounts of TigerGraph Containers](#mounting-existing-pvs-to-customize-volume-mounts-of-tigergraph-containers)
    - [Rules for Mounting Existing PVs](#rules-for-mounting-existing-pvs)
    - [YAML Configuration For Mounting Existing PVs](#yaml-configuration-for-mounting-existing-pvs)
    - [kubectl-tg Operation For Mounting Existing PVs](#kubectl-tg-operation-for-mounting-existing-pvs)
  - [Mounting Existing ConfigMap to Customize Volume Mounts of TigerGraph Containers](#mounting-existing-configmap-to-customize-volume-mounts-of-tigergraph-containers)
    - [YAML Configuration For Mounting Existing ConfigMap](#yaml-configuration-for-mounting-existing-configmap)
    - [kubectl-tg Operation For Mounting Existing ConfigMap](#kubectl-tg-operation-for-mounting-existing-configmap)
  - [See also](#see-also)

The TigerGraph Operator supports the mounting of multiple persistent volumes (PVs) for TigerGraph pods, enabling more efficient data storage for different purposes.

Prior to TigerGraph Operator version 0.0.9, all TigerGraph data was stored in a single disk (one PV), limiting scalability and negatively impacting disk performance. To address this, the TigerGraph Operator now supports the mounting of dedicated PVs for Kafka and TigerGraph logs, thereby enhancing overall performance.

Moreover, meeting distinct requirements for mounting an existing PV, as opposed to mounting an exclusive PV for each TigerGraph pod, can be achieved by configuring a custom volume for the TigerGraph pod and custom mount paths for the TigerGraph container.

> [!WARNING]
> Multiple persistent volumes mounting is only supported when creating a TigerGraph cluster using the Operator. If you already have an existing TigerGraph cluster on Kubernetes, you must recreate the cluster to meet this requirement.

## Mounting PVs for components reserved for the cluster

To improve the disk I/O performance of a TigerGraph cluster, dedicated PVs can be mounted for Kafka and TigerGraph logs.

> [!WARNING]
> To utilize a dedicated PV for Kafka in an existing TigerGraph cluster, backup and restore operations must be employed to fulfill this requirement.

### Mounting a dedicated PV for Kafka

- YAML Configuration

To mount a dedicated PV for Kafka, add a new storage named `tg-kafka` under the spec section's `additionalStorages`. The `storageClassName` attribute is optional, providing flexibility in specifying a custom class or using the main storage class.

```YAML
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: pd-standard
      volumeMode: Filesystem
    additionalStorages:
      - name: tg-kafka
        storageClassName: pd-ssd
        storageSize: 10Gi
```

- kubectl-tg Operation

> [!NOTE]
> Other parameters required to create a cluster are omitted here. Please refer to other documents.

```bash
kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --additional-storages additional-storage-kafka.yaml
```

Example additional storage YAML file:

```YAML
additionalStorages:
    - name: tg-kafka
    storageClassName: pd-ssd
    storageSize: 10Gi
```

### Mounting a dedicated PV for TigerGraph logs

- YAML Configuration

To mount a dedicated PV for TigerGraph logs, add a new storage named `tg-log` under spec `additionalStorages`. The `storageClassName` attribute is optional, providing flexibility in specifying a custom class or using the main storage class. The mount path for TigerGraph logs is set to `/home/tigergraph/tigergraph/log`.

```YAML
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: pd-standard
      volumeMode: Filesystem
    additionalStorages:
      - name: tg-log
        storageClassName: pd-standard
        storageSize: 5Gi
```

- kubectl-tg Operation

> [!NOTE]
> Other parameters required to create a cluster are omitted here. Please refer to other documents.

```bash
kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --additional-storages additional-storage-tg-logs.yaml
```

Example additional storage YAML file:

```YAML
additionalStorages:
    - name: tg-log
    storageClassName: pd-standard
    storageSize: 5Gi
```

## Mounting PVs for custom containers(Init, sidecar containers, and TigerGraph containers)

TigerGraph Operator supports the creation and management of PVs for custom containers using the `additionalStorages` specification. This eliminates the need to manually create persistent volumes for Init and sidecar containers.

### Rules for Creating Custom PVs

- Storage Names: Do not use reserved storage names for custom PVs.
  - config-volume
  - private-key-volume
  - tg-data
  - tg-gstore

- Mount Paths: You can mount custom PVs to specific paths in TigerGraph containers, but do not use reserved mount paths.
  - /tmp/init_tg_cfg
  - /etc/private-key-volume
  - /home/tigergraph/tigergraph/data/kafka-mntptr
  - /home/tigergraph/tigergraph/data
  - /home/tigergraph/tigergraph/log
  - /home/tigergraph/tigergraph/data/gstore

### YAML Configuration

The following example demonstrates how to mount a custom PV for a sidecar container (e.g.,tg-sidecar) and a custom PV to the path `/home/tigergraph/backup` of the TigerGraph container.

The mountPath attribute is optional for `additionalStorages`. It will mount the PV to this path if you configure it for the specific additional storage.

```YAML
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:3.9.3
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: xxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 3
  resources:
    requests:
      cpu: "6"
      memory: 12Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: gp2
      volumeMode: Filesystem
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
  initContainers:
    - image: alpine:3.17.2
      name: init-container
      args:
        - /bin/sh
        - -c
        - echo hello
  sidecarContainers:
      - args: # sidecar will execute this 
          - /bin/sh
          - -c
          - |
            while true; do
              echo "$(date) INFO hello from main-container" >> /tg-sidecar/myapp.log ;
              sleep 1;
            done
        image: alpine:3.17.2
        name: sidecar-container # name of sidecar
        readinessProbe: # check if the sidecar is ready
          exec:
            command:
              - sh
              - -c
              - if [[ -f /tg-sidecar/myapp.log ]];then exit 0; else exit 1;fi
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests: # request resouces for sidecar
            cpu: 500m
            memory: 512Mi
          limits: # limit resources
            cpu: 500m
            memory: 512Mi
        env: # inject the environment you need
          - name: CLUSTER_NAME
            value: test-cluster
        volumeMounts:
          - mountPath: /tg-sidecar
            name: tg-sidecar
```

### kubectl-tg Operation

```bash
kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --additional-storages additional-storage-tg-logs.yaml \
--custom-containers custom-containers.yaml
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

Example custom containers YAML file:

```YAML
initContainers:
- image: alpine:3.17.2
    name: init-container
    args:
    - /bin/sh
    - -c
    - echo hello
sidecarContainers:
    - args: # sidecar will execute this 
        - /bin/sh
        - -c
        - |
        while true; do
            echo "$(date) INFO hello from main-container" >> /tg-sidecar/myapp.log ;
            sleep 1;
        done
    image: alpine:3.17.2
    name: sidecar-container # name of sidecar
    readinessProbe: # check if the sidecar is ready
        exec:
        command:
            - sh
            - -c
            - if [[ -f /tg-sidecar/myapp.log ]];then exit 0; else exit 1;fi
        initialDelaySeconds: 10
        periodSeconds: 5
    resources:
        requests: # request resouces for sidecar
        cpu: 500m
        memory: 512Mi
        limits: # limit resources
        cpu: 500m
        memory: 512Mi
    env: # inject the environment you need
        - name: CLUSTER_NAME
        value: test-cluster
    volumeMounts:
        - mountPath: /tg-sidecar
        name: tg-sidecar
```

## Mounting Existing PVs to Customize Volume Mounts of TigerGraph Containers

If you have created a shared PV (e.g., EFS on AWS), you can mount existing PVs to customize volume mounts of TigerGraph containers for data sharing.

### Rules for Mounting Existing PVs

Do not use the following reserved paths when mounting existing PVs:

- /tmp/init_tg_cfg
- /etc/private-key-volume
- /home/tigergraph/tigergraph/data/kafka-mntptr
- /home/tigergraph/tigergraph/data
- /home/tigergraph/tigergraph/log
- /home/tigergraph/tigergraph/data/gstore

### YAML Configuration For Mounting Existing PVs

```YAML
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster2
  namespace: tigergraph
spec:
  ha: 1
  image: docker.io/tigergraph/tigergraph-k8s:3.9.3
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: xxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 1
  resources:
    requests:
      cpu: "6"
      memory: 12Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: efs-sc
      volumeMode: Filesystem
  customVolumes:
    - name: efs-storage
      persistentVolumeClaim:
        claimName: efs-claim
  customVolumeMounts:
    - name: efs-storage
      mountPath: /efs-data
```

### kubectl-tg Operation For Mounting Existing PVs

```bash

kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --custom-volumes custom-volumes.yaml \
--custom-volume-mounts custom-volume-mounts.yaml
```

Example custom volumes YAML file:

```YAML
customVolumes:
- name: efs-storage
    persistentVolumeClaim:
    claimName: efs-claim
```

Example custom volume mounts YAML file:

```YAML
customVolumeMounts:
- name: efs-storage
    mountPath: /efs-data
```

## Mounting Existing ConfigMap to Customize Volume Mounts of TigerGraph Containers

We can also mount a ConfigMap as a custom volume to configure the volume mounts of TigerGraph containers.

> [!IMPORTANT]
> When mounting a ConfigMap as a read-only file system, you must explicitly set the readOnly option to true. Failing to do so will cause the TigerGraph init container to fail when attempting to change file system permissions.

Before creating the TigerGraph cluster, you need to manually create a ConfigMap. Below is an example of a ConfigMap:

```YAML
apiVersion: v1
kind: ConfigMap
metadata:
  name: special-config
data:
  special-config.conf: |
    events {}

    http {
        server {
            listen 8080;
            location / {
                return 200 'Hello from port 8080';
                add_header Content-Type text/plain;
            }
        }

        server {
            listen 8081;
            location / {
                return 200 'Hello from port 8081';
                add_header Content-Type text/plain;
            }
        }
    }
```

### YAML Configuration For Mounting Existing ConfigMap

The example below assumes that you have created a ConfigMap named `special-config`:

```YAML
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster3
  namespace: tigergraph
spec:
  ha: 1
  image: docker.io/tigergraph/tigergraph-k8s:3.9.3
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: xxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 1
  resources:
    requests:
      cpu: "6"
      memory: 12Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
  customVolumes:
    - name: special-config-volume
      configMap:
        name: special-config
  customVolumeMounts:
    - name: special-config-volume
      mountPath: /etc/config/special-config.conf
      subPath: special-config.conf
      readOnly: true
```

### kubectl-tg Operation For Mounting Existing ConfigMap

```bash

kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --custom-volumes custom-volumes.yaml \
--custom-volume-mounts custom-volume-mounts.yaml
```

Example custom volumes YAML file:

```YAML
customVolumes:
  - name: special-config-volume
    configMap:
      name: special-config
```

Example custom volume mounts YAML file:

```YAML
customVolumeMounts:
  - name: special-config-volume
    mountPath: /etc/config/special-config.conf
    subPath: special-config.conf
    readOnly: true
```

## See also

If you are interested in creating static and dynamic persistent volume storage, refer to the following document:

- [How to use static & dynamic persistent volume storage](../07-reference/static-and-dynamic-persistent-volume-storage.md)
