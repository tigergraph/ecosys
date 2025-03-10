# Scale Up and Scale Down

This guide will walk you through the process of scaling up and scaling down TigerGraph Cluster using the TigerGraph Operator.

- [Scale Up and Scale Down](#scale-up-and-scale-down)
  - [Introduction](#introduction)
  - [Scale Up/Down TigerGraph Cluster](#scale-updown-tigergraph-cluster)
    - [By kubectl tg plugin](#by-kubectl-tg-plugin)
    - [By modifying TigerGraph CR](#by-modifying-tigergraph-cr)
  - [Overview of the Scale Up/Down Process](#overview-of-the-scale-updown-process)
  - [Troubleshooting](#troubleshooting)

## Introduction

Scaling up and scaling down a TigerGraph cluster is a common operation in a production environment. Scaling up lets you increase CPU/memory resources of each pod in the cluster, while scaling down lets you decrease CPU/memory resources of each pod in the cluster. You can configure CPU/memory resources by modifying `.spec.resources` in the TigerGraph CR or by using the `kubectl tg` plugin.

To know more about `resources` in Kubernetes, please refer to [Resource Management for Pods and Containers](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/).

## Scale Up/Down TigerGraph Cluster

### By kubectl tg plugin

You can use following command to scale up/down the TigerGraph cluster by using the `kubectl tg` plugin:

```bash
kubectl tg update --cluster-name ${TG_CLUSTER_NAME} -n ${NAMESPACE} \
  --cpu 6 --memory 16Gi --cpu-limit 6 --memory-limit 16Gi
```

### By modifying TigerGraph CR

Assuming you have a TigerGraph cluster named `test-cluster` in the namespace whose CPU is 4 and memory is 8Gi, you can modify the `.spec.resources` in the TigerGraph CR to scale up the cluster:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.2.0
  imagePullPolicy: IfNotPresent
  ha: 2
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 6
  resources:
    limits:
      cpu: "6"
      memory: 16Gi
    requests:
      cpu: "6"
      memory: 16Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: standard
```

## Overview of the Scale Up/Down Process

Since we introduce an improvement **Enhanced Rolling Upgrade orchestration by the Operator** in 1.5.0, the process of scaling up and scaling down a TigerGraph cluster is more efficient and reliable.
According to the version of Operator, the version and HA of TigerGraph cluster, the process of scaling up and scaling down a TigerGraph cluster is different. Please refer to the following table for more details:

| Operator Version | TigerGraph Version | HA of TigerGraph Cluster | Description |
| --- | --- | --- | --- |
| < 1.5.0 | Any | Any | The strategy of StatefulSet is `RollingUpdate`. TigerGraph Operator triggers rolling update of StatefulSet to scale up or scale down the TigerGraph cluster. To know more about the RollingUpdate of StatefulSet, please refer to [Rolling Updates](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#rolling-updates). |
| >= 1.5.0 | Any | 1 | The HA of cluster is 1, which means that the cluster does not enable High Availability. Each partition only has one replication. For such a cluster, **if one of the pods is unavailable, the cluster will be unavailable**. So the RollingUpdate strategy of StatefulSet is not suitable for such a cluster. The downtime will be very long since the pods are restarted one by one. The cluster won't be available until all the pods are restarted. To reduce the downtime, Operator set the strategy of StatefulSet to `OnDelete`. When you scale up or scale down the cluster, the Operator will delete all pods **at the same time** and let them be recreated by StatefulSet controller. The downtime will be reduced to the minimum. |
| >= 1.5.0 | < 4.2.0 | >= 2 | The HA of cluster is greater than 1, which means that the cluster enables High Availability. Each partition has more than one replication. For such a cluster, the cluster is only unavailable when the leader pod is killed. But for cluster < 4.2.0, the Operator is unable to get the leader pod of each partition. So the Operator will restart pods from the last pod to the first pod, which is the same as the strategy of StatefulSet `RollingUpdate`. |
| >= 1.5.0 | >= 4.2.0 | >= 2 | The HA of cluster is greater than 1, which means that the cluster enables High Availability. Each partition has more than one replication. For such a cluster, the cluster is only unavailable when the leader pod is killed. The Operator is able to get the leader pod of each partition for cluster >= 4.2.0. So the Operator will restart the follower pods first and then restart the leader pods. This strategy is trying to minimize the number of leader switches. |

When you scale up or scale down the TigerGraph cluster, the cluster will become `UpdateRoll, Unknown` status, and the Operator will restart the pods according to the strategy mentioned above. When the HA of the cluster is greater than 1, the cluster is available most of the time during the UpdateRoll state. When TigerGraph Operator > 1.5.0,the TigerGraph cluster >= 4.2.0 and the HA of the cluster is greater than 1, the downtime of the cluster reaches the minimum  during the process of scale up and scale down. We call this process **Online Scale Up/Down**.

> [!NOTE]
> When you upgrade TigerGraph cluster, the cluster will go through a `UpgradeRoll` state, the `UpgradeRoll` state follows the same strategy as the `UpdateRoll` state.

## Troubleshooting

When there is not enough resource on you Node, the pods may be stuck in `Pending` status and the cluster may be stuck in `UpdateRoll, Unknown` status. When the cluster is in `UpdateRoll, Unknown` status for a long time, you can check the status of the pods by running the following command:

```bash
kubectl get pods -n ${NAMESPACE} | grep ${TG_CLUSTER_NAME}
```

The output will be like:

```bash
test-cluster-0                                            0/1     Pending     0          3m21s
test-cluster-1                                            0/1     Pending     0          113s
test-cluster-2                                            0/1     Pending     0          2m39s
test-cluster-init-job-pzqdg                               0/1     Completed   0          4h4m
```

For the pods that are stuck in `Pending` status, you can describe the pod to get more information:

```bash
kubectl describe pod test-cluster-0 -n ${NAMESPACE}
```

See the events part of the output to find the reason why the pod is stuck in `Pending` status.

```bash
Events:
  Type     Reason             Age                   From                Message
  ----     ------             ----                  ----                -------
  Normal   NotTriggerScaleUp  4m6s                  cluster-autoscaler  pod didn't trigger scale-up:
  Warning  FailedScheduling   2m40s (x4 over 4m6s)  default-scheduler   0/6 nodes are available: 6 Insufficient memory. preemption: 0/6 nodes are available: 6 No preemption victims found for incoming pod.
```

In this case, the pod is stuck in `Pending` status because there is not enough memory on the Nodes. Check the capacity of your Nodes and the resource requests of the TigerGraph CR to make sure the pods can be scheduled on the Nodes.

```bash
$ kubectl get nodes  -o yaml | yq '.items.[0].status.capacity.memory'
65851384Ki
```

```bash
$ kubectl get tg ${TG_CLUSTER_NAME} -n ${NAMESPACE} -o yaml | yq .spec.resources
requests:
  cpu: "6"
  memory: 64Gi
```

Since the resource requests of the TigerGraph CR are greater than the capacity of the Nodes, you can modify the resource requests of the TigerGraph CR to make sure the pods can be scheduled on the Nodes.
