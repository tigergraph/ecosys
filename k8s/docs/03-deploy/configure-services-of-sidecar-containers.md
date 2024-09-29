# Configure Services of Sidecar Containers

Starting with Operator version 1.2.0, the TigerGraph operator supports creating services for sidecar containers during cluster deployment. This guide explains how to configure sidecar listeners to expose sidecar container services outside the Kubernetes cluster.

- [Configure Services of Sidecar Containers](#configure-services-of-sidecar-containers)
  - [Configure sidecar listener in TigerGraph CR](#configure-sidecar-listener-in-tigergraph-cr)
    - [Configure NodePort type service](#configure-nodeport-type-service)
    - [Configure LoadBalancer type service](#configure-loadbalancer-type-service)
    - [Configure Ingress type service](#configure-ingress-type-service)
    - [TigerGraph CR YAML full example](#tigergraph-cr-yaml-full-example)
  - [Configure sidecar listener by kubectl-tg](#configure-sidecar-listener-by-kubectl-tg)
    - [Configure sidecar listener when creating cluster](#configure-sidecar-listener-when-creating-cluster)
    - [Update sidecar listener using kubectl-tg](#update-sidecar-listener-using-kubectl-tg)
  - [Access the sidecar services outside Kubernetes cluster](#access-the-sidecar-services-outside-kubernetes-cluster)
    - [Accessing NodePort and LoadBalancer type of sidecar service](#accessing-nodeport-and-loadbalancer-type-of-sidecar-service)
    - [Accessing Ingress type of sidecar service](#accessing-ingress-type-of-sidecar-service)

## Configure sidecar listener in TigerGraph CR

Suppose you configure a sidecar container in the TigerGraph CR that exposes port 80 for its HTTP service. The example TigerGraph CR YAML file is shown below:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-tg-cluster
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: ${YOUR_LISCENSE}
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4
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
  sidecarContainers:
    - image: httpd:2.4
      name: httpd80
      resources:
        requests:
          cpu: 500m
          memory: 512Mi
        limits:
          cpu: 500m
          memory: 512Mi
```

You can configure the services for the sidecar container using `spec.sidecarListener`. It supports three types of services in Kubernetes: NodePort, LoadBalancer, and Ingress.

> [!IMPORTANT]
> TigerGraph pod is considered ready when all of its containers are ready, so readiness probe and liveness Probe should be configured for the sidecar container.

> [!IMPORTANT]
> Each container within the pod shares the same networking namespace. The listener ports of the sidecar container should not conflict with the TG container.

### Configure NodePort type service

The example sidecarListener configuration is below:

```yaml
  sidecarListener:
    type: NodePort
    labels:
      key1: value1
      key3: value3
    annotations:
      key2: value2
      key4: value4
    listenerPorts:
    - name: httpd80
      port: 80
      nodePort: 30080
```

You can customize labels and annotations for the NodePort service using the fields `spec.sidecarListener.labels` and `spec.sidecarListener.annotations`. Additionally, you can specify the listener ports of the sidecar container: the `port` field represents the listener port of the sidecar container, while `nodePort` is the port exposed by the Kubernetes service.

The `listenerPorts` field is an array type that allows you to configure listener ports for multiple sidecar containers in the TigerGraph Pod. Each listenerPort name should be unique if you configure multiple.

See the full [example](../09-samples/deploy/service-of-sidecar-nodeport-type.yaml).

### Configure LoadBalancer type service

The example sidecarListener configuration is below:

```yaml
  sidecarListener:
    type: LoadBalancer
    labels:
      key1: value1
    annotations:
      key2: value2
    listenerPorts:
    - name: httpd80
      port: 80
      targetPort: 80
```

For a LoadBalancer type of service, you only need to specify the listener port of the sidecar container. The configuration for labels and annotations is the same as for NodePort type services. See the full [example](../09-samples/deploy/service-of-sidecar-loadbanalance-type.yaml).

> [!NOTE]
> Beginning with TigerGraph Operator 1.3.0, both the LoadBalancer's exposed port and the internal port used by sidecar containers to access pods targeted by the service are fully customizable. Users can adjust these ports as needed to meet their specific requirements.

### Configure Ingress type service

The example sidecarListener configuration is below:

```yaml
  sidecarListener:
    type: Ingress
    ingressClassName: ${YourIngressClassName}
    labels:
      key1: value1
    annotations:
      key2: value2
    listenerPorts:
    - name: httpd80
      port: 80
      ingressRule:
        host: your.domain.hostname
        path: /
        pathType: Prefix
```

For an Ingress type of service, you can customize the Ingress class name using the `ingressClassName` field and configure the Ingress rules using the `ingressRule` field. See the full [example](../09-samples/deploy/service-of-sidecar-ingress-type.yaml).

### TigerGraph CR YAML full example

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: ${YOUR_LISCENSE}
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4
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
  sidecarListener:
    type: LoadBalancer
    labels:
      key1: value1
    annotations:
      key2: value2
    listenerPorts:
    - name: httpd80
      port: 80
  sidecarContainers:
    - image: httpd:2.4
      name: httpd80
      resources:
        requests: 
          cpu: 500m
          memory: 512Mi
        limits: 
          cpu: 500m
          memory: 512Mi
```

## Configure sidecar listener by kubectl-tg

### Configure sidecar listener when creating cluster

To make use of this feature, follow these steps:

Prepare a YAML file that includes the definitions for your `sidecarListener` and `sidecarContainers`. This YAML file will be passed to the `--sidecar-listener` and `--custom-containers` option.

Below is an illustrative example of a custom container and sidecar listener YAML file:

- Sidecar listener

Here, we use the LoadBalancer service type as an example.

sidecar-listener.yaml

```yaml
sidecarListener:
  type: LoadBalancer
  labels:
      key1: value1
  annotations:
      key2: value2
  listenerPorts:
  - name: httpd80
      port: 80
```

- Custom containers

custom-containers.yaml

```yaml
sidecarContainers:
  - image: httpd:2.4
  name: httpd80
  resources:
      requests:
      cpu: 500m
      memory: 512Mi
      limits:
      cpu: 500m
      memory: 512Mi
```

Create a cluster with sidecar service using kubectl-tg

```bash
kubectl tg create --cluster-name test-tg-cluster --license $LICENSE \
-k ${YOUR_SSH_KEY_SECRET} --size 4 --ha 2 \
--sidecar-listener sidecar-listener.yaml \
--custom-containers sidecar-containers.yaml \
--storage-class standard --storage-size 100G --cpu 6000m --memory 12Gi -n ${YOUR_NAMESPACE}
```

### Update sidecar listener using kubectl-tg

You can update the sidecar listener after creating a cluster, the example command is below:

```bash
kubectl tg update --cluster-name test-tg-cluster --sidecar-listener sidecar-listener.yaml -n ${YOUR_NAMESPACE}
```

You can also remove the sidecar listener by providing an empty YAML file to `--sidecar-listener` option.

## Access the sidecar services outside Kubernetes cluster

The operator will automatically create a service for the sidecar container according to the cluster CR configuration. The service names of the sidecars follow the rules below:

For service types LoadBalancer and NodePort, only one Kubernetes service resource will be created. The service name will be as `${CLUSTER-NAME}-sidecar-service`.

For service type Ingress, it needs another Kubernetes service  as its backend service, so the Kubernetes service resource name and ingress resource name would be as follows:

`${CLUSTER-NAME}-sidecar-ingress`

`${CLUSTER-NAME}-sidecar-ingress-backend`

### Accessing NodePort and LoadBalancer type of sidecar service

```bash
export SIDECAR_SERVICE_ADDRESS=$(kubectl get svc/${CLUSTER-NAME}-sidecar-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].hostname}')

curl http://${SIDECAR_SERVICE_ADDRESS}:${LISTENER_PORT}
```

### Accessing Ingress type of sidecar service

```bash
kubectl describe ingress ${CLUSTER-NAME}-sidecar-ingress --namespace ${YOUR_NAMESPACE}
```

You can access the sidecar service using the `ingressRule.host` configuration of the Ingress resource.
