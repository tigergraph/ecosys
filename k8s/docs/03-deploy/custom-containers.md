# InitContainers,SidecarContainers and CustomVolumes

- [InitContainers,SidecarContainers and CustomVolumes](#initcontainerssidecarcontainers-and-customvolumes)
  - [Basic knowledge](#basic-knowledge)
  - [Sidecar Containers](#sidecar-containers)
  - [Init Containers](#init-containers)
  - [Custom Volumes](#custom-volumes)
  - [Combining sidecarContainers, initContainers, and customVolumes](#combining-sidecarcontainers-initcontainers-and-customvolumes)
  - [What's Next](#whats-next)

## Basic knowledge

A K8s Pod has the capability to house multiple containers, including both init containers and app containers. Upon pod creation, the init containers execute sequentially in a designated order. Should any of the init containers encounter a failure, the overall pod execution is halted (for more insights, consult [Init Containers](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/)). Following the successful completion of all init containers, the app containers proceed to run concurrently.

By default, in the configuration of the TigerGraph CR, each TigerGraph Pod features a singular app container named "tigergraph". This container runs all TigerGraph services within the Pod. The functionality "InitContainers,SidecarContainers and CustomVolumes" empowers users to seamlessly integrate personalized initContainers and sidecarContainers into TigerGraph Pods. Furthermore, users can create customVolumes, enabling the mounting of these volumes within their initContainers or sidecarContainers.

> [!NOTE]
> You can utilize this feature by adding configurations in a YAML file or through `kubectl-tg`. This document exclusively focuses on the usage within YAML files. If you're interested in learning how to use it with `kubectl-tg`, please consult the guide on [Utilizing InitContainers, Sidecar Containers, and Custom Volumes with kubectl-tg](./use-custom-containers-by-kubectl-tg.md).

## Sidecar Containers

A sidecar container functions similarly to the app container named "tigergraph". In cases where the sidecar container requires readiness and liveness checks configuration, it is crucial to ensure that these checks do not interfere with the rolling update process of TigerGraph (TG) pods. Simultaneously, adopting the practice of setting resource limits for each sidecar container within the TG pod is recommended to prevent the excessive use of Kubernetes node resources.

To integrate sidecarContainers into TigerGraph Pods,  write the configurations in `.spec.sidecarContainers`. For detailed guidance on setting up sidecarContainers, consult the [K8S Containers](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#Container):

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  ha: 1
  license: YOUR_LICENSE_HERE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 1
  resources:
    requests:
      cpu: "8"
      memory: 16Gi
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
    # securityContext: # configure securityContext here
    #   privileged: true
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
```

## Init Containers

To incorporate custom initContainers into TigerGraph Pods,  place the configuration details within `.spec.initContainers` field. For detailed instructions on setting up initContainers, you can refer to the [K8S Containers API](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#Container). Your personalized initContainers will execute once the TG initContainer  finishes its tasks.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  ha: 1
  license: YOUR_LICENSE_HERE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 1
  resources:
    requests:
      cpu: "8"
      memory: 16Gi
  initContainers:
    - image: alpine:3.17.2
      name: init-hello
      args:
        - /bin/sh
        - -c
        - echo hello
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
```

## Custom Volumes

Incorporating initContainers and sidecarContainers with customVolumes facilitates seamless data exchange. For defining customVolumes, direct your configurations to the `.spec.customVolumes` field. To understand the essential fields of customVolumes, consult the  [Kubernetes Volumes documentation](https://kubernetes.io/docs/concepts/storage/volumes/)

By default, the Operator establishes two volumes: `tg-data` for persistent TG cluster data and `tg-log` for TG logs storage. In your sidecar containers, you can mount volume named `tg-log` to access TG logs effectively or mount `tg-data` to access TG data.

## Combining sidecarContainers, initContainers, and customVolumes

The following example demonstrates the integration of sidecarContainers and initContainers while facilitating data exchange through customVolumes. Init containers create a file in the `credentials` volume, which the sidecar named `main-container` subsequently utilizes for readiness checks. The sidecar named `main-container` also outputs to the file `/var/log/myapp.log`, accessible by the `sidecar-container` due to their common customVolume named `log`.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  ha: 1
  license: YOUR_LICENSE_HERE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 1
  resources:
    requests:
      cpu: "8"
      memory: 16Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
  initContainers:
    - image: alpine:3.17.2
      name: init-credential
      args:
        - /bin/sh
        - -c
        - echo CREDENTIAL > /credentials/auth_file
      volumeMounts:
      - name: credentials
        mountPath: /credentials
      
  sidecarContainers:
    - image: alpine:3.17.2
      name: main-container
      args:
        - /bin/sh
        - -c
        - while true; do echo "$(date) INFO hello from main-container" >> /var/log/myapp.log ;sleep 1;done
      volumeMounts:
      - name: credentials
        mountPath: /credentials
      - name: log
        mountPath: /var/log
      readinessProbe:
        exec:
          command:
          - sh
          - -c
          - if [[ -f /credentials/auth_file ]];then exit 0; else exit 1;fi
        initialDelaySeconds: 10
        periodSeconds: 5
    - name: sidecar-container
      image: alpine:3.17.2
      args:
        - /bin/sh
        - -c
        - tail -fn+1 /var/log/myapp.log
      volumeMounts:
      - name: log
        mountPath: /var/log
  customVolumes:
    - name: log
      emptyDir: {}
    - name: credentials
      emptyDir: {}
```

## What's Next

- Learn [how to integrate envoy sidecar containers with TG Pods](../07-reference/integrate-envoy-sidecar.md)
