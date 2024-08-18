# NodeSelector, Affinity and Toleration Use Cases

- [NodeSelector, Affinity and Toleration Use Cases](#nodeselector-affinity-and-toleration-use-cases)
  - [Basic Knowledge](#basic-knowledge)
    - [Which labels are TG using](#which-labels-are-tg-using)
      - [TigerGraph Cluster Pods](#tigergraph-cluster-pods)
    - [TigerGraph Job Pods](#tigergraph-job-pods)
      - [TigerGraph Backup/Restore Job Pods](#tigergraph-backuprestore-job-pods)
  - [NodeSelector](#nodeselector)
    - [Example: schedule pods to nodes with disktype=ssd](#example-schedule-pods-to-nodes-with-disktypessd)
  - [Affinity](#affinity)
    - [NodeAffinity](#nodeaffinity)
      - [Preferred Node Affinity](#preferred-node-affinity)
        - [Example: Difference between Preferred Affinity and Required Affinity](#example-difference-between-preferred-affinity-and-required-affinity)
      - [Weighted Affinity and Logical Operators](#weighted-affinity-and-logical-operators)
      - [Combining Rules with Logical Operators](#combining-rules-with-logical-operators)
        - [Examples: Combining Multiple Rules with Different Weights](#examples-combining-multiple-rules-with-different-weights)
    - [Inter-pod Affinity and Anti-Affinity](#inter-pod-affinity-and-anti-affinity)
      - [Example: Avoiding Scheduling TigerGraph Pods on the Same VM Instance](#example-avoiding-scheduling-tigergraph-pods-on-the-same-vm-instance)
      - [Scheduling Pods to Different Zones](#scheduling-pods-to-different-zones)
  - [Toleration](#toleration)
    - [Example: Implementing User Groups with Taints and Tolerations](#example-implementing-user-groups-with-taints-and-tolerations)
  - [Notice](#notice)

## Basic Knowledge

In a Kubernetes cluster, every node is equipped with labels that provide information about the node's attributes and capabilities. Some labels are automatically assigned by Kubernetes itself, while others can be added manually by administrators. These labels play a crucial role in workload distribution, resource allocation, and overall cluster management.(please refer to [Well-Known Labels, Annotations and Taints](https://kubernetes.io/docs/reference/labels-annotations-taints/) ). You also have the ability to manually assign labels to nodes in your Kubernetes cluster.

To view all labels associated with nodes, you can use the following command:

```bash
kubectl get nodes --show-labels
```

Here's an example of node labels in a Google Kubernetes Engine (GKE) cluster:

```bash
NAME                                             STATUS   ROLES    AGE     VERSION            LABELS
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   Ready    <none>   7m57s   v1.24.9-gke.3200   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/instance-type=e2-standard-8,beta.kubernetes.io/os=linux,cloud.google.com/gke-boot-disk=pd-balanced,cloud.google.com/gke-container-runtime=containerd,cloud.google.com/gke-cpu-scaling-level=8,cloud.google.com/gke-logging-variant=DEFAULT,cloud.google.com/gke-max-pods-per-node=110,cloud.google.com/gke-nodepool=default-pool,cloud.google.com/gke-os-distribution=cos,cloud.google.com/machine-family=e2,cloud.google.com/private-node=false,failure-domain.beta.kubernetes.io/region=us-central1,failure-domain.beta.kubernetes.io/zone=us-central1-a,kubernetes.io/arch=amd64,kubernetes.io/hostname=gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g,kubernetes.io/os=linux,node.kubernetes.io/instance-type=e2-standard-8,topology.gke.io/zone=us-central1-a,topology.kubernetes.io/region=us-central1,topology.kubernetes.io/zone=us-central1-a
...
```

To manually assign labels to nodes, you can use the kubectl label command. For example, to assign a label named environment with the value production to nodes NODE_1 and NODE_2, you would use the following command:

```bash
kubectl label nodes NODE_1 NODE_2  LABEL_KEY=LABEL_VALUE
```

These labels can then be utilized in affinity rules and other scheduling configurations to ensure that pods are placed on the most suitable nodes based on your specific requirements.

### Which labels are TG using

TigerGraph utilizes specific labels for different purposes in Kubernetes:

#### TigerGraph Cluster Pods

| Label                                  | Usage                                                               |
|----------------------------------------|---------------------------------------------------------------------|
| `tigergraph.com/cluster-name=CLUSTER_NAME` | Indicates which cluster the pod belongs to.                       |
| `tigergraph.com/cluster-pod=CLUSTER_NAME`  | Indicates that the pod belongs to a cluster and not a Job.        |
| `tigergraph.com/gui-service=true`         | Labeled on pods running the GUI service.                          |
| `tigergraph.com/restpp-service=true`      | Labeled on pods running the RESTPP service.                       |

### TigerGraph Job Pods

| Label                                           | Usage                                                                        |
|-------------------------------------------------|------------------------------------------------------------------------------|
| `tigergraph.com/cluster-name=CLUSTER_NAME`      | Indicates which cluster the job is for.                                     |
| `tigergraph.com/cluster-job={CLUSTER_NAME}-{JOB_TYPE}-job` | Specifies the type of job and the cluster it's associated with (JOB_TYPE: init, upgrade, expand, shrink-pre, shrink-post). |

#### TigerGraph Backup/Restore Job Pods

| Label                                            | Usage                                                                        |
|--------------------------------------------------|------------------------------------------------------------------------------|
| `tigergraph.com/backup-cluster=CLUSTER_NAME`     | Labeled on pods running backup jobs for the specified cluster.              |
| `tigergraph.com/restore-cluster=CLUSTER_NAME`    | Labeled on pods running restore jobs for the specified cluster.             |

These labels help identify the purpose and affiliation of various pods within the Kubernetes environment, making it easier to manage and monitor different components of TigerGraph clusters, jobs, backups, and restores.

## NodeSelector

NodeSelector in the TigerGraph Custom Resource (CR) allows you to control the scheduling of pods for the TigerGraph cluster. When you define a NodeSelector, the pods related to the TigerGraph cluster will only be scheduled on nodes that have specific labels matching the NodeSelector criteria. This feature ensures that the TigerGraph cluster pods are placed on nodes that meet your specified requirements.(to know more about NodeSelector: [Assign Pods to Nodes](https://kubernetes.io/docs/tasks/configure-pod-container/assign-pods-nodes/) )

It's important to note that NodeSelector only applies to pods directly associated with the TigerGraph cluster. Other pods running tasks such as init, upgrade, expand, or shrink jobs will not be influenced by the NodeSelector settings.

### Example: schedule pods to nodes with disktype=ssd

In this example, we will demonstrate how to use the NodeSelector feature to schedule pods to nodes with a specific label, such as disktype=ssd. This example assumes you are using Google Kubernetes Engine (GKE).

Use `kubectl get nodes` to list all nodes:

```bash
> kubectl get nodes

NAME                                             STATUS   ROLES    AGE   VERSION
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   Ready    <none>   10m   v1.24.9-gke.3200
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q   Ready    <none>   10m   v1.24.9-gke.3200
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-9t5m   Ready    <none>   10m   v1.24.9-gke.3200
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   Ready    <none>   10m   v1.24.9-gke.3200
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-lff2   Ready    <none>   10m   v1.24.9-gke.3200
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-wh8g   Ready    <none>   10m   v1.24.9-gke.3200
```

Add label `disktype=ssd` to 3 of the 6 nodes:

```bash
kubectl label nodes gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g \
  gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q  \
  gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-9t5m \
  disktype=ssd
```

Replace the node names with the actual names of the nodes you want to label as SSD.

First, we try to create a TG cluster without any rules. Use following CR:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 2
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
```

Apply the configuration using `kubectl apply -f <filename>.yaml`.

Use `kubectl describe pod` to see which node each pod is scheduled to

```bash
test-cluster-0:
Node:         gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l/10.128.0.68

test-cluster-1:
Node:         gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-wh8g/10.128.0.67

test-cluster-2 :
Node:         gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q/10.128.0.73
```

Note that the pods are scheduled to three random nodes.

Then we create a cluster with NodeSelector:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-nodeselector
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 2
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G

  ha: 1
  license: YOUR_LICENSE
  affinityConfiguration:
    nodeSelector:
      disktype: ssd
```

Apply the configuration using `kubectl apply -f <filename>.yaml`.

In this configuration, there is an additional field `.spec.affinityConfiguration`, which is used to define NodeSelector.

```yaml
  affinityConfiguration:
    nodeSelector:
      disktype: ssd
```

That means the pods can only be scheduled to nodes with label `disktype=ssd`.

We can use `kubectl describe pod` to see which node they are scheduled to:

```bash
test-nodeselector-0: 
Node: gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q/10.128.0.73

test-nodeselector-1: 
Node:  gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g/10.128.0.90

test-nodeselector-2: 
Node:  gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q/10.128.0.73
```

Both `gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q` and `gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g` possess the specified label.

## Affinity

Please note that affinity settings exclusively impact the pods within the TigerGraph cluster. Any other pods executing init/upgrade/expand/shrink tasks will remain unaffected by these affinity configurations.

### NodeAffinity

Additionally, TigerGraph pods can be strategically allocated to nodes with specific labels through the use of NodeAffinity. To gain a deeper understanding of Node Affinity, you can refer to the official Kubernetes documentation: [Assign Pods to Nodes using Node Affinity](https://kubernetes.io/docs/tasks/configure-pod-container/assign-pods-nodes-using-node-affinity/)

Here is an illustrative example of a CR (Custom Resource) configuration implementing NodeAffinity:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-nodeaffinity
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 2
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
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
```

In this example, the nodeAffinity section is utilized within the affinityConfiguration to specify that the pods require nodes with the label disktype=ssd during scheduling, while allowing execution to continue even if the affinity is disregarded.

Certainly, let's take a closer look at the `.spec.affinityConfiguration` section:

```yaml
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
```

Within the affinityConfiguration, the setting `requiredDuringSchedulingIgnoredDuringExecution` is employed. This signifies that it is mandatory for our pods to be scheduled exclusively on nodes possessing the specified label, ensuring a precise node placement throughout both the scheduling and execution phases.

You can use the following command to observe the nodes to which the pods are scheduled:

```bash
> kubectl get pods --output=wide
NAME                                                     READY   STATUS    RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-nodeaffinity-0                                      0/1     Running     0          17s   10.36.5.8   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   <none>           <none>
test-nodeaffinity-1                                      0/1     Running     0          17s   10.36.3.8   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q   <none>           <none>
test-nodeaffinity-2                                      0/1     Running     0          17s   10.36.5.9   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g 
```

Notice that both gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g and gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q nodes possess the specified label, indicating the successful enforcement of node affinity.

#### Preferred Node Affinity

For a deeper understanding of preferred node affinity, you can explore the document: [Schedule a Pod using preferred node affinity](https://kubernetes.io/docs/tasks/configure-pod-container/assign-pods-nodes-using-node-affinity/#schedule-a-pod-using-preferred-node-affinity).

It's crucial to differentiate between the `preferredDuringSchedulingIgnoredDuringExecution` and `requiredDuringSchedulingIgnoredDuringExecution` fields. When utilizing `requiredDuringSchedulingIgnoredDuringExecution`, pods will remain **unscheduled** if an insufficient number of nodes adhere to the specified rules. On the other hand, opting for `preferredDuringSchedulingIgnoredDuringExecution` indicates that the Kubernetes scheduler will **attempt** to schedule pods onto nodes aligned with the rules. In cases where no nodes fulfill the criteria, the pods will be scheduled alongside other pods.

##### Example: Difference between Preferred Affinity and Required Affinity

To illustrate the contrast between preferred affinity and required affinity, let's consider a scenario where we label only one node and create a TigerGraph cluster with specific resource requirements.

```yaml
kubectl label nodes gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g disktype=ssd
```

We create a TigerGraph cluster with resource requests that would limit one pod per node due to CPU constraints. We use `requiredDuringSchedulingIgnoredDuringExecution` to ensure nodes are selected based on the disktype label.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-nodeaffinity
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 4
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
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
```

Running kubectl get pods --output=wide provides the following output:

```bash
NAME                                                     READY   STATUS    RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-nodeaffinity-0                                      1/1     Running   0          107s    10.36.5.12   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   <none>           <none>
test-nodeaffinity-1                                      0/1     Pending   0          107s    <none>       <none>                                           <none>           <none>
test-nodeaffinity-2                                      0/1     Pending   0          106s    <none>       <none>                                           <none>           <none>
```

In this output, you can observe that only one pod has been scheduled to the node labeled with `disktype=ssd`. The remaining two pods are pending due to resource constraints, as there is only one node with the required label and that node does not have sufficient available CPU resources to accommodate all pods.

You can utilize the following command to gain insights into why `test-nodeaffinity-1` is in a pending state:

```bash
kubectl describe pod test-nodeaffinity-1
```

This command will provide detailed information about the pod's status, including any events and messages related to its scheduling and resource allocation. In this specific case, the output will indicate the reason for the pod's pending status, such as insufficient CPU resources and failure to match the pod's node affinity or selector.

Here is an example of the type of information you might encounter:

```yaml
Events:
  Type     Reason             Age                   From                Message
  ----     ------             ----                  ----                -------
  Normal   NotTriggerScaleUp  2m16s                 cluster-autoscaler  pod didn't trigger scale-up:
  Warning  FailedScheduling   101s (x2 over 2m17s)  default-scheduler   0/6 nodes are available: 1 Insufficient cpu, 5 node(s) didn't match Pod's node affinity/selector. preemption: 0/6 nodes are available: 1 No preemption victims found for incoming pod, 5 Preemption is not helpful for scheduling.
```

This output indicates that the pod is pending due to insufficient CPU resources (`Insufficient cpu`) and the fact that the node affinity or selector criteria are not being met by any available nodes (`node(s) didn't match Pod's node affinity/selector`).

Now we edit the above CR, use `preferredDuringSchedulingIgnoredDuringExecution` instead

```yaml
#......
#The same as above one
  affinityConfiguration:
    affinity:
      nodeAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 1
          preference:
            matchExpressions:
            - key: disktype
              operator: In
              values:
              - ssd         
```

Upon checking pod status with `kubectl get pods --output=wide`, you notice the following:

```bash
> kubectl get pods --output=wide
NAME                                                     READY   STATUS    RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-nodeaffinity-0                                      0/1     ContainerCreating   0          2s      <none>      gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   <none>           <none>
test-nodeaffinity-1                                      0/1     ContainerCreating   0          2s      <none>      gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   <none>           <none>
test-nodeaffinity-2                                      0/1     ContainerCreating   0          1s      <none>      gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q   <none>           <none>
```

 In the provided output, only one pod has been successfully scheduled to a node with the specified label (`disktype=ssd`). The other pods were scheduled to nodes without the specific label, which demonstrates the behavior of `preferredDuringSchedulingIgnoredDuringExecution`. This affinity setting attempts to schedule pods according to the defined preferences, but it is not a strict requirement. If nodes meeting the preferences are unavailable, the pods will still be scheduled on other nodes.

#### Weighted Affinity and Logical Operators

The `weight` attribute, ranging from 1 to 100, can be assigned to each instance of the `preferredDuringSchedulingIgnoredDuringExecution` affinity type. This weight represents the preference given to a particular affinity rule. When all other scheduling requirements for a Pod are met, the scheduler calculates a score by summing up the weights of satisfied preferred rules. This score contributes to the overall prioritization of nodes, with higher scores leading to higher scheduling priority for the Pod.

#### Combining Rules with Logical Operators

The `operator` field allows you to employ logical operators to determine how Kubernetes interprets the affinity rules. Various operators such as `In`, `NotIn`, `Exists`, `DoesNotExist`, `Gt`, and `Lt` can be used. These operators can be combined to craft nuanced rules that guide the scheduling behavior.

When using both `nodeSelector` and `nodeAffinity`, both sets of rules must be satisfied for the Pod to be scheduled onto a node.

In scenarios involving multiple terms associated with `nodeAffinity` types within `nodeSelectorTerms`, a Pod can be scheduled onto a node if any of the specified terms are satisfied (terms are ORed).

For a single term within `nodeSelectorTerms`, if multiple expressions are present in a single `matchExpressions` field, the Pod can only be scheduled onto a node if all the expressions are satisfied (expressions are ANDed).

##### Examples: Combining Multiple Rules with Different Weights

In this scenario, we have labeled nodes, with two labeled as `disktype=ssd` and two as `physical-machine=true`. We assign a weight of 1 to the `disktype=ssd` rule and a weight of 50 to the `physical-machine=true` rule. The objective is to demonstrate how to combine these rules effectively.

Nodes labeled with `disktype=ssd`:

```bash
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q
```

Nodes labeled with `physical-machine=true`:

```bash
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-9t5m 
gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l
```

Utilizing the following affinity configuration:

```yaml
affinityConfiguration:
   affinity:
     nodeAffinity:
       preferredDuringSchedulingIgnoredDuringExecution:
       - weight: 1
         preference:
           matchExpressions:
           - key: disktype
             operator: In
             values:
             - ssd
       - weight: 50
         preference:
           matchExpressions:
           - key: physical-machine
             operator: Exists
```

Running `kubectl get pods --output=wide` yields:

```plaintext
NAME                     READY   STATUS    RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-nodeaffinity-0      0/1     Running   0          20s     10.36.2.8    gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   <none>           <none>
test-nodeaffinity-1      0/1     Running   0          19s     10.36.4.5    gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-9t5m   <none>           <none>
test-nodeaffinity-2      0/1     Running   0          19s     10.36.3.12   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q   <none>           <none>
```

The pods are preferentially scheduled to nodes with the `physical-machine=true` label, as specified by the rule with a weight of 50. Two out of three pods are successfully scheduled on nodes meeting this rule. Additionally, one pod is scheduled to a node with the label `disktype=ssd`.

### Inter-pod Affinity and Anti-Affinity

[Inter-pod affinity and anti-affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#inter-pod-affinity-and-anti-affinity) offer the capability to restrict the nodes on which your Pods are scheduled based on the labels of other Pods that are already running on those nodes. This is in contrast to node affinity, which is based on the labels of the nodes themselves.

Similar to node affinity, inter-pod affinity and anti-affinity come in two types:  

- `requiredDuringSchedulingIgnoredDuringExecution`
- `preferredDuringSchedulingIgnoredDuringExecution`

Inter-pod affinity and anti-affinity rules follow this pattern: "This Pod should (or, in the case of anti-affinity, should not) run on an X node if that X node is already running one or more Pods that meet rule Y." In this context, X represents a topology domain such as a node, rack, cloud provider zone or region, and Y represents the rule that Kubernetes aims to satisfy.

These rules (Y) are expressed as label selectors, which can be associated with an optional list of namespaces. Since Pods are namespaced objects in Kubernetes, their labels inherently carry namespace information. Any label selectors used for Pod labels must explicitly specify the namespaces where Kubernetes should search for those labels.

To define the topology domain (X), a `topologyKey` is used. The `topologyKey` serves as the key for the node label that the system uses to identify the relevant domain. Careful consideration should be given to the choice of `topologyKey`. For instance, in Google Kubernetes Engine (GKE), selecting `kubernetes.io/hostname` as the topology key enables scheduling Pods to different virtual machine instances. Alternatively, using `topology.kubernetes.io/region` as the topology key allows Pods to be scheduled across different regions.

If you have specific requirements, such as the need for Pods to be distributed across certain domains, thoughtful selection of the appropriate `topologyKey` ensures that the scheduling behavior aligns with your needs.

#### Example: Avoiding Scheduling TigerGraph Pods on the Same VM Instance

In this example, we'll explore how to prevent the scheduling of TigerGraph pods on the same virtual machine (VM) instance. Each TigerGraph pod is uniquely labeled with `tigergraph.com/cluster-pod=${CLUSTER_NAME}`, which designates the cluster it belongs to. We will utilize this label to create the scheduling rule.

Consider the following Kubernetes resource definition:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 2
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
  affinityConfiguration:
    affinity:
      podAntiAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            labelSelector:
              matchExpressions:
              - key: tigergraph.com/cluster-pod
                operator: In
                values:
                - test-cluster
            topologyKey: kubernetes.io/hostname
```

This configuration enforces the rule that TigerGraph pods should not be scheduled on VM instances that are already hosting other TigerGraph pods belonging to the same cluster (`test-cluster`). However, in cases where there are insufficient nodes available, more than one TigerGraph pod may still be scheduled on the same VM instance.

By leveraging the `podAntiAffinity` feature with a preferred scheduling strategy, you ensure that TigerGraph pods are spread across different VM instances within the cluster to enhance fault tolerance and resource distribution.

Create TigerGraph with above CR and see which node the pods are scheduled to:

```bash
> kubectl get pods --output=wide

NAME                                                     READY   STATUS              RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-cluster-0                                           0/1     ContainerCreating   0          8s      <none>       gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-wh8g   <none>           <none>
test-cluster-1                                           0/1     ContainerCreating   0          8s      <none>       gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   <none>           <none>
test-cluster-2                                           0/1     Running             0          8s      10.36.2.9    gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   <none>           <none>
```

The output showed that the TigerGraph pods were scheduled to different nodes, demonstrating the successful application of the `podAntiAffinity` rule.

We can also require them to be scheduled on nodes which does not have pods of another TG cluster

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 1
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
  affinityConfiguration:
    affinity:
      podAntiAffinity:
        requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchExpressions:
            - key: tigergraph.com/cluster-pod
              operator: Exists
          topologyKey: kubernetes.io/hostname
```

This will require the scheduler to schedule pods of test-cluster to nodes that is not running any pods belonging to another TG cluster.

For example, we already have a TG cluster test-nodeaffinity and we want to create a new TG cluster named test-cluster. We don’t want pods of test-cluster to be scheduled to nodes that is running pod of test-nodeaffinity.

```bash
> kubectl get pods --output=wide

NAME                                                     READY   STATUS              RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-cluster-0                                           0/1     ContainerCreating   0          9s      <none>       gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-wh8g   <none>           <none>
test-cluster-1                                           0/1     ContainerCreating   0          9s      <none>       gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   <none>           <none>
test-cluster-2                                           0/1     Running             0          9s      10.36.1.19   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-wh8g   <none>           <none>
test-nodeaffinity-0                                      1/1     Running             0          85m     10.36.2.8    gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   <none>           <none>
test-nodeaffinity-1                                      1/1     Running             0          85m     10.36.4.5    gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-9t5m   <none>           <none>
test-nodeaffinity-2                                      1/1     Running             0          85m     10.36.3.12   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q   <none>           <none>
```

#### Scheduling Pods to Different Zones

We create an **OpenShift Cluster** which has one master node and five worker nodes.

```bash
> kubectl get nodes --show-labels

NAME                                         STATUS   ROLES    AGE   VERSION           LABELS
tg-k8s-openshift-1024-5jz2w-master-0         Ready    master   95m   v1.23.5+012e945   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/instance-type=n2-standard-4,beta.kubernetes.io/os=linux,failure-domain.beta.kubernetes.io/region=us-east1,failure-domain.beta.kubernetes.io/zone=us-east1-b,kubernetes.io/arch=amd64,kubernetes.io/hostname=tg-k8s-openshift-1024-5jz2w-master-0,kubernetes.io/os=linux,node-role.kubernetes.io/master=,node.kubernetes.io/instance-type=n2-standard-4,node.openshift.io/os_id=rhcos,topology.gke.io/zone=us-east1-b,topology.kubernetes.io/region=us-east1,topology.kubernetes.io/zone=us-east1-b
tg-k8s-openshift-1024-5jz2w-worker-b-w96n6   Ready    worker   84m   v1.23.5+012e945   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/instance-type=e2-standard-8,beta.kubernetes.io/os=linux,failure-domain.beta.kubernetes.io/region=us-east1,failure-domain.beta.kubernetes.io/zone=us-east1-b,kubernetes.io/arch=amd64,kubernetes.io/hostname=tg-k8s-openshift-1024-5jz2w-worker-b-w96n6,kubernetes.io/os=linux,node-role.kubernetes.io/worker=,node.kubernetes.io/instance-type=e2-standard-8,node.openshift.io/os_id=rhcos,topology.gke.io/zone=us-east1-b,topology.kubernetes.io/region=us-east1,topology.kubernetes.io/zone=us-east1-b
tg-k8s-openshift-1024-5jz2w-worker-b-xzrf9   Ready    worker   84m   v1.23.5+012e945   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/instance-type=e2-standard-8,beta.kubernetes.io/os=linux,failure-domain.beta.kubernetes.io/region=us-east1,failure-domain.beta.kubernetes.io/zone=us-east1-b,kubernetes.io/arch=amd64,kubernetes.io/hostname=tg-k8s-openshift-1024-5jz2w-worker-b-xzrf9,kubernetes.io/os=linux,node-role.kubernetes.io/worker=,node.kubernetes.io/instance-type=e2-standard-8,node.openshift.io/os_id=rhcos,topology.gke.io/zone=us-east1-b,topology.kubernetes.io/region=us-east1,topology.kubernetes.io/zone=us-east1-b
tg-k8s-openshift-1024-5jz2w-worker-c-456wl   Ready    worker   84m   v1.23.5+012e945   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/instance-type=e2-standard-8,beta.kubernetes.io/os=linux,failure-domain.beta.kubernetes.io/region=us-east1,failure-domain.beta.kubernetes.io/zone=us-east1-c,kubernetes.io/arch=amd64,kubernetes.io/hostname=tg-k8s-openshift-1024-5jz2w-worker-c-456wl,kubernetes.io/os=linux,node-role.kubernetes.io/worker=,node.kubernetes.io/instance-type=e2-standard-8,node.openshift.io/os_id=rhcos,topology.gke.io/zone=us-east1-c,topology.kubernetes.io/region=us-east1,topology.kubernetes.io/zone=us-east1-c
tg-k8s-openshift-1024-5jz2w-worker-c-t86pt   Ready    worker   84m   v1.23.5+012e945   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/instance-type=e2-standard-8,beta.kubernetes.io/os=linux,failure-domain.beta.kubernetes.io/region=us-east1,failure-domain.beta.kubernetes.io/zone=us-east1-c,kubernetes.io/arch=amd64,kubernetes.io/hostname=tg-k8s-openshift-1024-5jz2w-worker-c-t86pt,kubernetes.io/os=linux,node-role.kubernetes.io/worker=,node.kubernetes.io/instance-type=e2-standard-8,node.openshift.io/os_id=rhcos,topology.gke.io/zone=us-east1-c,topology.kubernetes.io/region=us-east1,topology.kubernetes.io/zone=us-east1-c
tg-k8s-openshift-1024-5jz2w-worker-d-7xv82   Ready    worker   84m   v1.23.5+012e945   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/instance-type=e2-standard-8,beta.kubernetes.io/os=linux,failure-domain.beta.kubernetes.io/region=us-east1,failure-domain.beta.kubernetes.io/zone=us-east1-d,kubernetes.io/arch=amd64,kubernetes.io/hostname=tg-k8s-openshift-1024-5jz2w-worker-d-7xv82,kubernetes.io/os=linux,node-role.kubernetes.io/worker=,node.kubernetes.io/instance-type=e2-standard-8,node.openshift.io/os_id=rhcos,topology.gke.io/zone=us-east1-d,topology.kubernetes.io/region=us-east1,topology.kubernetes.io/zone=us-east1-d
qiuyuhan@yuhan-qiu-bot-20220808075602-0:~/product/src/cqrs/k8s-operator$ 
```

Observing the node configuration, each node is associated with a label: `topology.kubernetes.io/zone=xxx`.

The master node bears the label `topology.kubernetes.io/zone=us-east1-b`, while two worker nodes are marked with `topology.kubernetes.io/zone=us-east1-b`, another two with `topology.kubernetes.io/zone=us-east1-c`, and one worker node with `topology.kubernetes.io/zone=us-east1-d`.

For the allocation of pods across distinct zones, the following affinity can be employed:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 1
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
  affinityConfiguration:
    affinity:
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

Upon creating the cluster, the assigned nodes can be observed:

```bash
NAME                 READY   STATUS              RESTARTS   AGE    IP            NODE                                         NOMINATED NODE   READINESS GATES
test-cluster-0       0/1     ContainerCreating   0          2s     <none>        tg-k8s-openshift-1024-5jz2w-worker-d-7xv82   <none>           <none>
test-cluster-1       0/1     ContainerCreating   0          2s     <none>        tg-k8s-openshift-1024-5jz2w-worker-b-w96n6   <none>           <none>
test-cluster-2       0/1     ContainerCreating   0          1s     <none>        tg-k8s-openshift-1024-5jz2w-worker-c-456wl   <none>           <none>
```

To elaborate, `tg-k8s-openshift-1024-5jz2w-worker-d-7xv82` corresponds to `us-east1-d`, `tg-k8s-openshift-1024-5jz2w-worker-b-w96n6` is positioned in `us-east1-b`, and `tg-k8s-openshift-1024-5jz2w-worker-c-456wl` is situated in `us-east1-c`.

## Toleration

[Taint and Toleration](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/)

You can put multiple taints on the same node and multiple tolerations on the same pod. The way Kubernetes processes multiple taints and tolerations is like a filter: start with all of a node's taints, then ignore the ones for which the pod has a matching toleration; the remaining un-ignored taints have the indicated effects on the pod. In particular,  

1. if there is at least one un-ignored taint with effect NoSchedule then Kubernetes will not schedule the pod onto that node  
2. if there is no un-ignored taint with effect NoSchedule but there is at least one un-ignored taint with effect PreferNoSchedule then Kubernetes will try to not schedule the pod onto the node  
3. if there is at least one un-ignored taint with effect NoExecute then the pod will be evicted from the node (if it is already running on the node), and will not be scheduled onto the node (if it is not yet running on the node).

To apply taints to our nodes, we can utilize the `kubectl taint` command. When employing a "NoSchedule" taint type, new pods that lack tolerance for this taint will not be assigned to the node. In the event of a "PreferNoSchedule" taint type, new pods that cannot tolerate this taint will not be given preference for scheduling on the node. With a "NoExecute" taint type, new pods that are intolerant to the taint will neither be scheduled nor will existing running pods be evicted from the node.

Should we furnish tolerations within the TigerGraph Custom Resource (CR), TigerGraph pods will overlook specified nodes.

It's important to note that tolerations exclusively affect pods within the TigerGraph cluster. Other pods engaged in init/upgrade/expand/shrink operations will remain unaffected.

### Example: Implementing User Groups with Taints and Tolerations

A practical application of Taints and Tolerations is the establishment of user groups for the exclusive utilization of designated nodes.

To illustrate this, let's take a step-by-step approach:

First taint 3 nodes `userGroup=enterprise:NoExecute`.This action ensures that these nodes are reserved for the designated user group.

```bash
kubectl taint nodes gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g  \
  gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q  \
  gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-9t5m  \
  userGroup=enterprise:NoExecute
```

Then create a cluster without toleration:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 4
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
```

Upon deploying the cluster, it becomes evident that all pods are scheduled to nodes devoid of the applied taints. This aligns with the concept of taints and tolerations, where pods are automatically assigned to nodes that do not possess taints that the pods cannot tolerate.

```bash
NAME                                                     READY   STATUS              RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-cluster-0                                           0/1     Running             0          14s     10.36.2.19   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   <none>           <none>
test-cluster-1                                           0/1     Running             0          14s     10.36.1.22   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-wh8g   <none>           <none>
test-cluster-2                                           0/1     ContainerCreating   0          14s     <none>       gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-lff2   <none>           <none>
```

Then we  can establish a new cluster configuration with the specified tolerations.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-toleration
spec:
  replicas: 3
  image: docker.io/tigergraph/tigergraph-k8s:3.9.2
  imagePullPolicy: IfNotPresent
  privateKeyName: ssh-key-secret
  listener:
    type: LoadBalancer
  resources:
    requests:
      cpu: 4
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      storageClassName: standard
      resources:
        requests:
          storage: 10G
  ha: 1
  license: YOUR_LICENSE
  affinityConfiguration:
    tolerations:
    - key: "userGroup"
      operator: "Equal"
      value: "enterprise"
      effect: "NoExecute"
```

By integrating tolerations into the configuration, the "test-toleration" cluster is designed to prioritize nodes with the specified taints. In this instance, pods belonging to the "test-toleration" cluster will be exclusively scheduled onto nodes bearing the "userGroup=enterprise" taint with the "NoExecute" effect.

This approach ensures that the pods from the "test-toleration" cluster are deliberately assigned to nodes with the designated taint, aligning with the defined toleration rules.

We can see that pods belonging to test-tolerations are all scheduled to nodes with taint

```bash
NAME                                                     READY   STATUS      RESTARTS   AGE     IP           NODE                                             NOMINATED NODE   READINESS GATES
test-cluster-0                                           1/1     Running     0          3m19s   10.36.2.19   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   <none>           <none>
test-cluster-1                                           1/1     Running     0          3m19s   10.36.1.22   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-wh8g   <none>           <none>
test-cluster-2                                           1/1     Running     0          3m19s   10.36.0.15   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-lff2   <none>           <none>
test-cluster-init-job-kz9hp                              0/1     Completed   0          49s     10.36.2.20   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-b99l   <none>           <none>
test-toleration-0                                        0/1     Running     0          55s     10.36.3.16   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-4z0q   <none>           <none>
test-toleration-1                                        0/1     Running     0          55s     10.36.4.6    gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-9t5m   <none>           <none>
test-toleration-2                                        1/1     Running     0          55s     10.36.5.23   gke-tg-k8s-gke-1024-default-pool-1e4fbc0f-2p9g   <none>           <none>
```

## Notice

- If the `affinityConfiguration` includes a `NodeSelector`, and the current node does not meet the `NodeSelector` configuration, and the K8S cluster has `auto-scaling` enabled, the K8S cluster will expand more nodes to accommodate the affinityConfiguration, even if the new node cannot accommodate it. This can result in a situation where there are no suitable nodes available for scheduling TigerGraph pods but useless nodes created. Therefore, it is important to configure the affinityConfiguration with the correct node specifications.

- If the `affinityConfiguration` includes `pod affinity`, and the current node does not meet the `pod affinity` settings, and the K8S cluster contains `multiple zones` with `auto-scaling` enabled, the automatic scaling of the K8S cluster will be prevented. This can result in a message like "2 node(s) had volume node affinity conflict and 1 node(s) didn't match pod affinity rules" being displayed. The "volume node affinity conflict" message means that the PV requires the current PV to be in the initial zone, which may be the reason why K8S cannot automatically scale. Similarly, there may be no suitable node available for scheduling TigerGraph pods.

    ```bash
    # Pod description
    Events:
      Type     Reason             Age   From                Message
      ----     ------             ----  ----                -------
      Warning  FailedScheduling   31s   default-scheduler   0/1 nodes are available: 1 pod has unbound immediate PersistentVolumeClaims. preemption: 0/1 nodes are available: 1 Preemption is not helpful for scheduling.
      Warning  FailedScheduling   30s   default-scheduler   0/1 nodes are available: 1 Insufficient cpu. preemption: 0/1 nodes are available: 1 No preemption victims found for incoming pod.
      Normal   NotTriggerScaleUp  29s   cluster-autoscaler  pod didn't trigger scale-up: 1 node(s) didn't match pod affinity rules, 2 node(s) had volume node affinity conflict
    
    # PV description
    Node Affinity:
      Required Terms:
        Term 0:        topology.kubernetes.io/region in [us-central1]
                       topology.kubernetes.io/zone in [us-central1-a]
    ```

- If pod scheduled failed due to limited resource, and got enough resources by expand more nodes, it may cause any pod move to another node, then it may prompt following error.

    ```bash
    Warning  FailedAttachVolume  45s   attachdetach-controller  Multi-Attach error for volume "pvc-dcdb2953-b50f-45a9-a5c3-7f7752c36698" Volume is already exclusively attached to one node and can't be attached to another
    ```

- Based on the factors mentioned, the following conclusions can be drawn:

    1. When running creating TG Cluster operations, it is crucial to configure affinityConfiguration based on the correct node resources to ensure successful scaling and operation of the cluster.

    2. It is preferred to ensure that there are corresponding nodes to implement HA during the creation TG Cluster phase, rather than updating TG Cluster in the future, because the Node Affinity of the PV may cause failure.

    3. Two common scenarios that can lead to failure are:
  
        1. In a K8S cluster with multiple zones, node resources may be insufficient. Since operator using Volume Node Affinity for PV, the pod associated with the PV must be created on the original node, resulting in the pod creation being stuck in the Pending state.
        2. When updating the affinity or adjusting the size of a TG Cluster that already has affinity, conflicts may occur due to the presence of Volume Node Affinity for PV. This is also because the pod associated with the PV must be created on the original node, but the new affinity may require the pod to be scheduled on other nodes, resulting in conflicts and potential failures.
