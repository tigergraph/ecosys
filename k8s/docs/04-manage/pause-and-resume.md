# Pause and Resume TigerGraph cluster

If you have experience with Custom Resources in Kubernetes, you can modify TigerGraph CR to pause and resume TigerGraph cluster. Alternatively, you can also use `kubectl tg` command to pause and resume TigerGraph cluster.

When you pause a TigerGraph cluster, the TigerGraph Operator will clean **all services** of the cluster and delete **all computing resources** including all pods, while **persistent volumes will be kept** . When you resume a TigerGraph cluster, the TigerGraph Operator will create all services and pods of the cluster again.

> [!WARNING]
> When a cluster is paused, all services will be unavailable. Please make sure that there is no active job including loading job and query running on the cluster before pausing it.

You can pause cluster only when the cluster is in `Normal` or `Resume` status. You can resume cluster only when the cluster is in `Paused` status. That means when the cluster is initializing, upgrading, scaling or config-updating, you cannot pause the cluster. If you try to pause or resume the cluster in these cases, you will get an error message.

- [Pause and Resume TigerGraph cluster](#pause-and-resume-tigergraph-cluster)
  - [Pause and Resume TigerGraph cluster by kubectl tg](#pause-and-resume-tigergraph-cluster-by-kubectl-tg)
    - [Prerequisites](#prerequisites)
    - [Utilizing `kubectl tg` to pause TigerGraph cluster](#utilizing-kubectl-tg-to-pause-tigergraph-cluster)
      - [Pause a running cluster](#pause-a-running-cluster)
      - [Pause a running cluster along with its backup schedule](#pause-a-running-cluster-along-with-its-backup-schedule)
    - [Utilizing `kubectl tg` to resume TigerGraph cluster](#utilizing-kubectl-tg-to-resume-tigergraph-cluster)
      - [Resume a paused cluster](#resume-a-paused-cluster)
      - [Resume a paused cluster with updated resources](#resume-a-paused-cluster-with-updated-resources)
  - [Pause and Resume TigerGraph cluster by modifying TigerGraph CR](#pause-and-resume-tigergraph-cluster-by-modifying-tigergraph-cr)
  - [Troubleshooting](#troubleshooting)
    - [It takes too long to pause a cluster](#it-takes-too-long-to-pause-a-cluster)
    - [Cluster cannot resume successfully due to insufficient resources](#cluster-cannot-resume-successfully-due-to-insufficient-resources)

## Pause and Resume TigerGraph cluster by kubectl tg

### Prerequisites

The successful execution of the `kubectl tg pause|resume` commands requires that you have installed the `kubectl tg` command line tool. For more information, see [Install kubectl-tg plugin](../02-get-started/get_started.md#install-kubectl-tg-plugin). Additionally, you must create your cluster as a prerequisite step.

### Utilizing `kubectl tg` to pause TigerGraph cluster

```bash
Pause a running cluster

Usage:
  kubectl tg pause [options]

Options:
  -n|--namespace :    set namespace to deploy TG cluster, if not set, use the default namespace in context
  -c|--cluster-name : 
                      (required)set cluster-name to deploy TG cluster, no default
  --cascade :         if the option is given, related backup schedules will be paused
```

#### Pause a running cluster

First check the name of the running cluster that you want to pause:

```bash
kubectl tg list -n tigergraph
```

The output should be similar to the following:

```bash
NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster   3          3              2            docker.io/tigergraph/tigergraph-k8s:3.9.3   LoadBalancer   Normal           True               12m
```

Use the following command to pause a running cluster:

```bash
kubectl tg pause --cluster-name test-cluster -n tigergraph
```

Check the status of the cluster:

```bash
kubectl tg list -n tigergraph

NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster   3          3              2            docker.io/tigergraph/tigergraph-k8s:3.9.3   LoadBalancer   PauseRoll        Unknown            13m
```

Wait for several minutes until the cluster is paused. You can check the status of the cluster:

```bash
kubectl tg list -n tigergraph

NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster   3          3              2            docker.io/tigergraph/tigergraph-k8s:3.9.3   LoadBalancer   Paused           True               13m
```

#### Pause a running cluster along with its backup schedule

You may have created a backup schedule for your cluster. When you pause the cluster, the backup schedule will still try to backup the cluster according to the schedule. So it's better to pause all backup schedule of the cluster. If you want to pause the cluster along with its backup schedule, you can use the `--cascade` option:

```bash
kubectl tg pause --cluster-name test-cluster -n tigergraph --cascade
```

### Utilizing `kubectl tg` to resume TigerGraph cluster

```bash
Resume a paused cluster

Usage:
  kubectl tg resume [options]

Options:
  -n|--namespace :    set namespace to deploy TG cluster, if not set, use the default namespace in context
  -c|--cluster-name : 
                      (required)set cluster-name to deploy TG cluster, no default
  -k|--private-key-secret :  
                      set the secret name of private ssh key
  --service-account-name :  
                      set the name of the service account for TG cluster pod, default empty
  --listener-type :   update TG cluster listener type, available types: NodePort, LoadBalancer, and Ingress, default LoadBalancer
  --nginx-node-port : update node port of TG cluster external nginx service when listener type is NodePort, default as 30240
  --nginx-host :      update host name of TG cluster external nginx service when listener type is Ingress, default empty
  --secret-name :     update secret name of TG cluster external service when listener type is Ingress, default empty
  --listener-labels : add the labels to TG services, your input should be like 'k1=v1,k2="v2 with space"'
  --listener-annotations :     
                      add the annotations to TG services, your input should be like 'k1=v1,k2="v2 with space"'
  --cpu :               update TG cluster cpu size of every instance, default as 8000m
  --cpu-limit :         limit cpu size of every instance, if you set it to 0, cpu is not limited
  --memory :            update TG cluster memory size of every instance, default as 16Gi
  --memory-limit :      limit memory size of every instance, if you set it to 0, memory is not limited
  --affinity :        give a YAML file to specify the nodeSelector,affinity and tolerations for TigerGraph pods
  --custom-containers :  
                      give a YAML file to add sidecar containers,init containers and sidecar volumes to TigerGraph pods
  --pod-labels :      add some customized labels to all pods, your input should be like like 'k1=v1,k2="v2 with space"'
  --pod-annotations : add some customized annotations to all pods, your input should be like like 'k1=v1,k2="v2 with space"'
```

#### Resume a paused cluster

First check the name of the paused cluster that you want to resume:

```bash
kubectl tg list -n tigergraph
```

The output should be similar to the following:

```bash
NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster              3              2            docker.io/tigergraph/tigergraph-k8s:3.9.3   LoadBalancer   Paused           True               27m
```

Use the following command to resume a paused cluster:

```bash
kubectl tg resume --cluster-name test-cluster -n tigergraph
```

Check the status of the cluster:

```bash
kubectl tg list -n tigergraph -w

NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster   3          3              2            docker.io/tigergraph/tigergraph-k8s:3.9.3   LoadBalancer   ResumeRoll       Unknown            29m
```

Wait for several minutes until the cluster is resumed. You can check the status of the cluster:

```bash
kubectl tg list -n tigergraph

NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster   3          3              2            docker.io/tigergraph/tigergraph-k8s:3.9.3   LoadBalancer   Normal           True               30m
```

And you can check that all pods of the cluster are running:

```bash
kubectl get pods -n tigergraph

NAME                                                     READY   STATUS      RESTARTS   AGE
test-cluster-0                                           1/1     Running     0          8m27s
test-cluster-1                                           1/1     Running     0          8m27s
test-cluster-2                                           1/1     Running     0          8m27s
test-cluster-init-job-fj8q2                              0/1     Completed   0          36m
tigergraph-operator-controller-manager-99b6fb86d-4kn49   2/2     Running     0          37m
```

#### Resume a paused cluster with updated resources

If you use above command to resume a paused cluster, the cluster will be resumed with the same resources as before. If you want to update the resources of the cluster, you can add other options to the command. For example, you can update the cpu and memory of the cluster while resuming it:

```bash
kubectl tg resume --cluster-name test-cluster -n tigergraph --cpu 4000m --memory 7Gi
```

Then the cluster will be resumed with the updated resources. You should be careful when you update the resources of the cluster. If you set the cpu or memory to a smaller value, the cluster may not work properly. If you set the cpu or memory to a larger value, the cluster may not be able to be scheduled to a node. So you should make sure that the updated resources are suitable for your cluster.

> [!NOTE]
> If you set wrong resources and the cluster cannot be scheduled to a node, you can pause the cluster and resume it with corrected resources again.

## Pause and Resume TigerGraph cluster by modifying TigerGraph CR

To pause or resume a TigerGraph cluster, you only need to modify the `.spec.pause` field of the TigerGraph CR. If you set the `pause` field to `true`, the cluster will be paused. If you set the `pause` field to `false`, the cluster will be resumed. But you have to make sure that the cluster is in `Normal` or `Resume` status when you pause it, and the cluster is in `Paused` status when you resume it. Otherwise the webhooks of the TigerGraph Operator will reject your request to change this field.

Here is an example of pausing a cluster:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  ha: 1
  image: docker.io/tigergraph/tigergraph-k8s:3.9.3
  imagePullPolicy: Always
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: YOUR_LICENSE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 1
  resources:
    requests:
      cpu: "4"
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
  # pause the cluster. when set as false, the cluster will be resumed
  pause: true
```

## Troubleshooting

### It takes too long to pause a cluster

When operator deletes all pods of the cluster, the pods will perform a graceful shutdown. So it may take a few minutes to pause the cluster. The maximum time is **6 minutes**. It's better to wait for the pods to be deleted instead of deleting them forcibly. If it takes more than 6 minutes to pause the cluster and the cluster is stuck in `PauseRoll` status, you can try to delete the pods forcibly.

### Cluster cannot resume successfully due to insufficient resources

When you resume a cluster, the TigerGraph Operator will create all pods of the cluster again. If there is not enough resources in the cluster, the pods may not be Pending forever:

```bash
kubectl tg resume -c test-cluster -n tigergraph

$ kubectl get pods -n tigergraph
NAME                                                     READY   STATUS      RESTARTS   AGE
test-cluster-0                                           0/1     Pending     0          5s
test-cluster-1                                           0/1     Pending     0          5s
test-cluster-2                                           0/1     Pending     0          5s
test-cluster-init-job-fj8q2                              0/1     Completed   0          3h41m
tigergraph-operator-controller-manager-99b6fb86d-4kn49   2/2     Running     0          3h42m
```

Use `kubectl describe pod` to check the reason why the pods are pending:

```bash
kubectl describe pods test-cluster-0  -n tigergraph

...
Events:
  Type     Reason            Age   From               Message
  ----     ------            ----  ----               -------
  Warning  FailedScheduling  63s   default-scheduler  0/1 nodes are available: 1 Insufficient cpu. preemption: 0/1 nodes are available: 1 No preemption victims found for incoming pod.
```

In this case, you can try to pause the cluster and resume the cluster with smaller resources request.

```bash
kubectl tg pause -c test-cluster -n tigergraph

kubectl tg resume -c test-cluster -n tigergraph --cpu 4 --memory 8Gi
```
