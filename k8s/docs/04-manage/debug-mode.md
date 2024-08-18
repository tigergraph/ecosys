# Enable debug mode of TigerGraph cluster

Starting with version 1.2.0, the Kubernetes Operator for TigerGraph supports debug mode.

The debug mode of TigerGraph cluster is a vital feature that stops the recreation of failed Jobs and allows users to manually debug and fix extreme errors that cannot be fixed by retrying.

Debug mode can be toggled on or off at any time, as long as the TigerGraph cluster remains accessible.

When debug mode is enabled, users can view the output logs of failed jobs, analyze the root causes of failures, and connect to the cluster for manual debugging and recovery, without concern that the failed jobs will be deleted in the reconciliation loop.

- [Enable debug mode of TigerGraph cluster](#enable-debug-mode-of-tigergraph-cluster)
  - [Enable debug mode of TigerGraph cluster by kubectl tg plugin](#enable-debug-mode-of-tigergraph-cluster-by-kubectl-tg-plugin)
    - [Prerequisites](#prerequisites)
    - [Utilizing `kubectl tg` to enable debug mode of TigerGraph cluster](#utilizing-kubectl-tg-to-enable-debug-mode-of-tigergraph-cluster)
      - [Enable debug mode during cluster initialization](#enable-debug-mode-during-cluster-initialization)
      - [Enable debug mode on a running cluster](#enable-debug-mode-on-a-running-cluster)
  - [Enable debug mode of TigerGraph cluster by modifying TigerGraph CR](#enable-debug-mode-of-tigergraph-cluster-by-modifying-tigergraph-cr)
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
xw
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
  imagePullPolicy: Always
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

# Debugging commands and resources

- [TigerGraph Cluster management Troubleshooting](../05-troubleshoot/cluster-management.md)
- [Comprehensive Kubectl Command Reference (external resource)](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands)