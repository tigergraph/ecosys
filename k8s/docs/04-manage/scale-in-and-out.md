# Scale In and Scale Out

This guide will walk you through the process of scaling in and scaling out TigerGraph Cluster using the TigerGraph Operator.

- [Scale In and Scale Out](#scale-in-and-scale-out)
  - [Introduction](#introduction)
  - [Scale Out TigerGraph Cluster (Cluster Expansion)](#scale-out-tigergraph-cluster-cluster-expansion)
    - [By kubectl tg plugin (Scale Out)](#by-kubectl-tg-plugin-scale-out)
    - [By modifying TigerGraph CR (Scale Out)](#by-modifying-tigergraph-cr-scale-out)
  - [Scale In TigerGraph Cluster (Cluster Shrinking)](#scale-in-tigergraph-cluster-cluster-shrinking)
    - [By kubectl tg plugin (Scale In)](#by-kubectl-tg-plugin-scale-in)
    - [By modifying TigerGraph CR (Scale In)](#by-modifying-tigergraph-cr-scale-in)
  - [Troubleshooting](#troubleshooting)
    - [Scale Out (Expansion) Troubleshooting](#scale-out-expansion-troubleshooting)
      - [Check Pod Status During Expansion](#check-pod-status-during-expansion)
      - [Check Expansion Job Status](#check-expansion-job-status)
      - [Verify Expansion Success](#verify-expansion-success)
      - [Common Expansion Issues](#common-expansion-issues)
    - [Scale In (Shrinking) Troubleshooting](#scale-in-shrinking-troubleshooting)
      - [Check Shrinking Job Status](#check-shrinking-job-status)
      - [Verify Shrinking Success](#verify-shrinking-success)
      - [Common Shrinking Issues](#common-shrinking-issues)
    - [General Troubleshooting](#general-troubleshooting)
      - [Pods Stuck in Pending Status](#pods-stuck-in-pending-status)
      - [Cluster Stuck in ExpandRoll State](#cluster-stuck-in-expandroll-state)
      - [Monitoring Scale Operations](#monitoring-scale-operations)

## Introduction

Scaling in and scaling out a TigerGraph cluster are fundamental operations for managing cluster capacity in a production environment.

- **Scale Out (Cluster Expansion)**: Increases the cluster size by adding more replicas (`spec.replicas`) and optionally updating the replication factor (`spec.ha`) configuration to accommodate the larger cluster.
- **Scale In (Cluster Shrinking)**: Decreases the cluster size by reducing the number of replicas (`spec.replicas`) and optionally updating the replication factor (`spec.ha`) configuration to optimize resource usage.

You can configure cluster size and HA settings by modifying `.spec.replicas` and `.spec.ha` in the TigerGraph CR or by using the `kubectl tg` plugin.

> [!WARNING]
> TigerGraph's exceptional performance comes with certain considerations regarding high availability during scaling operations. Currently, TigerGraph does not provide dedicated high-availability scale support, and some downtime is involved.

> [!IMPORTANT]
>
> 1. **Perform a full backup** of your existing system before performing the expansion/shrinking.
> 2. Ensure your TigerGraph license supports the target number of replicas.
> 3. Ensure that no loading jobs, queries, or REST requests are running on the original cluster.
> 4. Obtain a few key measures for the state of your data before the expansion, such as vertex counts/edge counts or certain query results. This will be useful in verifying data integrity after the expansion or shrinking completes.
> 5. **For clusters with [Cross-Region Replication(CRR)](../03-deploy/configure-crr-on-k8s.md) enabled**: cluster scaling changes the number and distribution of topics, while CRR depends on topic replication. Therefore, **the DR cluster must be recreated** to align with the updated topic configuration.
> 6. **For clusters with [region awareness](../03-deploy/region-awareness-with-pod-topology-spread-constraints.md) enabled**: Ensure that the TigerGraph replication factor (HA) is at least 2, and that the number of topology domains specified by `topologyKey` is at least 3..

## Scale Out TigerGraph Cluster (Cluster Expansion)

Scale out operations increase the cluster size by adding more TigerGraph pods to handle increased workload and data volume.

### By kubectl tg plugin (Scale Out)

You can use the following command to scale out the TigerGraph cluster by using the `kubectl tg` plugin:

```bash
# Scale out to 9 replicas with HA=3
kubectl tg update --cluster-name ${TG_CLUSTER_NAME} --size 9 --ha 3 --namespace ${NAMESPACE}
```

### By modifying TigerGraph CR (Scale Out)

Assuming you have a TigerGraph cluster named `test-cluster` in the namespace with 6 replicas and HA=2, you can modify the `.spec.replicas` and `.spec.ha` in the TigerGraph CR to scale out the cluster:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.2.1
  imagePullPolicy: IfNotPresent
  ha: 3                    # Increased from 2 to 3 for better fault tolerance
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 9             # Increased from 6 to 9 for more capacity
  resources:
    limits:
      cpu: "4"
      memory: 16Gi
    requests:
      cpu: "4"
      memory: 16Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: standard
```

## Scale In TigerGraph Cluster (Cluster Shrinking)

Scale in operations decrease the cluster size by removing TigerGraph pods to optimize resource usage and reduce operational costs.

### By kubectl tg plugin (Scale In)

You can use the following command to scale in the TigerGraph cluster by using the `kubectl tg` plugin:

```bash
# Scale in to 4 replicas with HA=2
kubectl tg update --cluster-name ${TG_CLUSTER_NAME} --size 4 --ha 2 --namespace ${NAMESPACE}
```

### By modifying TigerGraph CR (Scale In)

Assuming you have a TigerGraph cluster named `test-cluster` in the namespace with 8 replicas and HA=3, you can modify the `.spec.replicas` and `.spec.ha` in the TigerGraph CR to scale in the cluster:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.2.1
  imagePullPolicy: IfNotPresent
  ha: 2                    # Decreased from 3 to 2 for cost optimization
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4             # Decreased from 9 to 4 for resource optimization
  resources:
    limits:
      cpu: "4"
      memory: 16Gi
    requests:
      cpu: "4"
      memory: 16Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: standard
```

## Troubleshooting

### Scale Out (Expansion) Troubleshooting

#### Check Pod Status During Expansion

Ensure that the pods of the cluster have been scaled out to the expected size:

```bash
# test-cluster is the name of the cluster
# The example below tries to scale the cluster size from 3 to 5
kubectl get pod -l tigergraph.com/cluster-pod=${TG_CLUSTER_NAME} -n ${NAMESPACE}

NAME             READY   STATUS              RESTARTS   AGE
test-cluster-0   1/1     Running             0          17m
test-cluster-1   1/1     Running             0          17m
test-cluster-2   1/1     Running             0          17m
test-cluster-3   0/1     ContainerCreating   0          8s
test-cluster-4   0/1     ContainerCreating   0          7s
```

#### Check Expansion Job Status

Ensure the expansion job is running or has completed successfully:

```bash
# replace ${TG_CLUSTER_NAME} with your tigergraph cluster name
kubectl get job -l job-name=${TG_CLUSTER_NAME}-expand-job -n ${NAMESPACE}

NAME                      COMPLETIONS   DURATION   AGE
test-cluster-expand-job   0/1           4m13s      4m13s
```

If the expansion job fails, check the logs of the job:

```bash
# replace ${TG_CLUSTER_NAME} with your tigergraph cluster name
kubectl get pod -l job-name=${TG_CLUSTER_NAME}-expand-job -n ${NAMESPACE}

NAME                            READY   STATUS    RESTARTS   AGE
test-cluster-expand-job-6jk42   1/1     Running   0          5m38s

kubectl logs test-cluster-expand-job-6jk42 -n ${NAMESPACE}
```

#### Verify Expansion Success

- Check the status of the cluster CR:

  ```bash
  kubectl tg status --cluster-name ${TG_CLUSTER_NAME} -n ${NAMESPACE}
  ```

- Check the cluster status by executing gadmin:

  ```bash
  kubectl exec -it ${TG_CLUSTER_NAME}-0 -n ${NAMESPACE} -- /home/tigergraph/tigergraph/app/cmd/gadmin status -v
  ```

#### Common Expansion Issues

1. **Insufficient Resources**: If the K8s cluster's resources (CPU or Memory) are insufficient to expand the TigerGraph cluster:
   - For Operator versions 0.0.3 and earlier: Recreate the cluster with the same cluster name, which will load the remaining cluster data for recovery.
   - For Operator versions 0.0.4 and higher: Update the size to match the K8s cluster's available resources or reset the cluster to the previous configuration.

2. **Expansion Job Failures**: If the expanding job fails after retrying three times:
   - If you backed up the cluster before expansion, restore it with the backup package directly.
   - If there is no backup package, manual recovery is complex, so always backup the cluster before expansion.

3. **Repeated Pod Restarts**: After expansion ends, services such as GPE may take time to switch from warmup to running. This process may exceed 40 seconds (the health check limit), causing repeated pod restarts (5-12 times depending on data amount).

   **Solution**: Wait for all services to return to normal status before taking any action.

   ```bash
   kubectl describe pods ${TG_CLUSTER_NAME}-1 -n ${NAMESPACE}
   
   Events:
   Type     Reason     Age   From     Message
   ----     ------     ----  ----     -------
   Warning  Unhealthy  31m   kubelet  Readiness probe failed: service status of GPE_2#1 is Down, exit error.
   Warning  Unhealthy  30m (x9 over 74m)  kubelet  Readiness probe failed: service status of GPE_2#1 should not be Warmup, exit error
   ```

### Scale In (Shrinking) Troubleshooting

#### Check Shrinking Job Status

Ensure that the shrinking job is running or has completed successfully:

```bash
# replace ${TG_CLUSTER_NAME} with your tigergraph cluster name 
kubectl get job ${TG_CLUSTER_NAME}-shrink-pre-job -n ${NAMESPACE}

NAME                          COMPLETIONS   DURATION   AGE
test-cluster-shrink-pre-job   0/1           21s        21s
```

If the shrinking job fails, check the job logs:

```bash
kubectl get pod -l job-name=${TG_CLUSTER_NAME}-shrink-pre-job -n ${NAMESPACE}

NAME                                READY   STATUS    RESTARTS   AGE
test-cluster-shrink-pre-job-jzlhm   1/1     Running   0          2m11s

kubectl logs test-cluster-shrink-pre-job-jzlhm -n ${NAMESPACE}
```

#### Verify Shrinking Success

- Check the status of the cluster CR:

  ```bash
  kubectl tg status --cluster-name ${TG_CLUSTER_NAME} -n ${NAMESPACE}
  ```

- Check the cluster status by executing gadmin:

  ```bash
  kubectl exec -it ${TG_CLUSTER_NAME}-0 -n ${NAMESPACE} -- /home/tigergraph/tigergraph/app/cmd/gadmin status -v
  ```

#### Common Shrinking Issues

1. **Shrinking Job Failures**: If the shrinking job fails after retrying three times:
   - If you have a backup of the cluster before shrinking, restore it directly using the backup package.
   - If there is no backup package, manual recovery is complex, so always backup the cluster before shrinking.

2. **Data Loss Prevention**: When scaling in, ensure:
   - Maintain adequate HA value to prevent data loss
   - Allow sufficient time for pods to gracefully shutdown and transfer data
   - Take a backup before significant scale in operations

### General Troubleshooting

#### Pods Stuck in Pending Status

When there are not enough resources on your Nodes, pods may be stuck in `Pending` status:

```bash
kubectl get pods -n ${NAMESPACE} | grep ${TG_CLUSTER_NAME}

test-cluster-0                                            0/1     Pending     0          3m21s
test-cluster-1                                            0/1     Pending     0          113s
test-cluster-2                                            0/1     Pending     0          2m39s
```

Check pod details for resource constraints:

```bash
kubectl describe pod ${TG_CLUSTER_NAME}-0 -n ${NAMESPACE}

Events:
  Type     Reason             Age                   From                Message
  ----     ------             ----                  ----                -------
  Warning  FailedScheduling   2m40s (x4 over 4m6s)  default-scheduler   0/6 nodes are available: 6 Insufficient memory. preemption: 0/6 nodes are available: 6 No preemption victims found for incoming pod.
```

#### Cluster Stuck in ExpandRoll State

If the cluster remains in `ExpandRoll, Unknown` status for an extended period:

1. **Check Pod Status**: Verify all pods are running and ready
2. **Check Resource Availability**: Ensure sufficient resources are available for the target replica count
3. **Check Storage**: Verify persistent volume claims are available and properly configured
4. **Check Network**: Ensure network connectivity between pods is working
5. **Check Logs**: Review operator and TigerGraph pod logs for error messages

#### Monitoring Scale Operations

Monitor the following during scale operations:

```bash
# Check cluster status
kubectl get tg ${TG_CLUSTER_NAME} -n ${NAMESPACE}

# Check pod status
kubectl get pods -n ${NAMESPACE} | grep ${TG_CLUSTER_NAME}

# Check events
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'

# Check resource usage
kubectl top pods -n ${NAMESPACE} | grep ${TG_CLUSTER_NAME}
```
