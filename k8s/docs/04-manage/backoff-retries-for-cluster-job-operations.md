# Customize the backoff retries for cluster job operations

Starting with version 1.5.0, the Kubernetes Operator for TigerGraph introduces support for customizing backoff retries for cluster job operations.

This document provides detailed, step-by-step instructions for configuring backoff retries to meet your operational requirements.

- [Customize the backoff retries for cluster job operations](#customize-the-backoff-retries-for-cluster-job-operations)
  - [Introduction](#introduction)
    - [Calculating the Delay Time for Job Retries](#calculating-the-delay-time-for-job-retries)
  - [Customize the backoff retries using kubectl-tg plugin](#customize-the-backoff-retries-using-kubectl-tg-plugin)
  - [Customize the backoff retries using TigerGraph CR](#customize-the-backoff-retries-using-tigergraph-cr)
  - [Troubleshooting](#troubleshooting)
    - [How to check the status of job backoff retry status](#how-to-check-the-status-of-job-backoff-retry-status)
      - [Status After Reaching the Maximum Retry Attempts](#status-after-reaching-the-maximum-retry-attempts)
      - [Resetting Job Backoff Attempts](#resetting-job-backoff-attempts)
    - [How to make a failed job retry again after reaching the maximum retry attempts](#how-to-make-a-failed-job-retry-again-after-reaching-the-maximum-retry-attempts)
    - [Job Backoff retry attempts doesn't work](#job-backoff-retry-attempts-doesnt-work)

## Introduction  

In previous versions, if [debug mode](debug-mode.md) was not enabled, all cluster operation jobs would retry indefinitely in the event of failure. This behavior made it impossible to disable job retries when the cluster was in an abnormal state. To address this limitation, support for configuring backoff retries has been introduced.  

Before diving into the detailed configuration steps, it’s important to understand some basics. The retry mechanism uses a **global minimum retry duration** and a **maximum retry duration** to determine the exponential delay time for each job retry.  

Additionally, you can configure a specific number of retry attempts for each type of cluster operation job. The corresponding job key names are listed below:  

| **Job Type**            | **Job Key**        |  
|--------------------------|--------------------|  
| Cluster initialization   | `initialize-job`   |  
| Cluster config update    | `config-update-job`|  
| Cluster upgrading        | `upgrade-pre-job`  |  
| Cluster upgrading        | `upgrade-post-job` |  
| Cluster expansion        | `expand-job`       |  
| Cluster shrinking        | `shrink-pre-job`   |  
| Cluster HA update        | `ha-update-job`    |  
| Cluster pausing          | `pause-pre-job`    |  
| Cluster deletion         | `delete-pre-job`   |  

The **job key** must match one of the above values. If it doesn’t, the TigerGraph Operator webhook will reject the request.  

### Calculating the Delay Time for Job Retries  

To calculate the delay time for each job retry, let’s take the cluster initialization job as an example. Assume the following parameters:  

- **Maximum retry attempts**: 5  
- **Minimum retry duration**: 1 minute  
- **Maximum retry duration**: 30 minutes  

The retry durations for each attempt would be as follows:  
`1m`, `2m`, `4m`, `8m`, `16m`, and `30m`.  

Note that the delay times increase exponentially. However, since the **maximum retry duration** is capped at 30 minutes, the final delay time remains at 30 minutes instead of continuing to 32 minutes.

> [!IMPORTANT]  
> The current job retry attempts are reset under the following conditions:  
> - **Primary cluster status changes**: For example, if the cluster initialization fails twice during the initialization process, the value of the field `status.jobBackoffTimes` will be reset to zero when the primary cluster status changes. 
> - **Sub-job status changes**: In cases where a cluster operation consists of multiple sub-jobs (e.g., during cluster upgrading), the value of `status.jobBackoffTimes` will also reset to zero when a sub-job executes successfully.

You can manually reset the value of the field `status.jobBackoffTimes` by updating any of the following configuration parameters:  

- **Maximum retry attempts**
- **Minimum retry duration**  
- **Maximum retry duration**

## Customize the backoff retries using kubectl-tg plugin

Add three new options in kubectl-tg plugin to support configuring those configurations:

```bash
  --min-job-retry-duration :
                      set the min duration between two retries of a cluster operation job, the format is like "5s","10m","1h","1h20m5s"
  --max-job-retry-duration :
                      set the max duration between two retries of a cluster operation job, the format is like "5s","10m","1h","1h20m5s"
  --max-job-retry-times : set max times of retry for cluster operation jobs
```

To customize the backoff retries during cluster initialization, run the following command:

```bash
kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} --private-key-secret ${YOUR_SSH_KEY_SECRET_NAME} --size 4 --ha 2 --version 4.1.2 --license ${LICENSE} \
--storage-class standard --storage-size 10G --cpu 6000m --memory 12Gi --listener-type LoadBalancer \
--min-job-retry-duration '1m' --max-job-retry-duration '30m' --max-job-retry-times 'expand-job=6,shrink-pre-job=6,initialize-job=3'  --namespace ${YOUR_NAMESPACE}
```

To update cluster job configuration within an existing cluster

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME}  --min-job-retry-duration '15s' --max-job-retry-duration '10m' --max-job-retry-times 'expand-job=3,shrink-pre-job=4,initialize-job=3'  --namespace ${YOUR_NAMESPACE}
```

To remove cluster job configuration within an existing cluster

```bash
kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME}  --min-job-retry-duration null --max-job-retry-duration null --max-job-retry-times null  --namespace ${YOUR_NAMESPACE}
```

> [!IMPORTANT]  
> If you don't configure those configurations explicitly, the default value of them will be used:
> - **`--min-job-retry-duration`: 1m**
> - **`--max-job-retry-duration`: 60m**
> - **The maximum retry attempts of all jobs is 10**

## Customize the backoff retries using TigerGraph CR

To configure backoff retries for a TigerGraph cluster, you only need to modify the `.spec.clusterJobConfig` field of the TigerGraph CR. Minimum retry duration is configured by `.spec.clusterJobConfig.minRetryDuration`, maximum retry duration is configured by `.spec.clusterJobConfig.maxRetryDuration`, and the maximum retry times is configured by a map field `.spec.clusterJobConfig.maxRetry`.

> [!IMPORTANT]  
> If you don't configure those configurations explicitly, the default value of them will be used:
> - **`.spec.clusterJobConfig.minRetryDuration`: 1m**
> - **`.spec.clusterJobConfig.maxRetryDuration`: 60m**
> - **The maximum retry attempts of all jobs is 10**

Here is an example of configuring backoff retries for cluster job operations:

```YAML
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  annotations:
    tigergraph.com/debug-mode: "false"
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:4.2.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: xxxxxx
  privateKeyName: ssh-key-secret
  replicas: 4
  listener:
    type: LoadBalancer
  resources:
    limits:
      cpu: "6"
      memory: 12Gi
    requests:
      cpu: "6"
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
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
  clusterJobConfig:
    minRetryDuration: 60s
    maxRetryDuration: 30m
    maxRetryTimes:
      "upgrade-pre-job": 3
      "upgrade-post-job": 4
      "initialize-job": 3
      "expand-job": 5
      "shrink-pre-job": 5
      "config-update-job": 3
      "ha-update-job": 5
      "pause-pre-job": 2
      "delete-pre-job": 3
```

## Troubleshooting

### How to check the status of job backoff retry status

If a cluster operation job fails, you can check the status of the TigerGraph cluster Custom Resource (CR) to determine the current job retry attempts.

Below is an example of checking the status of a TigerGraph cluster CR.

The initial job backoff retry configuration is as follows:

```YAML
  clusterJobConfig:
    minRetryDuration: 60s
    maxRetryDuration: 10m
    maxRetryTimes:
      "upgrade-pre-job": 2
      "upgrade-post-job": 2
      "initialize-job": 3
      "expand-job": 3
      "shrink-pre-job": 4
```

In this example, we simulate a cluster job failure to check the retry status of the `initialize-job`. This job is configured to retry up to 3 times, resulting in a total of 4 job execution attempts.

When the `initialize-job` fails for the first time, the following example output is generated:

```bash
$ kubectl describe get pods
NAME                                                      READY   STATUS    RESTARTS   AGE
test-cluster-0                                            1/1     Running   0          3m8s
test-cluster-1                                            1/1     Running   0          3m8s
test-cluster-2                                            1/1     Running   0          3m8s
test-cluster-3                                            1/1     Running   0          3m8s
test-cluster-init-job-2hnlv                               0/1     Error     0          72s
test-cluster-init-job-4ntrz                               0/1     Error     0          14s
test-cluster-init-job-r9bdj                               0/1     Error     0          46s
tigergraph-operator-controller-manager-66c7c475c4-5xv2s   2/2     Running   0          5m
```

The TigerGraph CR status is as follows:

```bash
Status:
  Cluster Job Config:
    Max Retry Duration:  10m
    Max Retry Times:
      Expand - Job:          3
      Initialize - Job:      3
      Shrink - Pre - Job:    4
      Upgrade - Post - Job:  2
      Upgrade - Pre - Job:   2
    Min Retry Duration:      60s
  Conditions:
    Last Transition Time:  2025-01-24T08:35:54Z
    Message:               Job test-cluster-init-job execution failed 1 times, backoff 1m0s seconds
    Reason:                ClusterInitializePostFalse
    Status:                False
    Type:                  InitializePost
  Image:                   docker.io/tigergraph/tigergraph-k8s:4.2.0
  Pod Init Labels:
    tigergraph.com/cluster-name:  test-cluster
    tigergraph.com/cluster-pod:   test-cluster
  Replicas:                       4
  Storage:
    Tg Data Size:  10G
Events:
  Type    Reason                       Age    From        Message
  ----    ------                       ----   ----        -------
  Normal  SuccessfulCreateConfigMap    3m48s  TigerGraph  Create a new init ConfigMap success
  Normal  SuccessfulCreateService      3m48s  TigerGraph  Create a new service test-cluster-internal-service success
  Normal  SuccessfulCreateConfigMap    3m48s  TigerGraph  Create a new env ConfigMap success
  Normal  SuccessfulCreateStatefulSet  3m48s  TigerGraph  Create StatefulSet success
  Normal  SuccessfulCreateJob          112s   TigerGraph  Create a new initialize job success
```

The field `status.conditions[0].message` indicates the job execution count and the current backoff duration.

#### Status After Reaching the Maximum Retry Attempts

Once the job retry attempts reach the maximum number of retries, the TigerGraph CR status output changes as follows:

```bash
  Conditions:
    Last Transition Time:  2025-01-24T08:46:13Z
    Message:               Job test-cluster-init-job execution failed 4 times, max retry exceeded
    Reason:                ClusterInitializePostFalse
    Status:                False
    Type:                  InitializePost
  Image:                   docker.io/tigergraph/tigergraph-k8s:4.2.0
  Job Backoff Times:       3
  Pod Init Labels:
    tigergraph.com/cluster-name:  test-cluster
    tigergraph.com/cluster-pod:   test-cluster
  Replicas:                       4
  Storage:
    Tg Data Size:  10G
Events:
  Type    Reason                       Age                  From        Message
  ----    ------                       ----                 ----        -------
  Normal  SuccessfulCreateConfigMap    17m                  TigerGraph  Create a new init ConfigMap success
  Normal  SuccessfulCreateService      17m                  TigerGraph  Create a new service test-cluster-internal-service success
  Normal  SuccessfulCreateConfigMap    17m                  TigerGraph  Create a new env ConfigMap success
  Normal  SuccessfulCreateStatefulSet  17m                  TigerGraph  Create StatefulSet success
  Normal  SuccessfulCreateJob          5m15s (x4 over 15m)  TigerGraph  Create a new initialize job success
  Normal  SuccessfulDeleteJob          5m15s (x3 over 13m)  TigerGraph  Delete old initialize job success
```

The field `status.jobBackoffTimes` is updated to 3, and the `status.conditions[0].message` reflects:
Job test-cluster-init-job execution failed 4 times, max retry exceeded.

At this point, the job will pause and will no longer retry.

```bash
kubectl get pods -n tigergraph
NAME                                                      READY   STATUS    RESTARTS   AGE
test-cluster-0                                            1/1     Running   0          20m
test-cluster-1                                            1/1     Running   0          20m
test-cluster-2                                            1/1     Running   0          20m
test-cluster-3                                            1/1     Running   0          20m
test-cluster-init-job-955vp                               0/1     Error     0          7m33s
test-cluster-init-job-9lqhf                               0/1     Error     0          7m2s
test-cluster-init-job-tpfsg                               0/1     Error     0          7m54s
```

#### Resetting Job Backoff Attempts

After resolving the cluster initialization issue manually, you can update the parameters of `spec.clusterJobConfig`. The TigerGraph Operator will reset the value of status.jobBackoffTimes and rerun the `initialize-job`.

### How to make a failed job retry again after reaching the maximum retry attempts

If you want to enable the job retrying after the maximum retry attempts reached, you can update the parameters of `spec.clusterJobConfig`. The TigerGraph Operator will reset the value of status.jobBackoffTimes and rerun the current cluster operation job.

### Job Backoff retry attempts doesn't work

There are two potential reasons why Job Backoff retry attempts may not function as expected:

1. The TigerGraph Operator is not running properly. Please check the status of the TigerGraph Operator pods.
2. Debug mode for the TigerGraph cluster has been enabled. You need to disable debug mode to allow the job retry to continue.
