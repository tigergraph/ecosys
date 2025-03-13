# Enable debug mode of TigerGraph cluster

Starting with version 1.2.0, the Kubernetes Operator for TigerGraph supports debug mode.

The debug mode of TigerGraph cluster is a vital feature that stops the recreation of failed Jobs and allows users to manually debug and fix extreme errors that cannot be fixed by retrying.

> [!NOTE]
> Starting with version 1.5.0, the Kubernetes Operator for TigerGraph supports debug mode for continuous pod restarts caused by an unrecoverable PostStartHookError.

Debug mode can be toggled on or off at any time, as long as the TigerGraph cluster remains accessible.

When debug mode is enabled, users can view the output logs of failed jobs, analyze the root causes of failures, and connect to the cluster for manual debugging and recovery, without concern that the failed jobs will be deleted in the reconciliation loop.

- [Enable debug mode of TigerGraph cluster](#enable-debug-mode-of-tigergraph-cluster)
  - [Enable debug mode of TigerGraph cluster by kubectl tg plugin](#enable-debug-mode-of-tigergraph-cluster-by-kubectl-tg-plugin)
    - [Prerequisites](#prerequisites)
    - [Utilizing `kubectl tg` to enable debug mode of TigerGraph cluster](#utilizing-kubectl-tg-to-enable-debug-mode-of-tigergraph-cluster)
      - [Enable debug mode during cluster initialization](#enable-debug-mode-during-cluster-initialization)
      - [Enable debug mode on a running cluster](#enable-debug-mode-on-a-running-cluster)
  - [Enable debug mode of TigerGraph cluster by modifying TigerGraph CR](#enable-debug-mode-of-tigergraph-cluster-by-modifying-tigergraph-cr)
  - [Use cases](#use-cases)
    - [Disable automatic retries for failed cluster operation jobs](#disable-automatic-retries-for-failed-cluster-operation-jobs)
    - [Disable continuous pod restarts caused by an unrecoverable PostStartHookError](#disable-continuous-pod-restarts-caused-by-an-unrecoverable-poststarthookerror)
  - [Debugging commands and resources](#debugging-commands-and-resources)

## Enable debug mode of TigerGraph cluster by kubectl tg plugin

### Prerequisites

The successful execution of the `kubectl tg create|update --debug-mode` commands requires that you have installed the `kubectl tg` command line tool. For more information, see [Install kubectl-tg plugin](../02-get-started/get_started.md#install-kubectl-tg-plugin). Additionally, you must create your cluster as a prerequisite step.

### Utilizing `kubectl tg` to enable debug mode of TigerGraph cluster

```bash
  --debug-mode :      set true to enable debug mode for TigerGraph cluster, default as false
```

> [!NOTE]
> The accepted values for `--debug-mode` are `true` or `false`. Any other values will be considered invalid and rejected by the Operator.

#### Enable debug mode during cluster initialization

To enable debug mode when the cluster starts, set it to true during cluster creation with `kubectl tg`. Here is an example:

```bash
kubectl tg create --debug-mode true --cluster-name test-cluster -n tigergraph
```

The default value of debug mode is `false`, so it will be off unless specified otherwise during initialization. However, you can still choose to explicitly set it:

```bash
kubectl tg create --debug-mode false --cluster-name test-cluster -n tigergraph
```

#### Enable debug mode on a running cluster

To enable debug mode after the cluster is up, run the following command:

```bash
kubectrl tg update --debug-mode true --cluster-name test-cluster -n tigergraph
```

To disable debug mode, run the same command with `false`:

```bash
kubectrl tg update --debug-mode false --cluster-name test-cluster -n tigergraph
```

## Enable debug mode of TigerGraph cluster by modifying TigerGraph CR

The reserved annotation key for debug mode is `tigergraph.com/debug-mode`. To enable debug mode, set `tigergraph.com/debug-mode` to `true`. To disable it, either set `tigergraph.com/debug-mode` to `false` or remove the annotation.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  annotations:
    # turn off debug mode. when set as true, debug mode will be active
    tigergraph.com/debug-mode: "false"
spec:
  ha: 2
  image: docker.io/tigergraph/tigergraph-k8s:3.10.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: YOUR_LICENSE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 3
  resources:
    requests:
      cpu: "3"
      memory: 8Gi
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
```

> [!NOTE]
> The accepted values for `tigergraph.com/debug-mode` are `true` or `false`. Any other values will be considered invalid and rejected by the Operator.

## Use cases

> [!IMPORTANT]
> Enabling debug mode will disable cluster job retries and pod restarts in the event of recoverable errors, such as network instability.
> Therefore, it is highly recommended to disable debug mode after resolving any unrecoverable issues.

### Disable automatic retries for failed cluster operation jobs

The Kubernetes Operator for TigerGraph automatically retries failed cluster operation jobs, such as cluster initialization, upgrades, expansions, shrinking, and config update, in certain cases.

However, if automatic retries cannot recover a failed cluster operation due to unexpected errors, you can disable retries by enabling debug mode and manually recovering the operation.

Example: Disabling Automatic Retries for Failed Cluster Operation Job of configure update

```bash
$ kubectl get pods
NAME                                                      READY   STATUS      RESTARTS   AGE
test-cluster-0                                            1/1     Running     0          8h
test-cluster-config-update-job-rvlzc                      0/1     Error       0          3m43s
test-cluster-config-update-job-t76wb                      0/1     Error       0          3m21s
test-cluster-config-update-job-w8kw5                      0/1     Error       0          2m50s
test-cluster-init-job-qd8ql                               0/1     Completed   0          8h
tigergraph-operator-controller-manager-5697fbcc68-7wf8f   2/2     Running     0          8h
```

From the output of the pods for config update job, the failed job retried for multiple times, causing unnecessary resource consumption and potential downtime.

```bash
$ kubectl get tg test-cluster -o yaml|yq .status
clusterSize: 1
conditions:
  - lastTransitionTime: "2025-03-03T08:57:07Z"
    message: Job test-cluster-config-update-job execution failed 3 times, backoff 4m0s seconds
    reason: ClusterConfigUpdateFalse
    status: "False"
    type: ConfigUpdate
ha: 1
hashBucketInBit: 5
image: docker.io/tigergraph/tigergraph-k8s:4.1.2
jobBackoffTimes: 2
licenseHash: bfeb5653f8287743f3c5a452d99d031a
listener:
  type: LoadBalancer
podInitLabels:
  tigergraph.com/cluster-name: test-cluster
  tigergraph.com/cluster-pod: test-cluster
replicas: 1
storage:
  tgDataSize: 10G
```

Additionally, the status of the TigerGraph cluster shows that the configuration update job has failed three times and will retry again in four minutes.

If you want to immediately disable the job retry, you can enable debug mode and manually address the issue.

Once the issue is resolved, you can retry the cluster operation job by disabling debug mode.

### Disable continuous pod restarts caused by an unrecoverable PostStartHookError

In addition to disabling automatic retries for failed cluster operation jobs, debug mode can also be used to prevent continuous pod restarts caused by an unrecoverable PostStartHookError.

Example: Disabling Continuous Pod Restarts Due to PostStartHookError

During a rolling upgrade, unexpected errors may occur, causing some pods to become stuck with a PostStartHookError. If debug mode is disabled (false), the pod will repeatedly retry and eventually transition to the CrashLoopBackOff state.

```bash
$ kubectl get pods
NAME                                                      READY   STATUS               RESTARTS   AGE
test-cluster-0                                            1/1     Running              0          8m26s
test-cluster-1                                            1/1     Running              0          8m26s
test-cluster-2                                            0/1     PostStartHookError   0          2m57s
test-cluster-init-job-t4jrw                               0/1     Completed            0          8m7s
tigergraph-operator-controller-manager-5697fbcc68-2js5h   2/2     Running              0          9m34s
```

Example log output from the `test-cluster-2` pod:

```bash
$ kubectl logs test-cluster-2
Defaulted container "tigergraph" out of: tigergraph, init-tigergraph (init)
[Thu Feb 20 00:55:41 UTC 2025] tigergraph container is running now
[Thu Feb 20 00:55:56 UTC 2025] start local services
[   Info] Starting EXE
[   Info] Starting CTRL
[   Info] Starting ZK
[Thu Feb 20 00:56:29 UTC 2025] failed to execute "gadmin start all --local" for 1 times, exit code 127
[   Info] Starting EXE
[   Info] Starting CTRL
[   Info] Starting ZK
[Thu Feb 20 00:57:19 UTC 2025] failed to execute "gadmin start all --local" for 2 times, exit code 127
[   Info] Starting EXE
[   Info] Starting CTRL
[   Info] Starting ZK
[Thu Feb 20 00:58:09 UTC 2025] failed to execute "gadmin start all --local" for 3 times, exit code 127
[Thu Feb 20 00:58:29 UTC 2025] failed to executing "gadmin start all --local" on current node
[Thu Feb 20 00:58:29 UTC 2025] cluster debug mode is false
[Thu Feb 20 00:58:29 UTC 2025] stop local services
[   Info] Stopping ZK ETCD DICT KAFKA ADMIN NGINX RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
[   Info] Stopping CTRL
[   Info] Stopping EXE
[Thu Feb 20 00:58:30 UTC 2025] stop the services of current node completely
```

We can enable debug mode by setting it to true and log into the pod to manually recover the failed service.

```bash
$ kubectl get pods
NAME                                                      READY   STATUS      RESTARTS        AGE
test-cluster-0                                            1/1     Running     0               14m
test-cluster-1                                            1/1     Running     0               14m
test-cluster-2                                            0/1     Running     2 (3m27s ago)   9m12s
test-cluster-init-job-t4jrw                               0/1     Completed   0               14m
tigergraph-operator-controller-manager-5697fbcc68-2js5h   2/2     Running     0               15m
```

Example log output from the test-cluster-2 pod after enabling debug mode:

```bash
$ kubectl logs test-cluster-2 
Defaulted container "tigergraph" out of: tigergraph, init-tigergraph (init)
[Thu Feb 20 01:01:36 UTC 2025] tigergraph container is running now
[Thu Feb 20 01:01:51 UTC 2025] start local services
[   Info] Starting EXE
[   Info] Starting CTRL
[   Info] Starting ZK
[Thu Feb 20 01:02:24 UTC 2025] failed to execute "gadmin start all --local" for 1 times, exit code 127
[   Info] Starting EXE
[   Info] Starting CTRL
[   Info] Starting ZK
[Thu Feb 20 01:03:14 UTC 2025] failed to execute "gadmin start all --local" for 2 times, exit code 127
[   Info] Starting EXE
[   Info] Starting CTRL
[   Info] Starting ZK
[Thu Feb 20 01:04:04 UTC 2025] failed to execute "gadmin start all --local" for 3 times, exit code 127
[Thu Feb 20 01:04:24 UTC 2025] failed to executing "gadmin start all --local" on current node
[Thu Feb 20 01:04:24 UTC 2025] cluster debug mode is true
[Thu Feb 20 01:04:24 UTC 2025] skipping local service start failure as cluster debug mode is on. You need to resolve the issue manually with command "gadmin start all --local".
```

## Debugging commands and resources

- [TigerGraph Cluster management Troubleshooting](../05-troubleshoot/cluster-management.md)
- [Comprehensive Kubectl Command Reference (external resource)](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands)
