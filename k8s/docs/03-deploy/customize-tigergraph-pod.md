# Customize TigerGraph Pods and TigerGraph Containers

When you create a TigerGraph cluster, TigerGraph pods will be created in the Kubernetes cluster, TigerGraph system will run in TigerGraph containers. You may want to customize the pods or containers to meet your needs, for example, to add more customized labels and annotations, or to change the security context of the containers.

We have exposed some configurations for you to customize the pods and containers. You can customize them by modifying TigerGraph CR file or by using the `kubectl tg` command.

- [Customize TigerGraph Pods and TigerGraph Containers](#customize-tigergraph-pods-and-tigergraph-containers)
  - [Customize TigerGraph Pods/TigerGraph Containers by kubectl tg](#customize-tigergraph-podstigergraph-containers-by-kubectl-tg)
    - [Customize Labels and Annotations of TigerGraph Pods](#customize-labels-and-annotations-of-tigergraph-pods)
    - [Customize pod labels and annotations in the PodTemplateSpec of the StatefulSet managing TigerGraph pods](#customize-pod-labels-and-annotations-in-the-podtemplatespec-of-the-statefulset-managing-tigergraph-pods)
    - [Customize Security Context of TigerGraph Containers](#customize-security-context-of-tigergraph-containers)
  - [Customize TigerGraph Pods/TigerGraph Containers by TigerGraph CR](#customize-tigergraph-podstigergraph-containers-by-tigergraph-cr)
    - [Customize Labels and Annotations of TigerGraph Pods in TigerGraph CR](#customize-labels-and-annotations-of-tigergraph-pods-in-tigergraph-cr)
    - [Customize pod labels and annotations in the PodTemplateSpec of the StatefulSet managing TigerGraph pods in TigerGraph CR](#customize-pod-labels-and-annotations-in-the-podtemplatespec-of-the-statefulset-managing-tigergraph-pods-in-tigergraph-cr)
    - [Customize Security Context of TigerGraph Containers in TigerGraph CR](#customize-security-context-of-tigergraph-containers-in-tigergraph-cr)

## Customize TigerGraph Pods/TigerGraph Containers by kubectl tg

You can customize the pods by using the `kubectl tg create` command when you create the cluster, and you can also update them by `kubectl tg update`.

### Customize Labels and Annotations of TigerGraph Pods

> [!IMPORTANT]
> To prevent pods from undergoing a rolling update when adding or updating labels and annotations, these actions are performed during pod creation.
> As a result, the labels and annotations will not be included in the PodTemplateSpec of the StatefulSet managing TigerGraph pods.
> If you wish to apply these labels and annotations during pod creations, you can use the options `--pod-init-labels` and `--pod-init-annotations`.
> For more details, please refer to the following section.

```bash
  --pod-labels :      add some customized labels to all pods, your input should be like like 'k1=v1,k2="v2 with space"'
  --pod-annotations : add some customized annotations to all pods, your input should be like like 'k1=v1,k2="v2 with space"'
```

You can specify the labels and annotations of the pods by using the `--pod-labels` and `--pod-annotations` options. The input should be like `k1=v1,k2=v2`. For example:

```bash
kubectl tg create --cluster-name test-cluster --namespace tigergraph \
  --pod-labels "app=tg,env=prod" --pod-annotations "app=tg,env=prod" ${OTHER_OPTIONS}
```

You can also update them by:

```bash
    kubectl tg update --cluster-name test-cluster --namespace tigergraph \
  --pod-labels "app=tg,env=test" --pod-annotations "app=tg,env=test" ${OTHER_OPTIONS}
```

### Customize pod labels and annotations in the PodTemplateSpec of the StatefulSet managing TigerGraph pods

> [!NOTE]
> This feature is supported starting from TigerGraph Operator 1.4.0.

For certain use cases, such as injecting Istio sidecars into TigerGraph pods, you may need to add or update labels and annotations during pod creation. To customize these labels and annotations, use the following options:

```bash
  --pod-init-labels : add customized labels to the pod template before creating them, your input should be like like 'k1=v1,k2="v2 with space"'
  --pod-init-annotations : add customized annotations to the pod template before creating them, your input should be like like 'k1=v1,k2="v2 with space"'
```

You can specify the labels and annotations of the pods by using the `--pod-init-labels` and `--pod-init-annotations` options. The input should be like `k1=v1,k2=v2`. For example:

```bash
kubectl tg create --cluster-name test-cluster --namespace tigergraph \
  --pod-init-labels "app=tg,env=prod" --pod-init-annotations "app=tg,env=prod" ${OTHER_OPTIONS}
```

You can also update them by:

```bash
    kubectl tg update --cluster-name test-cluster --namespace tigergraph \
  --pod-init-labels "app=tg,env=test" --pod-init-annotations "app=tg,env=test" ${OTHER_OPTIONS}
```

### Customize Security Context of TigerGraph Containers

```bash
  --security-context : give a YAML file to specify SecurityContext for tigergraph container
```

Since the SecurityContext is a little complicated, you should know the details of it before you use it. You can refer to the [Set the security context for a Container](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-container) for more information.

Use a file to set the SecurityContext for TigerGraph containers. The file should be like:

```yaml
securityContext:
  capabilities:
    add:
      - SYS_PTRACE
      - SYSLOG
      - SYS_ADMIN
```

The above file will add the capabilities to the containers. Name the file as `security-context.yaml`, and then you can create a cluster with this security context configuration by:

```bash
kubectl tg create --cluster-name test-cluster --namespace tigergraph \
  --security-context security-context.yaml ${OTHER_OPTIONS}
```

You can also set `privileged: true` in the file to make the containers privilegedm, which means all capabilities will be added to the containers. For example:

```yaml
securityContext:
  privileged: true
```

Name the file as `security-context-privileged.yaml`, and then you can update the cluster with this security context configuration by:

```bash
kubectl tg update --cluster-name test-cluster --namespace tigergraph \
  --security-context security-context-privileged.yaml ${OTHER_OPTIONS}
```

> [!WARNING]
> We always use `runAsUser: 1000` and `runAsGroup: 1000` in the containers, and you are not allowed to change them. If you specify `runAsUser` or `runAsGroup` in the file, the update or creation will fail.

## Customize TigerGraph Pods/TigerGraph Containers by TigerGraph CR

### Customize Labels and Annotations of TigerGraph Pods in TigerGraph CR

> [!IMPORTANT]
> To prevent pods from undergoing a rolling update when adding or updating labels and annotations, these actions are performed during pod creation.
> As a result, the labels and annotations will not be included in the PodTemplateSpec of the StatefulSet managing TigerGraph pods.
> If you wish to apply these labels and annotations during pod creations, you can specify the field `spec.podInitLabels` and `spec.PodInitAnnotations`.
> For more details, please refer to the following section.

You can add labels to field `spec.podLabels` and annotations to field `spec.PodAnnotations` in the TigerGraph CR file. Operator will inject these labels and annotations to all tigergraph pods.
For example:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:3.10.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  license: YOUR_LICENSE
  replicas: 3
  ha: 1
  resources:
    requests:
      cpu: 4
      memory: 8Gi
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
  podLabels:
    key.tg.com: value
  podAnnotations:
    key.tg.com: value
```

### Customize pod labels and annotations in the PodTemplateSpec of the StatefulSet managing TigerGraph pods in TigerGraph CR

> [!NOTE]
> This feature is supported starting from TigerGraph Operator 1.4.0.

For certain use cases, such as injecting Istio sidecars into TigerGraph pods, you may need to add or update labels and annotations during pod creation. To customize these labels and annotations, you can add labels to the spec.podInitLabels field and annotations to the spec.podInitAnnotations field in the TigerGraph CR file. The operator will then inject these labels and annotations into all TigerGraph pods during pod creation.
For example:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.1.2
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  license: YOUR_LICENSE
  replicas: 4
  ha: 2
  resources:
    requests:
      cpu: 4
      memory: 8Gi
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
  podInitLabels:
    sidecar.istio.io/inject: "true"
  podInitAnnotations:
    traffic.sidecar.istio.io/excludeOutboundPorts: "9188,9178"
    traffic.sidecar.istio.io/excludeInboundPorts: "9178,9188"
```

### Customize Security Context of TigerGraph Containers in TigerGraph CR

Since the SecurityContext is a little complicated, you should know the details of it before you use it. You can refer to the [Set the security context for a Container](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-container) for more information.

You can add security context to field `spec.securityContext` in the TigerGraph CR file. Operator will configure this security context to all tigergraph containers.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:3.10.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  license: YOUR_LICENSE
  replicas: 3
  ha: 1
  resources:
    requests:
      cpu: 4
      memory: 8Gi
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
    securityContext:
      capabilities:
        add:
          - SYS_PTRACE
          - SYSLOG
          - SYS_ADMIN
```

> [!WARNING]
> We always use `runAsUser: 1000` and `runAsGroup: 1000` in the containers, and you are not allowed to change them. If you specify `runAsUser` or `runAsGroup` in the file, the webhook will reject the request.
