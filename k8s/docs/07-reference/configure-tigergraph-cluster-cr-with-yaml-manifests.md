# How to configure TG Cluster on K8s using TigerGraph CR

This document introduces how to configure the TG cluster using TigerGraph CR. It covers the following content:

- Configure resources

- Configure TigerGraph deployment

- [How to configure TG Cluster on K8s using TigerGraph CR](#how-to-configure-tg-cluster-on-k8s-using-tigergraph-cr)
  - [Configure resources](#configure-resources)
  - [Configure TG deployment](#configure-tg-deployment)
    - [Cluster name](#cluster-name)
    - [TigerGraph cluster version](#tigergraph-cluster-version)
    - [TigerGraph cluster size and HA factor](#tigergraph-cluster-size-and-ha-factor)
    - [TigerGraph Cluster license](#tigergraph-cluster-license)
    - [Service account name of TigerGraph pod(Optional)](#service-account-name-of-tigergraph-podoptional)
    - [Private ssh key name of TigerGraph Cluster](#private-ssh-key-name-of-tigergraph-cluster)
    - [Storage volumes of TigerGraph Cluster](#storage-volumes-of-tigergraph-cluster)
    - [Resource requests and limits of TigerGraph pod](#resource-requests-and-limits-of-tigergraph-pod)
    - [External access service](#external-access-service)
    - [Customized labels and annotations for external service](#customized-labels-and-annotations-for-external-service)
    - [Container Customization of TigerGraph pods](#container-customization-of-tigergraph-pods)
    - [NodeSelector, Affinity, and Toleration configuration](#nodeselector-affinity-and-toleration-configuration)
    - [TigerGraph Configurations](#tigergraph-configurations)
    - [Pod Labels and Annotations](#pod-labels-and-annotations)
    - [Security Context of TigerGraph Containers](#security-context-of-tigergraph-containers)
    - [Lifecycle Hooks of TigerGraph](#lifecycle-hooks-of-tigergraph)
      - [PostInitAction](#postinitaction)
  - [API reference of TigerGraphSpec](#api-reference-of-tigergraphspec)

## Configure resources

Before deploying a TG cluster, it is necessary to be familiar with the hardware and software requirements depending on your needs. For details, refer to [Hardware and Software Requirements](https://docs.tigergraph.com/tigergraph-server/current/installation/hw-and-sw-requirements).

To ensure the proper scheduling and stable operation of the components of the TG cluster on Kubernetes, it is recommended to set Guaranteed-level quality of service (QoS) by making `limits` equal to `requests` when configuring resources. For details, refer to [Configure Quality of Service for Pods](https://kubernetes.io/docs/tasks/configure-pod-container/quality-service-pod/).

## Configure TG deployment

To configure a TG deployment, you need to configure the TigerGraph CR. Refer to the following example.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:3.9.3
  imagePullPolicy: IfNotPresent
  ha: 2
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 6
  resources:
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
```

### Cluster name

The cluster name can be configured by changing `metadata.name` in the `TigerGraph` CR, cluster names should be unique within a certain namespace.

### TigerGraph cluster version

The TigerGraph cluster version can be configured by changing `spec.image` in the `TigerGraph` CR, you can also specify the `imagePullPolicy` and `imagePullSecrets` according to your needs.

Besides, you also need to specify the TG version by changing `spec.initTGConfig.version` which is required when initializing and upgrading cluster.(Before Operator version 0.0.8)

### TigerGraph cluster size and HA factor

The TigerGraph cluster version can be configured by changing `spec.replicas` in the `TigerGraph` CR,

and the HA factor can be configured by changing `spec.ha`.

### TigerGraph Cluster license

The TigerGraph cluster license is required for TigerGraph deployment, and it can be configured by changing `spec.license` in the TigerGraph CR.

### Service account name of TigerGraph pod(Optional)

A service account name of TigerGraph pod is required to acquire permission for some special K8s distribution, such as OpenShift.

You can create a service account name and grant permission to it first, and it can be configured by changing `spec.serviceAccountName` in the `TigerGraph` CR.

It’s an optional configuration, you can omit it if there are no permission issues.

### Private ssh key name of TigerGraph Cluster

The field `privateKeyName` is a mandatory configuration for Operator 0.0.4 and later.

The private ssh key pair is required for security when running TigerGraph on K8s, you can create a private ssh key pair, and then create a Secret with these ssh key files.

```bash
# create a new private keys
echo -e 'y\\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''

# Create a Secret of K8s with above ssh key files
kubectl create secret generic ssh-key-secret --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace YOUR_NAME_SPACE
```

Then you can specify the value of `spec.privateKeyName` to the secret name you created above.

### Storage volumes of TigerGraph Cluster

Storage volumes configurations can be configured by changing `spec.storage` , there are two types of storage, `persistent-claim` and `ephemeral`. For production, you should use the `persistent-claim` type to store the data on persistent volumes.

Moreover, since Operator 0.1.0, you can mount multiple PVs for TigerGraph pods.

- persistent-claim

```yaml
spec:
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 10G
      storageClassName: standard
```

- ephemeral

```yaml
spec: 
  storage:
    type: ephemeral
```

- Mounting a dedicated PV for Kafka and TigerGraph logs

```yaml
spec: 
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
      - name: tg-log
        storageClassName: pd-standard
        storageSize: 5Gi
```

- Mounting PVs for custom containers(Init, sidecar containers, and TigerGraph containers)

```yaml
spec:
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

- Mounting Existing PVs to Customize Volume Mounts of TigerGraph Containers

```YAML
spec:
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

Details on how to mount multiple PVs for TigerGraph Pods, see [Multiple persistent volumes mounting](../03-deploy/multiple-persistent-volumes-mounting.md)

### Resource requests and limits of TigerGraph pod

The Resource requests and limits of TG Cluster pod can be configured by changing `spec.resources.requests` and `spec.resources.limits` in the `TigerGraph` CR.

```yaml
spec:
  resources:
    limits:
      cpu: 8
      memory: 100Gi
    requests:
      cpu: 8
      memory: 100Gi
```

### External access service

TigerGraph Operator provides three types of external access services, LoadBalancer, NodePort, and Ingress. It can be configured by changing `spec.listener.type` in the `TigerGraph` CR.

- LoadBalancer

```yaml
spec:
  listener:
    type: LoadBalancer
```

- NodePort

```yaml
spec:
  listener:
    type: NodePort
    nginxNodePort: 30240
```

- Ingress

```yaml
spec:
  listener:
    ingressClassName: INGRESS_CLASS_NAME
    type: Ingress
    nginxHost: tigergraph-api.k8s.company.com
    secretName: k8s.company.com
```

### Customized labels and annotations for external service

If you want to add customized labels and annotations for external service, you can configure it by adding `spec.listener.labels` and `spec.listener.annotations` in `TigerGraph` CR.

```yaml
spec:
  listener:
    type: LoadBalancer
  labels:
    label-key: label-value
  annotations:
    annotation-key: annotation-value
```

### Container Customization of TigerGraph pods

TigerGraph CR support customizing the containers of TG pods, including the Init container, Sidecar container, and container volumes. To know more about this feature, you can refer to [InitContainers,SidecarContainers and CustomVolumes](../03-deploy/custom-containers.md)

The init container can be configured by changing `spec.initContainers`, you can add multiple init containers through this configuration field. About the fields of Container, you can refer to K8S Container API [https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#Container](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#Container)

```yaml
spec:
  initContainers:
    - args:
        - /bin/sh
        - -c
        - echo "this is init-container test"
      image: alpine:3.17.3
      name: init-container-test
      securityContext:
        capabilities:
          add:
            - NET_ADMIN
        privileged: true
```

Sidecar containers can be configured by changing `spec.sidecarContainers`, you can add multiple sidecar containers through this configuration field.

```yaml
spec:
  sidecarContainers:
    - args: # sidecar will execute this 
        - /bin/sh
        - -c
        - |
          while true; do
            echo "$(date) INFO hello from main-container" >> /var/log/myapp.log ;
            sleep 1;
          done
      image: alpine:3.17.2
      name: main-container # name of sidecar
      readinessProbe: # check if the sidecar is ready
        exec:
          command:
            - sh
            - -c
            - if [[ -f /var/log/myapp.log ]];then exit 0; else exit 1;fi
        initialDelaySeconds: 10
        periodSeconds: 5
      resources:
        requests: # request resouces for sidecar
          cpu: 2
          memory: 1Gi
        limits: # limit resources
          cpu: 4
          memory: 4Gi
      env: # inject the environment you need
        - name: CLUSTER_NAME
          value: test-cluster
      volumeMounts:
        - mountPath: /var/log
          name: tg-log # this volume is used by TG, you can access log of tg here
```

Additional volumes can be configured by changing `spec.customVolumes` . If you need to mount extra volumes into the init container or sidecar container, you can update this configuration.

The Operator has created two volumes by default, one is tg-data which is used to persistent data of TG cluster, another volume name is tg-log which is used to save logs of TG, and the mount path is `/home/tigergraph/tigergraph/log` , you can use volume name `tg-log` and mount path `/home/tigergraph/tigergraph/log` in the sidecar to access the logs of TG.

For detailed configurations of different volumes, refer to [https://kubernetes.io/docs/concepts/storage/volumes](https://kubernetes.io/docs/concepts/storage/volumes) .

```yaml
spec:
  customVolumes:
    - name: auth-sidecar-config
      configMap:
        name: auth-sidecar-configmap
    - name: credentials 
      emptyDir:
        medium: Memory
    - name: fallback-config
        configMap:
        name: fallback
        optional: true
    - name: heap-dump
      hostPath:
        path: /var/tmp/heapdump
        type: DirectoryOrCreate
```

### NodeSelector, Affinity, and Toleration configuration

NodeSelector, Affinity, and Toleration can be configured by changing `spec.affinityConfiguration`, the special cases for both of these configurations, you can refer to page [NodeSelector, Affinity and Toleration using cases](../03-deploy/affinity-use-cases.md).

- NodeSelector

```yaml
spec:
  affinityConfiguration:
    nodeSelector:
      disktype: ssd
```

- Toleration

```yaml
spec:
  affinityConfiguration:
    tolerations:
    - key: "userGroup"
      operator: "Equal"
      value: "enterprise"
      effect: "NoExecute"
```

- Affinity

```yaml
spec:
  affinityConfiguration:
    affinity:
      nodeAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          nodeSelectorTerms:
          - matchExpressions:
            - key: disktype
              operator: In
              values:
              - ssd
            podAntiAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchExpressions:
            - key: tigergraph.com/cluster-pod
              operator: In
              values:
                - test-cluster
          topologyKey: topology.kubernetes.io/zone
```

### TigerGraph Configurations

TigerGraph configurations can be adjusted by modifying `spec.tigergraphConfig` in the `TigerGraph` Custom Resource (CR). These configurations will be applied to the TigerGraph (TG) cluster during initialization. If the cluster is already running, you can update the TG cluster configurations by altering `.spec.tigergraphConfig`. This will trigger the creation of a config-update job to apply the new configurations to the TG cluster.

> [!NOTE]
> The configurations you provide must be compatible with the `gadmin config set` command. All values should be of string type, enclosed in double quotes ("").

```yaml
spec:
  tigergraphConfig:
  # Values must be strings, enclosed in double quotes ("")
    GSQL.UDF.Policy.Enable: "false"
    Controller.BasicConfig.LogConfig.LogFileMaxDurationDay: "40"
```

### Pod Labels and Annotations

You can customize the labels and annotations of the TigerGraph pods by modifying `spec.podLabels` and `spec.podAnnotations` in the `TigerGraph` CR. These labels and annotations will be applied to all TigerGraph pods.

```yaml
spec:
  podLabels:
    key.tg.com: value
  podAnnotations:
    key.tg.com: value
```

### Security Context of TigerGraph Containers

You can customize the security context of the TigerGraph containers by modifying `spec.securityContext` in the `TigerGraph` CR. These security contexts will be applied to all TigerGraph containers. For more information about security context, refer to [Set the security context for a Container](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-container).

```yaml
spec:
  securityContext:
    capabilities:
      add:
        - SYS_PTRACE
        - SYSLOG
        - SYS_ADMIN
```

### Lifecycle Hooks of TigerGraph

You can customize the lifecycle hooks of TigerGraph by modifying `spec.lifecycle` in the `TigerGraph` CR.

#### PostInitAction

You can specify a bash script in `spec.lifecycle.postInitAction`, and the script will be put into the init-job and be executed in the first TigerGraph pod(whose suffix is `-0`) after the TigerGraph system is initialized. For example:

```yaml
spec:
  lifecycle:
    postInitAction: |
      echo "This is a post init action" >> /tmp/post-init-action.log
```

For more information about lifecycle hooks, refer to [Configure Lifecycle Hooks in TigerGraph CR](../03-deploy/lifecycle-of-tigergraph.md).

## API reference of TigerGraphSpec

TigerGraphSpec contains the details of TigerGraph members

| Field | Description |
|----------|----------|
| replicas | The desired TG cluster size |
| image | The desired TG docker image |
| imagePullPolicy | (*Optional*)The image pull policy of TG docker image, default is IfNotPresent |
| imagePullSecrets | (*Optional*)The own keys can access the private registry |
| serviceAccountName | (*Optional*)The service account name of pod which is used to acquire special permission |
| privateKeyName | The secret name of private ssh key files |
| ha | The replication factor of TG cluster |
| license | The license of TG cluster |
| listener.type | The type of external access service, which can be set to LoadBalancer, NodePort, and Ingress |
| listener.nginxNodePort | The nginx service port which is required when setting listener.type to NodePort |
| listener.ingressClassName | (Optional)The ingress class name of nginx service which can be set optionally when setting listener.type to Ingress |
| listener.nginxHost | The domain name of nginx service which is required when setting listener.type to Ingress |
| listener.secretName | (*Optional*)The secretName is the name of the secret used to terminate TLS traffic on port 443 when setting listener.type to Ingress |
| listener.labels | (*Optional*)The customized labels will be added to external service |
| listener.annotations | (*Optional*)The customized annotations will be added to external service |
| resources | [The compute resource requirements](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.22/#resourcerequirements-v1-core) |
| storage | The persistent volumes for TigerGraph pods |
| storage.type | The type of persistent volume, which can be set to ephemeral or persistent-claim|
| storage.volumeClaimTemplate | The persistent volume claim template for TigerGraph main storage. [PersistentVolumeClaimSpec](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#persistentvolumeclaimspec-v1-core)|
| storage.additionalStorages | (*Optional*)Additional storages for TigerGraph pods, it's an array list of StorageVolume|
| `StorageVolume`.name | Additional storage name|
| `StorageVolume`.storageClassName | (*Optional*)The `StorageClassName` of an additional storage|
| `StorageVolume`.storageSize | The storage size of an additional storage|
| `StorageVolume`.mountPath | (*Optional*)The mount path of TigerGraph container for an additional storage|
| `StorageVolume`.accessMode | (*Optional*)The access mode of an additional storage, which can be set to ReadWriteOnce, ReadOnlyMany, ReadWriteMany, or ReadWriteOncePod|
| `StorageVolume`.volumeMode | (*Optional*) The volume mode of an additional storage, which can be set to Filesystem or Block|
| customVolumeMounts | (*Optional*)The custom [volume mount](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#persistentvolumeclaimspec-v1-core:~:text=error%20was%20encountered.-,VolumeMount%20v1%20core,-Group) of TigerGraph container.|
| initContainers | The [init containers](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#Container) run in TigerGraph pods. |
| sidecarContainers | (*Optional*)The [sidecar containers](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#Container) run in TG pods |
| customVolumes | (*Optional*)The custom [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) used in init container and sidecar container |
| affinityConfiguration | (*Optional*)The configurations for NodeSelector, Affinity, and Tolerations |
| affinityConfiguration.nodeSelector | (*Optional*)The configuration of assigning pods to special nodes using [NodeSelector](https://kubernetes.io/docs/tasks/configure-pod-container/assign-pods-nodes/) |
| affinityConfiguration.tolerations | (*Optional*)The [tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/) configuration of TigerGraph pod |
| affinityConfiguration.affinity | (*Optional*)The [affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#inter-pod-affinity-and-anti-affinity) configuration of TigerGraph pod |
| podLabels | (*Optional*)The customized labels will be added to TigerGraph pods |
| podAnnotations | (*Optional*)The customized annotations will be added to TigerGraph pods |
| securityContext | (*Optional*)The [security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/) of TigerGraph containers |
| lifecycle.postInitAction | (*Optional*)The bash script will be executed in the first TigerGraph pod whose prefix is `-0` after the TigerGraph system is initialized |
