# TigerGraph Cluster management Troubleshooting

This document provides solutions for common issues that may arise during the management of a TigerGraph cluster in Kubernetes.

- [TigerGraph Cluster management Troubleshooting](#tigergraph-cluster-management-troubleshooting)
  - [Troubleshooting Steps for updating cluster](#troubleshooting-steps-for-updating-cluster)
    - [Potential failure of update](#potential-failure-of-update)
  - [Troubleshooting Steps for upgrading cluster](#troubleshooting-steps-for-upgrading-cluster)
    - [Potential failure of upgrading](#potential-failure-of-upgrading)
  - [Troubleshooting Steps for scaling cluster](#troubleshooting-steps-for-scaling-cluster)
    - [Expansion](#expansion)
      - [Potential failure of expansion](#potential-failure-of-expansion)
    - [Shrinking](#shrinking)
      - [Potential Causes](#potential-causes)
  - [Troubleshooting Steps for External Service Accessibility](#troubleshooting-steps-for-external-service-accessibility)
    - [TigerGraph GUI](#tigergraph-gui)
  - [Troubleshooting Steps for customizing and updating license/TigerGraph configurations](#troubleshooting-steps-for-customizing-and-updating-licensetigergraph-configurations)
    - [Customizing TigerGraph configurations during initialization](#customizing-tigergraph-configurations-during-initialization)
    - [Updating TigerGraph configurations and license when cluster is running](#updating-tigergraph-configurations-and-license-when-cluster-is-running)
  - [Troubleshooting for expanding storages on EKS](#troubleshooting-for-expanding-storages-on-eks)

## Troubleshooting Steps for updating cluster

- Verify the CPU and memory resources of the cluster Custom Resource (CR) have been updated:

  ```bash
  kubectl get tg test-cluster -o json -n tigergraph|jq .spec.resources

  {
    "requests": {
      "cpu": "2",
      "memory": "8Gi"
    }
  }
  ```

- Ensure the CPU and memory resources of the cluster's StatefulSet have been updated:

  ```bash
  kubectl get statefulset test-cluster -o json  -n tigergraph|jq .spec.template.spec.containers[0].resources
  {
    "requests": {
      "cpu": "2",
      "memory": "8Gi"
    }
  }
  ```

  If the resources haven't been updated, the cluster might be in another process, such as upgrading or scaling. In this case, check the cluster's status to determine the ongoing process. If the resource update is not initiated, you may need to wait for the last operation to complete:

  ```bash
  kubectl tg status --cluster-name test-cluster -n tigergraph

  Name:         test-cluster
  Namespace:    tigergraph
  Labels:       <none>
  Annotations:  <none>
  API Version:  graphdb.tigergraph.com/v1alpha1
  Kind:         TigerGraph
  Metadata:
    Creation Timestamp:  2023-02-27T07:47:28Z
    Generation:          2
    ......
  Spec:
    Image:              docker.io/tigergrah/tigergraph-k8s:3.8.0
    Image Pull Policy:  IfNotPresent
    Image Pull Secrets:
      Name:  tigergraph-image-pull-secret
    Init Job:
      Image:              docker.io/tigergrah/tigergraph-k8s-init:0.0.3
      Image Pull Policy:  IfNotPresent
      Image Pull Secrets:
        Name:  tigergraph-image-pull-secret
    Init TG Config:
      App Root:    /home/tigergraph/tigergraph/app
      Data Root:   /home/tigergraph/tigergraph/data
      Ha:          2
      License:     
      Log Root:    /home/tigergraph/tigergraph/log
      Password:    tigergraph
      Privatekey:  /home/tigergraph/.ssh/tigergraph_rsa
      Temp Root:   /home/tigergraph/tigergraph/tmp
      Username:    tigergraph
      Version:     3.8.0
    Listener:
      Type:    LoadBalancer
    Replicas:  3
    Resources:
      Requests:
        Cpu:     16
        Memory:  32Gi
    Storage:
      Type:  persistent-claim
      Volume Claim Template:
        Access Modes:
          ReadWriteOnce
        Resources:
          Requests:
            Storage:         10G
        Storage Class Name:  standard
        Volume Mode:         Filesystem
  Status:
    Cluster Topology:
      test-cluster-0:
        gui
        restpp
      test-cluster-1:
        gui
        restpp
      test-cluster-2:
        gui
        restpp
    Conditions:
      Last Probe Time:  2023-02-27T08:29:48Z
      Status:           Unknown
      Type:             UpdateRoll
      Last Probe Time:  2023-02-27T08:29:48Z
      Message:          Hello GSQL
      Status:           True
      Type:             test-cluster-0-rest-Available
      Last Probe Time:  2023-02-27T08:29:48Z
      Message:          Hello GSQL
      Status:           True
      Type:             test-cluster-1-rest-Available
      Last Probe Time:  2023-02-27T08:29:48Z
      Message:          Get "<http://test-cluster-2.test-cluster-internal-service.tigergraph.svc.cluster.local:9000/echo>": dial tcp 10.131.0.15:9000: connect: connection refused
      Status:           Unknown
      Type:             test-cluster-2-rest-Available
    Image:              docker.io/tigergrah/tigergraph-k8s:3.8.0
    Listener:
      Type:    LoadBalancer
    Replicas:  3
  Events:
    Type    Reason                    Age   From        Message
    ----    ------                    ----  ----        -------
    Normal  InitConfigMap created     42m   TigerGraph  Creating a new init configmap success
    Normal  EnvConfigMap created      42m   TigerGraph  Creating a new env configmap success
    Normal  external service created  42m   TigerGraph  Create a new external rest service success.
    Normal  external service created  42m   TigerGraph  Create a new external gui service success.
    Normal  internal service created  42m   TigerGraph  Create a new internal service success.
    Normal  StatefulSet created       42m   TigerGraph  Create a new StatefulSet success.
    Normal  Init job created          42m   TigerGraph  Create a new init job success.
    Normal  Update StatefulSet        4s    TigerGraph  Update a StatefulSet success.
  ```

### Potential failure of update

- If the updated resources (CPU or memory) exceed the available resources of the Kubernetes (K8s) cluster, the pods of the cluster will remain in a pending state. In such cases, you need to adjust the cluster configuration to allocate suitable resources:

  ```bash
  kubectl get pod -l app=test-cluster,tigergraph.com/cluster-pod=test-cluster -n tigergraph
  
  NAME             READY   STATUS    RESTARTS   AGE
  test-cluster-0   0/1     Pending   0          58s
  test-cluster-1   0/1     Pending   0          58s
  test-cluster-2   0/1     Pending   0          58s
  ```

- License updates via the K8s Operator are not supported yet. You can perform a license update using the following methods:
  - Use the `kubectl tg update` --license ${LICENSE} command (supported in version 0.0.7).
  - Use the License function in the GUI to access the Admin Portal for updates.
  - Use `gadmin config entry license` to update the license in the pod. Refer to the TigerGraph Docs for more information.

- Resizing Persistent Volumes (PV) via the K8s Operator is not yet supported. Please refer to the [manual documentation](../07-reference/expand-persistent-volume.md) for instructions.

## Troubleshooting Steps for upgrading cluster

- Verify that the Docker image of the cluster CR has been updated:

  ```bash
  kubectl get tg test-cluster -o json -n tigergraph|jq .spec.image
  
  "docker.io/tigergrah/tigergraph-k8s:3.8.0"
  ```

- Ensure the Docker image of the cluster's StatefulSet has been updated:

  ```bash
  kubectl get statefulset test-cluster -o json  -n tigergraph|jq .spec.template.spec.containers[0].image

  "docker.io/tigergrah/tigergraph-k8s:3.9.0"
  ```

  If the images haven't been updated, the cluster might be in another process, such as resource updating or scaling. Check the cluster's status to determine the ongoing process. If the cluster upgrade is not initiated, you may need to wait for the last operation to complete:

  ```bash
  kubectl tg status --cluster-name test-cluster -n tigergraph
  ```

- Ensure the upgrade process completes successfully. Verify the following:
  
  - The cluster status should be "Normal," and the actual image version should match the expected version:

    ```bash
    kubectl tg status --cluster-name test-cluster -n tigergraph
    ```

  - Check the status and version of the TigerGraph cluster by executing the following commands:

    ```bash
    kubectl exec -it test-cluster-0 -n tigergraph -- /home/tigergraph/tigergraph/app/cmd/gadmin version
    kubectl exec -it test-cluster-0 -n tigergraph -- /home/tigergraph/tigergraph/app/cmd/gadmin status -v
    ```

### Potential failure of upgrading

- If you set a misconfiguration upgrade version, the pods of the cluster will be in ErrImagePull or ImagePullBackOff state, then you need to update the cluster configuration to correct the version.

  ```bash
  kubectl get pod -l tigergraph.com/cluster-pod=test-cluster -n tigergraph

  NAME             READY   STATUS             RESTARTS   AGE
  test-cluster-0   0/1     ErrImagePull       0          63s
  test-cluster-1   0/1     ImagePullBackOff   0          63s
  test-cluster-2   0/1     ImagePullBackOff   0          63s

  kubectl tg update --cluster-name test-cluster --version ${CORRECT_VERSION} -n tigergraph
  ```

- Rollback version of TigerGraph is not supported

  Operator Version 0.0.9 (Please note that this issue persists across all versions of the operator and may continue to be encountered in future releases)

  A unique scenario arises during the upgrading process when attempting to downgrade the TigerGraph cluster from a higher version to a lower version. For instance, moving from version 3.9.2 to 3.9.1. While the rolling update process will initially proceed successfully, an issue arises during the execution of the UpgradePost job due to the downgrade being disabled. Consequently, this prevents the ability to revert to a previous version for recovery purposes.

  Should the need to revert to the previous version arise, a two-step process is necessary. First, the cluster must be deleted, and subsequently recreated with the identical cluster name. This process enables the rollback to the desired version and facilitates the restoration of normal operations.

## Troubleshooting Steps for scaling cluster

### Expansion

- Ensure that the pods of the cluster have been scaled up to the expected size:

  ```bash
  # test-cluster is the name of the cluster
  # The example below tries to scale the cluster size from 3 to 5
  kubectl get pod -l tigergraph.com/cluster-pod=test-cluster -n tigergraph

  NAME             READY   STATUS              RESTARTS   AGE
  test-cluster-0   1/1     Running             0          17m
  test-cluster-1   1/1     Running             0          17m
  test-cluster-2   1/1     Running             0          17m
  test-cluster-3   0/1     ContainerCreating   0          8s
  test-cluster-4   0/1     ContainerCreating   0          7s
  ```

- Ensure the expansion job is running or has completed successfully:

  ```bash
  # replace test-cluster with you tigergraph cluster name
  kubectl get job -l job-name=test-cluster-expand-job -n tigergraph

  NAME                      COMPLETIONS   DURATION   AGE
  test-cluster-expand-job   0/1           4m13s      4m13s
  ```

  If the expansion job fails, we can check out the logs of the job.

  ```bash
  # replace test-cluster with you tigergraph cluster name
  kubectl get pod -l job-name=test-cluster-expand-job -n tigergraph

  NAME                            READY   STATUS    RESTARTS   AGE
  test-cluster-expand-job-6jk42   1/1     Running   0          5m38s

  kubectl logs test-cluster-expand-job-6jk42 -n tigergraph
  Could not create directory '/.ssh' (Permission denied).
  Failed to add the host to the list of known hosts (/.ssh/known_hosts).
  [   Info] Starting EXE
  [   Info] Starting CTRL
  [   Info] Generating config files to all machines
  [   Info] Successfully applied configuration change. Please restart services to make it effective immediately.
  [   Info] Initializing KAFKA
  [   Info] Starting EXE
  [   Info] Starting CTRL
  [   Info] Starting ZK ETCD DICT KAFKA ADMIN GSE NGINX GPE RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
  [   Info] Applying config
  [Warning] No difference from staging config, config apply is skipped.
  [   Info] Successfully applied configuration change. Please restart services to make it effective immediately.
  [   Info] Cluster is initialized successfully
  [   Info] Configuration has been changed. Please use 'gadmin config apply' to persist the changes.
  [Warning] No difference from staging config, config apply is skipped.
  [   Info] Successfully applied configuration change. Please restart services to make it effective immediately.
  [   Info] Stopping ZK ETCD DICT KAFKA ADMIN GSE NGINX GPE RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
  [   Info] Stopping CTRL
  [   Info] Stopping EXE
  [   Info] Starting EXE
  [   Info] Starting CTRL
  [   Info] Starting ZK ETCD DICT KAFKA ADMIN GSE NGINX GPE RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
  Could not create directory '/.ssh' (Permission denied).
  Failed to add the host to the list of known hosts (/.ssh/known_hosts).
  [   Info] Starting EXE
  [   Info] Starting CTRL
  [   Info] Generating config files to all machines
  [   Info] Successfully applied configuration change. Please restart services to make it effective immediately.
  [   Info] Initializing KAFKA
  [   Info] Starting EXE
  [   Info] Starting CTRL
  [   Info] Starting ZK ETCD DICT KAFKA ADMIN GSE NGINX GPE RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
  [   Info] Applying config
  [Warning] No difference from staging config, config apply is skipped.
  [   Info] Successfully applied configuration change. Please restart services to make it effective immediately.
  [   Info] Cluster is initialized successfully
  [   Info] Configuration has been changed. Please use 'gadmin config apply' to persist the changes.
  [Warning] No difference from staging config, config apply is skipped.
  [   Info] Successfully applied configuration change. Please restart services to make it effective immediately.
  [   Info] Stopping ZK ETCD DICT KAFKA ADMIN GSE NGINX GPE RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
  [   Info] Stopping CTRL
  [   Info] Stopping EXE
  [   Info] Starting EXE
  [   Info] Starting CTRL
  [   Info] Starting ZK ETCD DICT KAFKA ADMIN GSE NGINX GPE RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
  Could not create directory '/.ssh' (Permission denied).
  Failed to add the host to the list of known hosts (/.ssh/known_hosts).
  hostlist is: m4:test-cluster-3.test-cluster-internal-service,m5:test-cluster-4.test-cluster-internal-service
  You have entered:

  m4 : test-cluster-3.test-cluster-internal-service
  m5 : test-cluster-4.test-cluster-internal-service

  Replication number will be changed to 2. The previous value is 2
  [   Info] [Mon Feb 27 09:34:54 UTC 2023] Validate cluster change requests
  [   Info] [Mon Feb 27 09:34:54 UTC 2023] Export gsql/gui data
  [   Info] [Mon Feb 27 09:34:54 UTC 2023] Export graph data, time cost will be proportional to data size
  ```

- Ensure that the expansion operation has been performed successfully:

  - Check the status of the cluster CR:

    ```bash
    kubectl tg status --cluster-name test-cluster -n tigergraph
    ```

  - Check the cluster status by executing gadmin:

    ```bash
    kubectl exec -it test-cluster-0 -n tigergraph -- /home/tigergraph/tigergraph/app/cmd/gadmin status -v
    ```

#### Potential failure of expansion

- If the K8s cluster's resources (CPU or Memory) are insufficient to expand the TigerGraph cluster, you have two options based on the Operator version:

  - For Operator versions 0.0.3 and earlier, recreate the cluster with the same cluster name, which will load the remaining cluster data for recovery.

  - For Operator versions 0.0.4 and higher, update the size to match the K8s cluster's available resources or reset the cluster to the previous configuration.

- The expanding job fails again after retrying three times

  - If you back up the cluster before expansion, you can restore it with the backup package directly.

  - If there is no backup package, it has to do a complex manual recovery, so we’d better backup the cluster before expansion.

- Repeated pod restart after shrink/expand operations since GPE is not ready in-time causes healthy check failed. After Expansion/Shrink ends, services such as GPE may take a while to switch from warmup to running. This process may exceed 40 seconds, which is the upper limit of the health check. If the services of one or more TG nodes, but not all TG nodes, are still in the warmup state, their pods will be removed and rebuilt. This may cause repeated pod restarts. There will be about 5-12 times, depending on the amount of data, until the TG Service status of all pods is normal. At present, at the Operator level, it is impossible to limit such waiting time, or perceive the maximum warmup time, and it is also impossible to configure such waiting time arbitrarily.
solution

  - Wait for all services to return to normal status before taking any action.

    ```bash
    kubectl describe pods tg-cluster-1 
    
    Events:
    Type     Reason     Age   From     Message
    ----     ------     ----  ----     -------
    Warning  Unhealthy  31m   kubelet  Readiness probe failed: service status of GPE_2#1 is Down, exit error.
    [Warning] Status might not be up-to-date due to sync data error; failed to get latest offset, err is "kafka server: Request was for a topic or partition that does not exist on this broker."
    Warning  Unhealthy  30m                kubelet  Readiness probe failed: service status of GPE_2#1 is Down, exit error.
    Warning  Unhealthy  29m (x9 over 74m)  kubelet  Readiness probe failed: service status of GPE_2#1 should not be Warmup, exit error
    kubectl describe pods tg-cluster-0
    Events:
    Type     Reason     Age                From     Message
    ----     ------     ----               ----     -------
    Warning  Unhealthy  30m (x5 over 31m)  kubelet  Readiness probe failed: service status of GPE_1#1 is Down, exit error.
    Warning  Unhealthy  30m (x7 over 72m)  kubelet  Readiness probe failed: service status of GPE_1#1 should not be Warmup, exit error
    kubectl describe pods tg-cluster-2
    Events:
    Type     Reason     Age                From     Message
    ----     ------     ----               ----     -------
    Warning  Unhealthy  30m (x5 over 31m)  kubelet  Readiness probe failed: service status of GPE_3#1 is Down, exit error.
    Warning  Unhealthy  30m (x7 over 78m)  kubelet  Readiness probe failed: service status of GPE_3#1 should not be Warmup, exit error
    kubectl describe pods tg-cluster-3 | tail -10
    Events:
    Type     Reason     Age   From     Message
    ----     ------     ----  ----     -------
    Warning  Unhealthy  31m   kubelet  Readiness probe failed: service status of GPE_4#1 is Down, exit error.
    [Warning] Status might not be up-to-date due to sync data error; failed to get latest offset, err is "kafka server: Request was for a topic or partition that does not exist on this broker."
    Warning  Unhealthy  31m                kubelet  Readiness probe failed: service status of GPE_4#1 is Down, exit error.
    Warning  Unhealthy  30m (x8 over 80m)  kubelet  Readiness probe failed: service status of GPE_4#1 should not be Warmup, exit error
    ```

### Shrinking

- Ensure that the shrinking job is running or has completed successfully:

  ```bash
  # replace test-cluster with you tigergraph cluster name 
  kubectl get job test-cluster-shrink-pre-job -n tigergraph

  NAME                          COMPLETIONS   DURATION   AGE
  test-cluster-shrink-pre-job   0/1           21s        21s
  ```

  If the shrinking job fails, you can check the job logs:

  ```bash
  kubectl get pod -l job-name=test-cluster-shrink-pre-job -n tigergraph

  NAME                                READY   STATUS    RESTARTS   AGE
  test-cluster-shrink-pre-job-jzlhm   1/1     Running   0          2m11s

  kubectl logs test-cluster-shrink-pre-job-jzlhm -n tigergraph

  Warning: Permanently added 'test-cluster-0.test-cluster-internal-service.tigergraph' (ED25519) to the list of known hosts.
  hostlist is: m4:test-cluster-3.test-cluster-internal-service,m5:test-cluster-4.test-cluster-internal-service
  You have entered:

  m5 : test-cluster-4.test-cluster-internal-service
  m4 : test-cluster-3.test-cluster-internal-service

  Replication number will be changed to 2. The previous value is 2
  [   Info] [Mon Feb 27 10:06:21 UTC 2023] Validate cluster change requests
  [   Info] [Mon Feb 27 10:06:21 UTC 2023] Export gsql/gui data
  [   Info] [Mon Feb 27 10:06:21 UTC 2023] Export graph data, time cost will be proportional to data size
  ```

- Ensure that the shrinking operation has been performed successfully:

  - Check the status of the cluster CR:

    ```bash
    kubectl tg status --cluster-name test-cluster -n tigergraph
    ```

  - Check the cluster status by executing gadmin:

    ```bash
    kubectl exec -it test-cluster-0 -n tigergraph -- /home/tigergraph/tigergraph/app/cmd/gadmin status -v
    ```

#### Potential Causes

- The shrinking job fails again after retrying three times

  - If you have a backup of the cluster before shrinking, you can restore it directly using the backup package.

  - If there is no backup package, manual recovery is a complex process, so it's recommended to backup the cluster before shrinking.

## Troubleshooting Steps for External Service Accessibility

### TigerGraph GUI

> [!IMPORTANT]
> TigerGraph 3.6.3 doesn't support session affinity in HA env

In TigerGraph 3.6.3, Session Affinity is not supported. Direct external access through the Service (SVC) may lead to the following issues:

1. The TigerGraph GUI application may repeatedly prompt an authentication error after login because authentication occurs on one node, and requests sent to the GUI may be directed to other nodes.
2. Sending HTTP/HTTPS requests to the GUI service may yield similar results.

    ```bash
    curl -H "Cookie: TigerGraphApp=5eaf48c3-cb0b-4c78-9f27-7251c53dc0bc"    http://34.29.233.2:14240/api/loading-jobs/ldbc_snb/meta
    {"error":true,"message":"You are not authorized to use this API.","results":{}}%
    ```

- Solution

1. Use the latest version of TigerGraph that supports Session Affinity.

2. To enable Session Affinity, edit the service (SVC) configuration:

    ```bash
    kubectl edit svc tg-cluster-1-gui-external-service         
    # edit the sessionAffinity as below
    sessionAffinity: ClientIP
    sessionAffinityConfig:
    clientIP:
        timeoutSeconds: 1800

    ```

## Troubleshooting Steps for customizing and updating license/TigerGraph configurations

### Customizing TigerGraph configurations during initialization

The init-job may fail due to wrong TigerGraph configurations. Check the logs of the init-job to find the root cause.

```bash
kubectl get pod -l job-name=test-cluster-init-job -n tigergraph
```

The output should be similar to the following:

```bash
NAME                         READY   STATUS    RESTARTS   AGE
test-cluster-init-job-2j4x4   0/1    Error     0          2m
```

Use `kubectl logs` to check the logs of the init-job:

```bash
kubectl logs test-cluster-init-job-2j4x4 -n tigergraph
```

You may see logs like the following:

```bash
Server:         10.96.0.10
Address:        10.96.0.10:53

Name:   test-cluster-0.test-cluster-internal-service.tigergraph.svc.cluster.local
Address: 10.244.0.36


Server:         10.96.0.10
Address:        10.96.0.10:53


Name:   test-cluster-1.test-cluster-internal-service.tigergraph.svc.cluster.local
Address: 10.244.0.37

Server:         10.96.0.10
Address:        10.96.0.10:53


Name:   test-cluster-2.test-cluster-internal-service.tigergraph.svc.cluster.local
Address: 10.244.0.38

Warning: Permanently added '[test-cluster-0.test-cluster-internal-service.tigergraph]:10022' (ED25519) to the list of known hosts.
HOST_LIST: [{"Hostname":"test-cluster-0.test-cluster-internal-service","ID":"m1","Region":""},{"Hostname":"test-cluster-1.test-cluster-internal-service","ID":"m2","Region":""},{"Hostname":"test-cluster-2.test-cluster-internal-service","ID":"m3","Region":""}]
[Warning] For HA setup, there might be some nodes unused since replication factor 2 is not a factor of machine number 3
the bucket bit is 5
false
[Wed Dec 20 06:39:02 UTC 2023] set config entry Controller.ServiceManager.AutoRestart to true
Log mode is prod
[Wed Dec 20 06:39:02 UTC 2023] start setting TigerGraph configurations
[  Error] config entry WRONG_CONFIG not found
[  Error] ParameterErr (failed to set one or more config entries)
```

The error message `[  Error] config entry WRONG_CONFIG not found` means that you have set a wrong configuration entry in the CR. Check the CR and make sure the configuration entry is correct according to the [Configuration Parameters](https://docs.tigergraph.com/tigergraph-server/current/reference/configuration-parameters). Once you have corrected the configuration entry, you can update the CR and wait for the init-job to be recreated automatically.

### Updating TigerGraph configurations and license when cluster is running

You can updae TigerGraph configurations and license when the cluster is running. This will trigger a config-update job to update the configurations and license in the cluster. The config-update job may fail if you set a wrong configuration entry or wrong license in the CR. Check the logs of the config-update job to find the root cause.

```bash
kubectl get pod -l job-name=test-cluster-config-update-job -n tigergraph
```

The output should be similar to the following:

```bash
NAME                                   READY   STATUS   RESTARTS   AGE
test-cluster-config-update-job-9p6bt   0/1     Error    0          18s
```

Use `kubectl logs` to check the logs of the config-update-job:

```bash
kubectl logs test-cluster-config-update-job-9p6bt -n tigergraph
```

If the output is like

```bash
Warning: Permanently added '[test-cluster-0.test-cluster-internal-service.tigergraph]:10022' (ED25519) to the list of known hosts.
[Wed Dec 20 07:07:22 UTC 2023] Start updating TigerGraph configurations
[  Error] config entry WRONG_CONFIG not found
[  Error] ParameterErr (failed to set one or more config entries)
```

You need to check the CR and make sure the configuration entry is correct according to the [Configuration Parameters](https://docs.tigergraph.com/tigergraph-server/current/reference/configuration-parameters).

If the output is like

```bash
Warning: Permanently added '[test-cluster-0.test-cluster-internal-service.tigergraph]:10022' (ED25519) to the list of known hosts.
[Wed Dec 20 07:12:43 UTC 2023] Start setting license
[  Error] ExternalError (failed to set license; token contains an invalid number of segments)
```

You need to check the CR and make sure the license is correct.

Once you correct TigerGraph configurations and license in the CR, the config-update job will be recreated automatically.

## Troubleshooting for expanding storages on EKS

Refer to [the limitations of EBS volume modifications](https://docs.aws.amazon.com/ebs/latest/userguide/modify-volume-requirements.html#elastic-volumes-limitations), after modifying a volume, you must wait **at least six hours** and ensure that the volume is in the `in-use` or `available` state before you can modify the same volume.

So when you expand the storage size again less than six hours after the last modification, the cluster will be stuck in the `StorageExpanding,Unknown` status until EBS completes the modification.
