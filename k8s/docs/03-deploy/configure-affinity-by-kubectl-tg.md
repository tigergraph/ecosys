<h1> Use Affinity in kubectl-tg plugin</h1>

To know how to use NodeSelector/Affinity/Tolerations in YAML, please read [NodeSelector, Affinity and Tolerations using cases](./affinity-use-cases.md) .

This DOC will include all cases in above document.

- [Usage Instructions](#usage-instructions)
  - [Removing Affinity Configuration](#removing-affinity-configuration)
- [Examples](#examples)
  - [Scheduling Pods on Nodes with `disktype=ssd` Label](#scheduling-pods-on-nodes-with-disktypessd-label)
  - [Preferring Pods to be Scheduled on Nodes with `disktype=ssd` Label](#preferring-pods-to-be-scheduled-on-nodes-with-disktypessd-label)
  - [Combining Multiple Rules with Different Weights](#combining-multiple-rules-with-different-weights)
  - [Preventing Multiple TigerGraph Pods on the Same VM Instance](#preventing-multiple-tigergraph-pods-on-the-same-vm-instance)
  - [Require TG pods not to be scheduled to VM instances that is running TG pods belonging to another cluster](#require-tg-pods-not-to-be-scheduled-to-vm-instances-that-is-running-tg-pods-belonging-to-another-cluster)
  - [Require TG pods not to be scheduled to the same zone](#require-tg-pods-not-to-be-scheduled-to-the-same-zone)
  - [Implementing User Groups using Taints and Tolerations](#implementing-user-groups-using-taints-and-tolerations)
  - [See also](#see-also)

Usage Instructions
=====

To employ affinity within `kubectl-tg`, the procedure involves crafting your affinity rules in a YAML file. Presented below is an exemplary affinity configuration file:

```yaml
# NodeSelector field.
# See https://kubernetes.io/docs/tasks/configure-pod-container/assign-pods-nodes/ 
nodeSelector:
    disktype: ssd
# Affinity, include Node Affinity, Pod Affinity and Pod Anti-Affinity
# See https://kubernetes.io/docs/tasks/configure-pod-container/assign-pods-nodes-using-node-affinity/
# and https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#inter-pod-affinity-and-anti-affinity
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
      - matchExpressions:
        - key: topology.kubernetes.io/zone
          operator: In
          values:
          - antarctica-east1
          - antarctica-west1
  podAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchExpressions:
        - key: security
          operator: In
          values:
          - S1
      topologyKey: topology.kubernetes.io/zone
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: security
            operator: In
            values:
            - S2
        topologyKey: topology.kubernetes.io/zone
# Tolerations. See https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/
tolerations:
  - key: "example-key"
    operator: "Exists"
    effect: "NoSchedule"
```

Assume that we name our affinity config file as `tg-affinity.yaml`.

Then we can use following command to create a TG cluster with the rules we write:

```bash
kubectl tg create --cluster-name test-cluster --size 3 --ha 1 --namespace NAMESPACE \
  --version 3.9.1 --storage-class standard --storage-size 10G  \
  --private-key-secret ssh-key-secret \
  --affinity tg-affinity.yaml
```

For an existing cluster, the affinity rules can be updated with this command:

```bash
kubectl tg update --cluster-name test-cluster --namespace NAMESPACE \
  --affinity tg-affinity.yaml
```

Removing Affinity Configuration
----------------------------------

To eliminate all existing tolerations, affinity rules, and nodeSelectors from your TigerGraph cluster configuration, the process is straightforward. Follow the steps outlined below:

1. **Create an Empty YAML File:**

   Start by generating an empty YAML file. You can create an empty file named `empty.yaml` using the following command:

   ```bash
   touch empty.yaml
   ```

2. **Execute Removal Operation:**

   To perform the removal of all tolerations, affinity rules, and nodeSelectors, invoke the `kubectl tg update` command and provide the `--affinity` option with the previously created empty YAML file:

   ```bash
   kubectl tg update --cluster-name test-cluster --namespace NAMESPACE \
     --affinity empty.yaml
   ```

This procedure effectively clears all existing affinity-related configurations, providing a clean slate for your TigerGraph cluster settings. If you wish to retain certain rules while removing others, simply modify your configuration file accordingly and execute the `kubectl tg update` command.


Examples
========

Scheduling Pods on Nodes with `disktype=ssd` Label
-----------------------------------------------------

To ensure that pods are scheduled exclusively on nodes labeled with `disktype=ssd`, you can utilize the provided affinity configurations. These configurations utilize both Node Selector and Node Affinity approaches. Please note that when employing **required** rules, if an insufficient number of nodes with the desired label are available for scheduling TigerGraph (TG) pods, the pods will remain in a Pending status.

1. **Using Node Selector:**
   ```yaml
   nodeSelector:
     disktype: ssd
   ```

2. **Using Node Affinity:**
   ```yaml
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

With these configurations, TigerGraph pods will be scheduled specifically on nodes bearing the `disktype=ssd` label. However, it's important to be aware that if there are an inadequate number of nodes fulfilling this criterion, the TG pods may become Pending due to the required scheduling rules.


Preferring Pods to be Scheduled on Nodes with `disktype=ssd` Label
--------------------------------------------------------------------

If your objective is to prioritize scheduling pods on nodes labeled with `disktype=ssd`, you can implement the desired behavior using a preferred rule within the affinity configuration. Here's how you can achieve this:

```yaml
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

With this affinity configuration, the specified weight of 1 signifies a preference for scheduling pods on nodes with the `disktype=ssd` label. However, in scenarios where an insufficient number of nodes possess this label, the pods will still be scheduled on other available nodes that lack the label.

By utilizing this **preferred** rule, you ensure that scheduling attempts prioritize nodes with the desired label, while also allowing for scheduling flexibility to accommodate situations where a limited number of labeled nodes are available. This approach offers a balanced trade-off between preference and availability, optimizing the scheduling behavior of your pods within your Kubernetes cluster.

Combining Multiple Rules with Different Weights
-----------------------------------------------

When you need to combine multiple affinity rules with varying weights to guide pod scheduling, you can achieve this by utilizing a configuration similar to the one you provided. Here's an example configuration:

```yaml
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

With this configuration:

1. Pods will be preferentially scheduled to nodes with the `physical-machine=true` label due to the higher weight of 50.
2. If nodes with the `physical-machine=true` label are not available, the next preference will be for nodes with the `disktype=ssd` label, indicated by a weight of 1.

This approach provides a flexible and versatile way to guide pod scheduling behavior based on the defined affinity rules and their associated weights. It ensures that pods are distributed across nodes according to the specified preferences while accommodating availability constraints.

Preventing Multiple TigerGraph Pods on the Same VM Instance
------------------------------------------------------------

To ensure that no more than one TigerGraph pod is scheduled on the same VM instance, you can employ a `podAntiAffinity` configuration. This rule helps distribute TigerGraph pods across different VM instances, thus avoiding overloading a single instance. Here's how you can achieve this:

1. **Identify the VM Instance Label:**

   First, ascertain the label that designates the VM instance where a node is running. In the case of GKE, the label is `kubernetes.io/hostname=xxx`.

2. **Apply Affinity Configuration:**

   Utilize the following affinity configuration in your deployment:

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: tigergraph.com/cluster-name
            operator: In
            values:
            - test-cluster
        topologyKey: kubernetes.io/hostname
```

With this configuration:

- The rule establishes that TigerGraph pods should not be scheduled onto a VM instance where another TigerGraph pod from the same cluster is already running.
- The topologyKey `kubernetes.io/hostname` ensures that the affinity rule considers the VM instance label.

This approach effectively prevents the overloading of a single VM instance by ensuring that TigerGraph pods are distributed across different VM instances, while still accommodating availability constraints.

Please note that if there are an adequate number of nodes available, multiple TigerGraph pods may still be scheduled on the same VM instance. The rule is designed to minimize such instances and optimize distribution across VM instances.

Require TG pods not to be scheduled to VM instances that is running TG pods belonging to another cluster
--------------------------------------------------------------------------------------------------------

```yaml
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchExpressions:
        - key: tigergraph.com/cluster-pod
          operator: Exists
      topologyKey: kubernetes.io/hostname
```

Require TG pods not to be scheduled to the same zone
----------------------------------------------------

```yaml
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

Implementing User Groups using Taints and Tolerations
-------------------------------------------------------

To establish user groups and control pod scheduling based on taints and tolerations, follow these steps:

1. **Taint Nodes:**

   Begin by tainting specific nodes with the label `userGroup=enterprise` and the effect `NoExecute` using the following command:

   ```bash
   kubectl taint nodes <node-name> userGroup=enterprise:NoExecute
   ```

2. **Pod Configuration:**

   To ensure that only pods with the appropriate tolerations are scheduled on the tainted nodes, include the following tolerations configuration in your pod specification:

   ```yaml
   tolerations:
   - key: "userGroup"
     operator: "Equal"
     value: "enterprise"
     effect: "NoExecute"
   ```

   This configuration specifies that the pods should tolerate the taint with the label `userGroup=enterprise` and the effect `NoExecute`, allowing them to be scheduled on the tainted nodes.

By following these steps, you can successfully implement user groups using taints and tolerations. Only pods that adhere to the defined toleration rules will be scheduled on the nodes tainted with the `userGroup=enterprise` label and `NoExecute` effect, allowing you to control and segregate pod scheduling based on user groups.

## See also

If you are interested in learning how to use and configure Pod affinity with YAML resources, please refer to the following documentation:

- [NodeSelector, Affinity and Toleration Use Cases](../03-deploy/affinity-use-cases.md)
