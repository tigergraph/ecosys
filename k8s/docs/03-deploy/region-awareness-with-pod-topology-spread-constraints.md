# Enable Region Awareness with Pod Topology Spread Constraints

Starting with **TigerGraph Operator version 1.2.0** and **TigerGraph 4.1.0**, TigerGraph Operator supports configuring the [Topology Spread Constraints](https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/) of TigerGraph Pods. More importantly, you can create a Multi-Availability Zone cluster with region awareness to achieve high availability and better resource utilization.

- [Enable Region Awareness with Pod Topology Spread Constraints](#enable-region-awareness-with-pod-topology-spread-constraints)
  - [Topology Spread Constraints for TigerGraph Pods](#topology-spread-constraints-for-tigergraph-pods)
    - [Configure Topology Spread Constraints for TigerGraph Pods by TigerGraph CR](#configure-topology-spread-constraints-for-tigergraph-pods-by-tigergraph-cr)
    - [Configure Topology Spread Constraints for TigerGraph Pods by kubectl-tg](#configure-topology-spread-constraints-for-tigergraph-pods-by-kubectl-tg)
  - [Region Awareness of TigerGraph](#region-awareness-of-tigergraph)
    - [The webhook check of TigerGraph CR for region awareness](#the-webhook-check-of-tigergraph-cr-for-region-awareness)
    - [Configure Region Awareness of TigerGraph by TigerGraph CR](#configure-region-awareness-of-tigergraph-by-tigergraph-cr)
    - [Configure Region Awareness of TigerGraph by kubectl-tg](#configure-region-awareness-of-tigergraph-by-kubectl-tg)
  - [Use cases](#use-cases)
    - [Enable region awareness with one topology spread constraint](#enable-region-awareness-with-one-topology-spread-constraint)
    - [Enable region awareness with multiple topology spread constraints](#enable-region-awareness-with-multiple-topology-spread-constraints)
    - [Enable region awareness with topology spread constraint and node affinity](#enable-region-awareness-with-topology-spread-constraint-and-node-affinity)
    - [Apply topology spread constraint between multiple TigerGraph clusters](#apply-topology-spread-constraint-between-multiple-tigergraph-clusters)
  - [Troubleshooting](#troubleshooting)
    - [How to continue cluster installation when enabling region awareness fails](#how-to-continue-cluster-installation-when-enabling-region-awareness-fails)
      - [HA is less than 2](#ha-is-less-than-2)
      - [Minimum 3 regions are needed to handle region awareness failures](#minimum-3-regions-are-needed-to-handle-region-awareness-failures)
    - [How to continue cluster expansion, shrinking and HA update when enabling region awareness fails](#how-to-continue-cluster-expansion-shrinking-and-ha-update-when-enabling-region-awareness-fails)
    - [The special failure cases when enable region awareness during cluster shrinking](#the-special-failure-cases-when-enable-region-awareness-during-cluster-shrinking)
    - [Enabling region awareness for auto-scaling Kubernetes clusters is not supported](#enabling-region-awareness-for-auto-scaling-kubernetes-clusters-is-not-supported)
    - [How to Handle the failure of an entire zone](#how-to-handle-the-failure-of-an-entire-zone)
  - [See also](#see-also)

## Topology Spread Constraints for TigerGraph Pods

Since TigerGraph Operator 0.0.6, it has supported Kubernetes features such as Pod affinity and anti-affinity, which allow some control over Pod placement in different topologies. However, these features only address specific use cases:

- Placing all Pods into a single topology without limits.
- Preventing two Pods from co-locating in the same topology.

Between these two extreme cases, there is a common need to distribute the Pods evenly across the topologies to achieve better cluster utilization and high availability of applications. This can be achieved by configuring topology spread constraints to control how Pods are distributed across your cluster among failure domains such as regions, zones, nodes, and other user-defined topology domains.

> [!IMPORTANT]
> To utilize the Kubernetes feature Pod Topology Spread Constraints, ensure that your cluster is running Kubernetes version 1.27 or higher, as the matchLabelKeys field, introduced as a beta feature, is enabled by default starting from version 1.27.

### Configure Topology Spread Constraints for TigerGraph Pods by TigerGraph CR

The TigerGraph Cluster CRD includes a field, spec.topologySpreadConstraints. The usage of this field looks like the following:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-tg-cluster
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
  regionAware:
    enable: true
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: test-tg-cluster
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: test-tg-cluster
        matchLabelKeys:
          - pod-template-hash
```

You can read more about this field by running kubectl explain Pod.spec.topologySpreadConstraints or refer to the [scheduling](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#scheduling) section of the API reference for Pod.

Most of the configurations are universally applicable, for TigerGraph on Kubernetes, you must have to specify the correct pod labels to `spec.topologySpreadConstraints.labelSelector.matchLabels`, the label for TigerGraph cluster Pods is `tigergraph.com/cluster-pod: ${YOUR_CLUSTER_NAME}`.

You can also customize the TigerGraph cluster pod labels by field `spec.podLabels` of TigerGraph Cluster CRD, there is another example for this case:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-tg-cluster
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
  podLabels:
    company.domain.com/tg-cluster-pod: tg-cluster-pod-labels
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            company.domain.com/tg-cluster-pod: tg-cluster-pod-labels
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            company.domain.com/tg-cluster-pod: tg-cluster-pod-labels
        matchLabelKeys:
          - pod-template-hash
```

> [!IMPORTANT]
> The `whenUnsatisfiable` field indicates how to deal with a Pod if it doesn't satisfy the spread constraint; DoNotSchedule (default) tells the scheduler not to schedule it, ScheduleAnyway tells the scheduler to still schedule it while prioritizing nodes that minimize the skew. So you should configure the field value according to your requirements.

### Configure Topology Spread Constraints for TigerGraph Pods by kubectl-tg

Prepare a YAML file that includes the definitions for your `topologySpreadConstraints`. This YAML file will be passed to the `--topology-spread-constraints` option.

Below is an illustrative example of a topology spread constraints YAML file:

topology-spread-constraints.yaml

```yaml
topologySpreadConstraints:
    - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
    labelSelector:
        matchLabels:
        tigergraph.com/cluster-pod: test-tg-cluster
    matchLabelKeys:
        - pod-template-hash
    - maxSkew: 1
    topologyKey: "kubernetes.io/hostname"
    whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
    labelSelector:
        matchLabels:
        tigergraph.com/cluster-pod: test-tg-cluster
    matchLabelKeys:
        - pod-template-hash
```

Create a cluster with topology spread constraints using kubectl-tg

```bash
kubectl tg create --cluster-name test-tg-cluster --license $LICENSE \
-k ${YOUR_SSH_KEY_SECRET} --size 4 --ha 2 \
--topology-spread-constraints topology-spread-constraints.yaml \
--storage-class standard --storage-size 100G --cpu 6000m --memory 12Gi -n ${YOUR_NAMESPACE}
```

Update a cluster with topology spread constraints using kubectl-tg

```bash
kubectl tg update --cluster-name test-tg-cluster --topology-spread-constraints topology-spread-constraints.yaml  -n ${YOUR_NAMESPACE}
```

If the YAML file is empty, it will remove the topology spread constraints configurations.

## Region Awareness of TigerGraph

Configure topology spread constraints for TigerGraph Pods can ensure distribute the Pods evenly across the topology domains,

but the high availability of TigerGraph services on Kubernetes relies on the region-aware replica placement feature of TigerGraph.

So we need to configure region awareness of TigerGraph with topology spread constraints to achieve high availability.

> [!NOTE]
> For TigerGraph Operator version 1.2.0, you can enable or disable region-ware replica placement of TigerGraph during cluster initialization, expansion, shrinking, and HA update.

### The webhook check of TigerGraph CR for region awareness

The TigerGraph cluster CR must meet the following requirements to enable or disable region awareness when creating or updating cluster, otherwise, the webhook check will reject the request with the specific errors.

- Enabling region awareness only allows to be enabled or disabled during cluster initialization, expansion, shrinking, and HA update.
- Configure `topologySpreadConstraints` is required for enabling regionAware for TigerGraph on K8s, so it will check if topologySpreadConstraints has been configured when regionAware is enabled.
- It does not allow updating `topologySpreadConstraints` if the TigerGraph cluster has been enabled with region awareness.
- Region awareness can only be enabled when the TigerGraph Docker image version is 4.1.0 or higher.

### Configure Region Awareness of TigerGraph by TigerGraph CR

The TigerGraph Cluster CRD includes a field spec.regionAware, which is used to enable or disable region-aware replica placement. The usage of this field is shown below:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: tg-test-cluster
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
  regionAware:
    enable: true
    topologyKey: topology.kubernetes.io/zone
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #DoNotSchedule
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: tg-test-cluster
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule #DoNotSchedule
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: test-cluster
        matchLabelKeys:
          - pod-template-hash
```

> [!IMPORTANT]
> To enable region-aware replica placement for TigerGraph, the cluster replication (HA) must be greater than or equal to 2, and there must be greater than or equal to 3 available topology domains. If these conditions are not met, the pre-check for enabling region-aware replica placement will fail. Additionally, the maxSkew of topology spread constraints should be set to 1 to comply with the region awareness requirements.

In the example above, the field `spec.regionAware.enable` is used to enable or disable region-aware replica placement. The `spec.regionAware.topologyKey` field is optional and defaults to `topology.kubernetes.io/zone`. You can customize this field if you have other user-defined topology domains, but its value must match the topology spread constraint configuration.

> [!IMPORTANT]
> You must specify the correct pod labels in `spec.topologySpreadConstraints.labelSelector.matchLabels`. The label for TigerGraph cluster pods is `tigergraph.com/cluster-pod: ${YOUR_CLUSTER_NAME}`. You can also customize the TigerGraph cluster pod labels using the `spec.podLabels` field of the TigerGraph Cluster CRD.

- **matchLabelKeys** is a list of pod label keys to select the pods over which spreading will be calculated. The keys are used to lookup values from the pod labels, those key-value labels are ANDed with labelSelector to select the group of existing pods over which spreading will be calculated for the incoming pod. The same key is forbidden to exist in both matchLabelKeys and labelSelector. matchLabelKeys cannot be set when labelSelector isn't set. Keys that don't exist in the pod labels will be ignored. A null or empty list means only match against the labelSelector.

With matchLabelKeys, you don't need to update the pod.spec between different revisions. The controller/operator just needs to set different values to the same label key for different revisions. The scheduler will assume the values automatically based on matchLabelKeys. For example, if you are configuring a Deployment, you can use the label keyed with pod-template-hash, which is added automatically by the Deployment controller, to distinguish between different revisions in a single Deployment.

> [!NOTE]
> The matchLabelKeys field is a beta-level field and enabled by default in Kubernetes 1.27.

### Configure Region Awareness of TigerGraph by kubectl-tg

You can configure region awareness by using the `kubectl tg create` command when you create a cluster, and you can also update them by `kubectl tg update`.

```bash
--enable-region-awareness :
                    set true to enable region awareness for TigerGraph cluster, default as false
--region-awareness-topology-key :
                    set the topology key for region awareness, default as topology.kubernetes.io/zone
                    set it to null if you want to remove this configuration
```

The topology spread constraints is required to be configured when enabling region awareness of TigerGraph cluster.

topology-spread-constraints.yaml

```yaml
topologySpreadConstraints:
    - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
    labelSelector:
        matchLabels:
        tigergraph.com/cluster-pod: test-tg-cluster
    matchLabelKeys:
        - pod-template-hash
    - maxSkew: 1
    topologyKey: "kubernetes.io/hostname"
    whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
    labelSelector:
        matchLabels:
        tigergraph.com/cluster-pod: test-tg-cluster
    matchLabelKeys:
        - pod-template-hash
```

```bash
kubectl tg create --cluster-name tg-test-cluster --enable-region-awareness true \
 --region-awareness-topology-key topology.kubernetes.io/zone \
 --topology-spread-constraints topology-spread-constraints.yaml --namespace tigergraph
```

You can also update region awareness by:

- Disable region awareness

```bash
kubectl tg update --cluster-name tg-test-cluster --enable-region-awareness false --namespace tigergraph
```

- Enable or disable region awareness during cluster expansion, shrinking and HA update

```bash
kubectl tg update --cluster-name tg-test-cluster --size ${CLUSTER_SIZE} --ha ${CLUSTER_HA} --enable-region-awareness false --namespace tigergraph
```

## Use cases

### Enable region awareness with one topology spread constraint

If you want an incoming TigerGraph Pod to be evenly spread with existing Pods across zones, and enable region awareness of TigerGraph, you can use a manifest similar to:

YAML [sample](../09-samples/deploy/region-awareness-with-topology-spread-constraint.yaml) example:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: tg-test-cluster
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
  regionAware:
    enable: true
    topologyKey: topology.kubernetes.io/zone
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: tg-test-cluster
        matchLabelKeys:
          - pod-template-hash
```

It is important to note that if you want to enable region awareness for TigerGraph, you must follow these rules:

- The TigerGraph replication(HA) should be greater than or equal to 2.
- The topology domains specified by `topologyKey` should be greater than or equal to 3.
- The `maxSkew` field should be set to 1.
- The `labelSelector.matchLabels` field must be one of the TigerGraph cluster Pod labels, pre-define label(`tigergraph.com/cluster-pod: ${CLUSTER_NAME}`) of TigerGraph Operator or customized labels.
- The `whenUnsatisfiable` field should be set to `DoNotSchedule`
- Add `pod-template-hash` to the `matchLabelKeys` field

You can ignore the above limitations if you don't enable region awareness for TigerGraph.

### Enable region awareness with multiple topology spread constraints

You can also configure multiple topology spread constraints when enabling region awareness, the below example shows how to combine two topology spread constraints to control the spread of Pods both by node and by zone.

YAML [sample](../09-samples/deploy/region-awareness-with-multiple-topology-spread-constraints.yaml) example:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-tg-cluster
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
  regionAware:
    enable: true
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: test-tg-cluster
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: test-tg-cluster
        matchLabelKeys:
          - pod-template-hash
```

> [!IMPORTANT]
> Ensure that the Kubernetes cluster has sufficient nodes in the specified zones or regions; otherwise, the TigerGraph pods will remain in a pending status due to the lack of available nodes.

### Enable region awareness with topology spread constraint and node affinity

In a large Kubernetes cluster, there may be many zones. You can combine topology spread constraints and node affinity to specify the zones or nodes where pods will be scheduled.

YAML [sample](../09-samples/deploy/region-awareness-with-topology-spread-constraint-and-node-affinity.yaml) example:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-tg-cluster
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
  regionAware:
    enable: true
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: test-tg-cluster
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: test-tg-cluster
        matchLabelKeys:
          - pod-template-hash
  affinityConfiguration:
    affinity:
      nodeAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
          nodeSelectorTerms:
          - matchExpressions:
            - key: topology.kubernetes.io/zone
              operator: In
              values:
              - us-east-2a
              - us-east-2b
              - us-east-2c
```

> [!IMPORTANT]
> Ensure that the Kubernetes cluster has sufficient nodes in the specified zones or regions; otherwise, the TigerGraph pods will remain in a pending status due to the lack of available nodes.

For detailed information about how to configure NodeSelector, Affinity and Toleration in TigerGraph CR, please refer to [NodeSelector, Affinity and Toleration Use Cases](../03-deploy/affinity-use-cases.md)

### Apply topology spread constraint between multiple TigerGraph clusters

There may be scenarios where you want to deploy multiple TigerGraph clusters within a Kubernetes cluster. The following example demonstrates how to use topology spread constraints to manage the distribution of TigerGraph Pods across these clusters.

The key configuration is to add a custom pod label to the TigerGraph clusters and use this label as the value for the `labelSelector.matchLabels` field. Enabling region awareness for a TigerGraph cluster is optional.

YAML [sample](../09-samples/deploy/apply-topology-spread-constraint-between-multiple-tigergraph-clusters.yaml) example:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-tg-cluster1
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
  podLabels:
    tigergraph.com/across-cluster-pod: across-tg-cluster
  regionAware:
    enable: true
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/across-cluster-pod: across-tg-cluster
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/across-cluster-pod: across-tg-cluster
        matchLabelKeys:
          - pod-template-hash
---
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-tg-cluster2
  namespace: tigergraph
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.1.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${YOUR_LICENSE}
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
  podLabels:
    tigergraph.com/across-cluster-pod: across-tg-cluster
  regionAware:
    enable: true
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/across-cluster-pod: across-tg-cluster
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule #ScheduleAnyway
        labelSelector:
          matchLabels:
            tigergraph.com/across-cluster-pod: across-tg-cluster
        matchLabelKeys:
          - pod-template-hash
```

## Troubleshooting

### How to continue cluster installation when enabling region awareness fails

Enable region awareness for TigerGraph, you must follow the following rules:

- The TigerGraph replication(HA) should be greater than or equal to 2.
- The topology domains specified by `topologyKey` should be greater than or equal to 3.
- The `maxSkew` field should be set to 1.
- The `labelSelector.matchLabels` field must be one of the TigerGraph cluster Pod labels, pre-define label(`tigergraph.com/cluster-pod: ${CLUSTER_NAME}`) of TigerGraph Operator or customized labels.
- The `whenUnsatisfiable` field should be set to `DoNotSchedule`
- Add `pod-template-hash` to the `matchLabelKeys` field

You may encounter a region awareness check failure if the TigerGraph cluster CR does not meet the rules mentioned above. There are four cluster statuses that define region awareness check failures during cluster installation, expansion, shrinking, and HA updates:

| TigerGraph Cluster Status | Description  |
|----------|----------|
| InitializeRegionAwarePre, Unknown| The TigerGraph Operator is trying to perform the region uniform check during cluster initialization|
| InitializeRegionAwarePre, False| The TigerGraph Operator failed to perform the region uniform check during cluster initialization|
| InitializeRegionAwarePre, True| The TigerGraph Operator expanded to perform the region uniform check successfully during cluster initialization|
| ExpandRegionAwarePre, Unknown | The TigerGraph Operator is trying to perform the region uniform check during cluster expansion|
| ExpandRegionAwarePre, False | The TigerGraph Operator failed to perform the region uniform check during cluster expansion|
| ExpandRegionAwarePre, True | The TigerGraph Operator expanded to perform the region uniform check successfully during cluster expansion|
| ShrinkRegionAwarePre, Unknown | The TigerGraph Operator is trying to perform the region uniform check during cluster shrinking|
| ShrinkRegionAwarePre, False | The TigerGraph Operator failed to perform the region uniform check during cluster shrinking|
| ShrinkRegionAwarePre, True | The TigerGraph Operator expanded to perform the region uniform check successfully during cluster shrinking|
| HAUpdateRegionAwarePre, Unknown | The TigerGraph Operator is trying to perform the region uniform check during cluster HA update|
| HAUpdateRegionAwarePre, False | The TigerGraph Operator failed to perform the region uniform check during cluster HA update|
| HAUpdateRegionAwarePre, True | The TigerGraph Operator expanded to perform the region uniform check successfully during cluster HA update|

#### HA is less than 2

If the cluster HA is less than 2, it will fail and transition the cluster status to InitializeRegionAwarePre, False. You can track the cluster status by:

```bash
$ kubectl get tg test-cluster  -n tigergraph -w
NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                                             SERVICE-TYPE   REGION-AWARENESS   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                                      InitializeRoll   Unknown            2s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                                      InitializeRoll   True               29s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                            InitializeRegionAwarePre   Unknown            29s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                            InitializeRegionAwarePre   False              29s
```

You can also check the root cause by running the following command:

```bash
$ kubectl get tg test-cluster -n tigergraph -o yaml |yq .status

conditions:
  - lastTransitionTime: "2024-06-20T11:17:08Z"
    message: |
      Failed to check region awareness: HostList is [{"Hostname":"test-cluster-0.test-cluster-internal-service","ID":"m1","Region":"us-central1-f"},{"Hostname":"test-cluster-1.test-cluster-internal-service","ID":"m2","Region":"us-central1-b"},{"Hostname":"test-cluster-2.test-cluster-internal-service","ID":"m3","Region":"us-central1-c"},{"Hostname":"test-cluster-3.test-cluster-internal-service","ID":"m4","Region":"us-central1-f"}], and HA is 1, stderr: [Warning] InternalError (License could not be retrieved since .tg.cfg is missing)
      [  Error] ParameterErr (To enable region awareness, then HA should be minimum 2)
    reason: ClusterInitializeRegionAwarePreFalse
    status: "False"
    type: InitializeRegionAwarePre
image: docker.io/tigergraph/tigergraph-k8s:4.1.0
listener:
  type: LoadBalancer
replicas: 4
storage:
  tgDataSize: 10G
topologySpreadConstraints:
  - labelSelector:
      matchLabels:
        tigergraph.com/cluster-pod: test-cluster
    matchLabelKeys:
      - pod-template-hash
    maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
  - labelSelector:
      matchLabels:
        tigergraph.com/cluster-pod: test-cluster
    matchLabelKeys:
      - pod-template-hash
    maxSkew: 1
    topologyKey: kubernetes.io/hostname
    whenUnsatisfiable: ScheduleAnyway
```

Then we can continue the cluster installation by updating cluster HA to 2:

```bash
$ kubectl get tg test-cluster  -n tigergraph -w

NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   REGION-AWARENESS   CONDITION-TYPE            CONDITION-STATUS   AGE
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRoll             Unknown            2s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRoll             True               29s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   Unknown            29s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              29s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              30s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              32s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              35s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              45s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              47s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              48s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              86s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              7m39s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   True               7m39s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializePost             Unknown            7m40s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializePost             True               9m30s
test-cluster   4          4              2            docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer   true               Normal                     True               9m30s
```

#### Minimum 3 regions are needed to handle region awareness failures

If the number of unique regions of TigerGraph Pods is less than 3, it will also fail and transition the cluster status to InitializeRegionAwarePre, False. You can track the cluster status by:

```bash
$ kubectl get tg test-cluster  -n tigergraph -w

NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   REGION-AWARENESS   CONDITION-TYPE             CONDITION-STATUS   AGE
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRoll             Unknown            9s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRoll             True               2m7s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   Unknown            2m7s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              2m7s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              2m8s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              2m9s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              2m10s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              2m11s
test-cluster   4                                      docker.io/tigergraph/tigergraph-k8s:4.1.0   LoadBalancer                      InitializeRegionAwarePre   False              2m21s
```

You can also check the root cause by running the following command:

```bash
$ kubectl get tg test-cluster -n tigergraph -o yaml |yq .status
conditions:
  - lastTransitionTime: "2024-06-20T12:27:54Z"
    message: |
      Failed to check region awareness: HostList is [{"Hostname":"test-cluster-0.test-cluster-internal-service","ID":"m1","Region":"us-east-2b"},{"Hostname":"test-cluster-1.test-cluster-internal-service","ID":"m2","Region":"us-east-2a"},{"Hostname":"test-cluster-2.test-cluster-internal-service","ID":"m3","Region":"us-east-2b"},{"Hostname":"test-cluster-3.test-cluster-internal-service","ID":"m4","Region":"us-east-2a"}], and HA is 2, stderr: [Warning] InternalError (License could not be retrieved since .tg.cfg is missing)
      [  Error] ParameterErr (config sanity check failed)
      , stdout: Configuration sanity check failed:
      0 ParameterErr (Minimum 3 regions are needed to handle region failures)
    reason: ClusterInitializeRegionAwarePreFalse
    status: "False"
    type: InitializeRegionAwarePre
image: docker.io/tigergraph/tigergraph-k8s:4.1.0
listener:
  type: LoadBalancer
replicas: 4
storage:
  tgDataSize: 10G
topologySpreadConstraints:
  - labelSelector:
      matchLabels:
        tigergraph.com/cluster-pod: test-cluster
    matchLabelKeys:
      - pod-template-hash
    maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
  - labelSelector:
      matchLabels:
        tigergraph.com/cluster-pod: test-cluster
    matchLabelKeys:
      - pod-template-hash
    maxSkew: 1
    topologyKey: kubernetes.io/hostname
    whenUnsatisfiable: ScheduleAnyway
```

In case of failure, if the number of regions reaches three or more, the installation process may still not proceed. TigerGraph cluster pods will not be rescheduled even if you update the `topologySpreadConstraints` or `nodeAffinity`. Therefore, you must delete the TigerGraph CR and recreate it with the correct configuration.

### How to continue cluster expansion, shrinking and HA update when enabling region awareness fails

There may be a scenario where you have a running cluster without region awareness and you want to enable it through expansion. The TigerGraph Operator already supports this case if the `topologySpreadConstraints` configuration has been set up correctly. Otherwise, you need to follow the steps below:

- Scale up cluster without region awareness to ensure the cluster HA is more than 2 if necessary.
- Pause the cluster first, update the TigerGraph cluster CR with correct `topologySpreadConstraints` configuration for region awareness before resuming cluster without region awareness. For the details of pausing and resuming the cluster, please refer to [Pause and Resume TigerGraph cluster](../04-manage/pause-and-resume.md)
- After resuming the cluster, you can scale the cluster with region awareness.

> [!NOTE]
> If the above steps don't resolve the issue, you can back up and restore the cluster with the correct Topology Spread Constraints without region awareness first. Then, enable region awareness with cluster expansion, shrinking, or an HA update.
> If you encounter failures while enabling region awareness, you can always recover the cluster by disabling region awareness.

### The special failure cases when enable region awareness during cluster shrinking

There's no guarantee that the constraints will remain satisfied when pods are removed. Scaling down a region-aware TigerGraph cluster may result in an imbalanced pod distribution, causing it to fail the region uniform check during shrinking. In such situations, users can disable region awareness to continue with the cluster shrinking. Additionally, it's best practice to back up the cluster before shrinking it.

### Enabling region awareness for auto-scaling Kubernetes clusters is not supported

The Kubernetes scheduler does not have prior knowledge of all zones or other topology domains that a cluster might have. These are determined based on existing nodes in the cluster. This lack of knowledge can be problematic in auto-scaled clusters, especially when a node pool (or node group) is scaled down to zero nodes. In such cases, the cluster won't consider those topology domains until there is at least one node in them, which may affect scaling operations.

### How to Handle the failure of an entire zone

Kubernetes topology spread constraints do not support the redistribution of pods when zones are added or removed. Additionally, TigerGraph service distribution supports only cluster initialization, expansion, shrinking, and HA updates. TigerGraph is a stateful application that uses Persistent Volumes (PV) to persist data. Once the TigerGraph cluster is created, the zone of the PV is fixed, preventing pod rescheduling between different zones after cluster creation.

For recovery in case of failure, there are two methods:

1. The failed pods should become ready again once the specific zone recovers.
2. If the pods do not recover, the user needs to use backup and restore to clone a new cluster.

## See also

- [NodeSelector, Affinity and Toleration Use Cases](../03-deploy/affinity-use-cases.md)
- [Pause and Resume TigerGraph cluster](../04-manage/pause-and-resume.md)
