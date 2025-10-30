# Node Repaving for TigerGraph on Kubernetes

Beginning with TigerGraph version 4.2.1 and Operator version 1.6.0, the TigerGraph Kubernetes Operator provides official support for node repaving in Kubernetes environments.

This document offers a detailed, step-by-step guide to performing node repaving operations for TigerGraph clusters deployed on Kubernetes.

- [Node Repaving for TigerGraph on Kubernetes](#node-repaving-for-tigergraph-on-kubernetes)
  - [Overview](#overview)
  - [Prerequisites](#prerequisites)
  - [Deploying TigerGraph with HA and Topology Spread Constraints](#deploying-tigergraph-with-ha-and-topology-spread-constraints)
    - [Sample YAML Configuration](#sample-yaml-configuration)
    - [Configuring HA settings](#configuring-ha-settings)
  - [Node repaving on EKS and GKE](#node-repaving-on-eks-and-gke)
    - [Node repaving on EKS](#node-repaving-on-eks)
    - [Node repaving on GKE](#node-repaving-on-gke)

## Overview

When running TigerGraph on Kubernetes with the TigerGraph Operator, users may need to repave (i.e., replace or remove) worker nodes for infrastructure upgrades or maintenance.

To ensure high availability during these operations, both TigerGraph and its Operator have been enhanced for better stability and flexibility.

Node repaving scenarios fall into two categories, depending on the availability of cluster resources in the affected zone:

1. Sufficient Resources:
If enough nodes are available in the same zone, the Kubernetes scheduler will automatically reschedule pods without manual intervention.

2. Insufficient Resources:
If there are not enough nodes in the same zone, rescheduled pods will remain in a Pending state due to PVC binding issues. Once new nodes are added, pods will automatically transition to Ready.

TigerGraph generally maintains high availability under both conditions, provided certain deployment best practices are followed.

Note that some critical TigerGraph components—such as GSE, GSQL, and Kafka—may perform a leader switch during node repaving. In rare cases, this can cause brief service disruption. Therefore, upstream applications should implement retry logic with a minimum retry interval of 5 seconds.

## Prerequisites

Ensure you have the following before proceeding:

- [Helm](https://helm.sh/docs/intro/install/): Version >= 3.7.0
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): Version >= 1.26
- [yq](https://github.com/mikefarah/yq): yq version >= 4.18.1
- An existing Kubernetes cluster with appropriate permissions
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html): aws-cli version >= 2.27.11
- [Google Cloud SDK](https://cloud.google.com/sdk/docs/install-sdk) gcloud version >= 410.0.0

## Deploying TigerGraph with HA and Topology Spread Constraints

Node repaving involves removing one or more Kubernetes worker nodes. During this time, the Kubernetes scheduler will reschedule TigerGraph pods to available nodes. However, these pods will be temporarily unavailable during the rescheduling period.

To minimize downtime and service impact, High Availability (HA) must be enabled before node repaving begins.

Additionally, you must configure [Topology Spread Constraints](../03-deploy/region-awareness-with-pod-topology-spread-constraints.md) to avoid placing multiple pods of the same type on a single node. This enhances fault tolerance and ensures better workload distribution.

### Sample YAML Configuration

The following YAML shows how to deploy a TigerGraph cluster with HA and topology spread constraints enabled to support high availability during node repaving:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: ${CLUSTER_NAME}
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.2.1
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  license: ${LICENSE}
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4
  resources:
    limits:
      cpu: 6
      memory: 12Gi
    requests:
      cpu: 6
      memory: 12Gi
  securityContext:
    privileged: false
    runAsGroup: 1000
    runAsUser: 1000
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: ${STORAGE_CLASS}
      volumeMode: Filesystem
  topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule 
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: ${CLUSTER_NAME}
        matchLabelKeys:
          - pod-template-hash
      - maxSkew: 1
        topologyKey: "kubernetes.io/hostname"
        whenUnsatisfiable: DoNotSchedule 
        labelSelector:
          matchLabels:
            tigergraph.com/cluster-pod: ${CLUSTER_NAME}
```

### Configuring HA settings

TigerGraph's HA (High Availability) service provides load balancing when all components are operational, and automatic failover in the event of a service disruption. For detailed information, please refer to the [official documents](https://docs.tigergraph.com/tigergraph-server/current/cluster-and-ha-management/ha-cluster).

The minimum value for the replication factor (HA) is 1, meaning high availability is not configured for the cluster. The partitioning factor is not explicitly set by the user; instead, TigerGraph determines it using the following formula:

`partitioning factor = number of pods / replication factor`

If the result is not an integer, some machines will remain unused. For example, in a 7-node cluster with a replication factor of 2, the system will configure 2-way HA with a partitioning factor of 3, leaving one machine unused.

In general, we recommend setting the replication factor (HA) to 2 and using a cluster size that is a power of 2 (e.g., 4, 8, 16)

## Node repaving on EKS and GKE

> [!IMPORTANT]  
> To ensure service continuity, repave one node at a time.Even if your cluster uses ha: 2, some TigerGraph services might not run on every pod, so gradual node replacement is strongly recommended.

### Node repaving on EKS

In this section, we demonstrate how to repave a node in Amazon EKS using a managed node group that supports auto-scaling.

- Deploy a TigerGraph Cluster with HA and Topology Spread Constraints
    Here we skip the TigerGraph Operator installation process, you can refer to the document [Deploy TigerGraph on AWS EKS](../03-deploy/tigergraph-on-eks.md) for the details.

```bash
export CLUSTER_NAME=<YOUR_CLUSTER_NAME>
export LICENSE=<YOUR_LICENSE>
export NAMESPACE=<YOUR_NAMESPACE>
export STORAGE_CLASS=<YOUR_STORAGE_CLASS>

cat <<EOF | kubectl apply -f -
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: ${CLUSTER_NAME}
  namespace: ${NAMESPACE}
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.2.1
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: ${LICENSE}
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4
  resources:
    limits:
      cpu: "6"
      memory: "12Gi"
    requests:
      cpu: "6"
      memory: "12Gi"
  securityContext:
    privileged: false
    runAsGroup: 1000
    runAsUser: 1000
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 100Gi
      storageClassName: ${STORAGE_CLASS}
      volumeMode: Filesystem
  topologySpreadConstraints:
    - maxSkew: 1
      topologyKey: topology.kubernetes.io/zone
      whenUnsatisfiable: DoNotSchedule
      labelSelector:
        matchLabels:
          tigergraph.com/cluster-pod: ${CLUSTER_NAME}
      matchLabelKeys:
        - pod-template-hash
    - maxSkew: 1
      topologyKey: kubernetes.io/hostname
      whenUnsatisfiable: DoNotSchedule
      labelSelector:
        matchLabels:
          tigergraph.com/cluster-pod: ${CLUSTER_NAME}
EOF
```

- Ensure Cluster is in Normal Status
    Before performing node repaving, ensure that the TigerGraph cluster is in the Normal status.

    ```bash
    kubectl get tg $CLUSTER_NAME -n $NAMESPACE -o yaml | yq .status.conditions[0].type
    ```

    example output:

    ```bash
    $ kubectl get tg $CLUSTER_NAME -n $NAMESPACE -o yaml | yq .status.conditions[0].type
    Normal
    ```

- Identify the Node Hosting the Target Pod

    List the TigerGraph pods:

    ```bash
    kubectl get pods -l tigergraph.com/cluster-pod=$CLUSTER_NAME -n $NAMESPACE
    ```

    example output:

    ```bash
    NAME             READY   STATUS    RESTARTS   AGE
    test-cluster-0   1/1     Running   0          18m
    test-cluster-1   1/1     Running   0          19m
    test-cluster-2   1/1     Running   0          21m
    test-cluster-3   1/1     Running   0          22m
    ```

    Here we take pod test-cluster-0 as the value of `pod_name` below

    ```bash
    pod_name=test-cluster-0
    node_name=$(kubectl get pod $pod_name -n $NAMESPACE -o yaml|yq .spec.nodeName)
    ```

    example output:

    ```bash
    $ kubectl get pod $pod_name -n $NAMESPACE -o yaml|yq .spec.nodeName
    ip-10-1-116-183.us-east-2.compute.internal
    ```

- Marks the node as unschedulable to prevent new pods from being scheduled on it

    ```bash
    kubectl cordon $node_name
    ```

- Evicts all non-daemonset pods, deletes emptyDir data, and forces eviction even if the node is not managed by a cloud provider or has non-replicated pods

    ```bash
    kubectl drain $node_name --ignore-daemonsets --delete-emptydir-data --force
    ```

- Verify TigerGraph Services Remain Accessible

    Update the example graph query below based on your test scenario.

    Query the external service address:

    ```bash
    export EXTERNAL_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-nginx-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].hostname}')
    ```

    Verify the RESTPP API service

    ```bash
    curl http://${EXTERNAL_SERVICE_ADDRESS}:14240/restpp/echo

    {"error":false, "message":"Hello GSQL"}
    ```

- Terminate the Node from the Auto-Scaling Group

    Acquire the node instance id by node name:

    ```bash
    NODE_INSTANCE_ID=$(kubectl get node $node_name -o json | jq -r '.spec.providerID'|sed 's#.*/##')
    ```

    Example output:

    ```bash
    $ kubectl get node $node_name -o json | jq -r '.spec.providerID'|sed 's#.*/##'
    i-0949da727f98ec5c6
    ```

    Terminate the node instance:

    ```bash
    NODE_INSTANCE_ID=$(kubectl get node $node_name -o json | jq -r '.spec.providerID'|sed 's#.*/##')
    aws autoscaling terminate-instance-in-auto-scaling-group \
        --instance-id $NODE_INSTANCE_ID \
        --no-should-decrement-desired-capacity
    ```

- Ensure Cluster Returns to Normal Status

    ```bash
    kubectl get tg $CLUSTER_NAME -n $NAMESPACE -o yaml | yq .status.conditions[0].type
    ```

Follow the above steps to remove one node. To repave other nodes, simply repeat the same procedure.

### Node repaving on GKE

In the following example, we'll conduct node repaving test for TigerGraph with a standard GKE, which have an auto-scale node pool with fixed size that can scale up a new node automatically after removing a node.

- Deploy a TigerGraph Cluster with HA and Topology Spread Constraints

    Here we skip the TigerGraph Operator installation process, you can refer to the document [Deploy TigerGraph on Google Cloud GKE](../03-deploy/tigergraph-on-gke.md) for the details.

```bash
export CLUSTER_NAME=<YOUR_CLUSTER_NAME>
export LICENSE=<YOUR_LICENSE>
export NAMESPACE=<YOUR_NAMESPACE>
export STORAGE_CLASS=<YOUR_STORAGE_CLASS>

cat <<EOF | kubectl apply -f -
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: ${CLUSTER_NAME}
  namespace: ${NAMESPACE}
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.2.1
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: ${LICENSE}
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4
  resources:
    limits:
      cpu: "6"
      memory: "12Gi"
    requests:
      cpu: "6"
      memory: "12Gi"
  securityContext:
    privileged: false
    runAsGroup: 1000
    runAsUser: 1000
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 100Gi
      storageClassName: ${STORAGE_CLASS}
      volumeMode: Filesystem
  topologySpreadConstraints:
    - maxSkew: 1
      topologyKey: topology.kubernetes.io/zone
      whenUnsatisfiable: DoNotSchedule
      labelSelector:
        matchLabels:
          tigergraph.com/cluster-pod: ${CLUSTER_NAME}
      matchLabelKeys:
        - pod-template-hash
    - maxSkew: 1
      topologyKey: kubernetes.io/hostname
      whenUnsatisfiable: DoNotSchedule
      labelSelector:
        matchLabels:
          tigergraph.com/cluster-pod: ${CLUSTER_NAME}
EOF
```

- Verify Cluster Status
    Before performing node repaving, ensure that the TigerGraph cluster is in the Normal status.

    ```bash
    kubectl get tg $CLUSTER_NAME -n $NAMESPACE -o yaml | yq .status.conditions[0].type
    ```

    example output:

    ```bash
    $ kubectl get tg $CLUSTER_NAME -n $NAMESPACE -o yaml | yq .status.conditions[0].type
    Normal
    ```

- Find Node Name for a Running Pod

    List the TigerGraph pods:

    ```bash
    kubectl get pods -l tigergraph.com/cluster-pod=$CLUSTER_NAME -n $NAMESPACE
    ```

    example output:

    ```bash
    NAME             READY   STATUS    RESTARTS   AGE
    test-cluster-0   1/1     Running   0          18m
    test-cluster-1   1/1     Running   0          19m
    test-cluster-2   1/1     Running   0          21m
    test-cluster-3   1/1     Running   0          22m
    ```

    Here we take pod test-cluster-0 as the value of `pod_name` below

    ```bash
    pod_name=test-cluster-0
    node_name=$(kubectl get pod $pod_name -n $NAMESPACE -o yaml|yq .spec.nodeName)
    ```

    example output:

    ```bash
    $ kubectl get pod $pod_name -n $NAMESPACE -o yaml|yq .spec.nodeName
    gke-tg-gke-test-default-pool-193dc71f-d8pk
    ```

- Marks the node as unschedulable to prevent new pods from being scheduled on it

    ```bash
    kubectl cordon $node_name
    ```

- Evicts all non-daemonset pods, deletes emptyDir data, and forces eviction even if the node is not managed by a cloud provider or has non-replicated pods

    ```bash
    kubectl drain $node_name --ignore-daemonsets --delete-emptydir-data --force
    ```

- Verify Services Are Still Reachable

    Update the example graph query below based on your test scenario.

    Query the external service address:

    ```bash
    export EXTERNAL_SERVICE_ADDRESS=$(kubectl get svc/${YOUR_CLUSTER_NAME}-nginx-external-service --namespace ${YOUR_NAMESPACE} -o=jsonpath='{.status.loadBalancer.ingress[0].hostname}')
    ```

    Verify the RESTPP API service

    ```bash
    curl http://${EXTERNAL_SERVICE_ADDRESS}:14240/restpp/echo

    {"error":false, "message":"Hello GSQL"}
    ```

- Delete the GKE Node Instance

    ```bash
    gcloud compute instances delete $node_name --zone ${GCP_ZONE} --quiet
    ```

- Confirm Normal Cluster Status

    ```bash
    kubectl get tg $CLUSTER_NAME -n $NAMESPACE -o yaml | yq .status.conditions[0].type
    ```

Follow the above steps to remove one node. To repave other nodes, simply repeat the same procedure.
