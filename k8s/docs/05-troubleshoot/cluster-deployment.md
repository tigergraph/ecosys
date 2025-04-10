# TigerGraph Cluster Deployment Troubleshooting

This document provides guidance on troubleshooting common issues encountered during the deployment of a TigerGraph cluster in Kubernetes.

- [TigerGraph Cluster Deployment Troubleshooting](#tigergraph-cluster-deployment-troubleshooting)
  - [Troubleshooting Steps](#troubleshooting-steps)
    - [Check the pods of the TG Cluster](#check-the-pods-of-the-tg-cluster)
    - [Check the initialize job of TG Cluster](#check-the-initialize-job-of-tg-cluster)
  - [Troubleshooting Cases](#troubleshooting-cases)
    - [Conflict port for NodePort Listener type](#conflict-port-for-nodeport-listener-type)
    - [TigerGraph Status is empty and Pods are not created](#tigergraph-status-is-empty-and-pods-are-not-created)
    - [The TigerGraph Pod was killed and restarted because its ephemeral local storage usage exceeded the total limit set for the containers](#the-tigergraph-pod-was-killed-and-restarted-because-its-ephemeral-local-storage-usage-exceeded-the-total-limit-set-for-the-containers)
    - [The TigerGraph CR was created successfully, but the TigerGraph pods and other dependent resources are not created](#the-tigergraph-cr-was-created-successfully-but-the-tigergraph-pods-and-other-dependent-resources-are-not-created)
    - [Webhook Certificate Verification Failure in TigerGraph Cluster creation](#webhook-certificate-verification-failure-in-tigergraph-cluster-creation)

## Troubleshooting Steps

In the following steps, it is assumed that the operator has already been successfully installed within the `tigergraph` namespace, and that the cluster has been named `test-cluster`. However, please ensure to make appropriate adjustments based on your specific circumstances and environment.

### Check the pods of the TG Cluster

Ensure that all the pods of the cluster are running, and the READY figure is 1/1. Starting from Operator version 0.0.7, sidecarContainers are supported. If you have X sideContainers, the READY figure should be (1+X)/(1+X).

```bash
kubectl get pod -l tigergraph.com/cluster-name=test-cluster,tigergraph.com/cluster-pod=test-cluster -n tigergraph

NAME             READY   STATUS    RESTARTS   AGE
test-cluster-0   1/1     Running   0          11h
test-cluster-1   1/1     Running   0          11h
test-cluster-2   1/1     Running   0          11h
```

If the status of a Pod is not `Running`, it might be in `Pending` or `PullImageError` state. In such cases, you can check detailed information about the specific pod using:

```bash
kubectl describe pod test-cluster-0 -n tigergraph
```

- Insufficient CPU or Memory

  If a pod is in a `Pending` state, it might be due to insufficient CPU or memory resources. You can identify this issue by checking the pod's status:
  
  ```bash
  kubectl get pod -l app=test-cluster,tigergraph.com/cluster-pod=test-cluster -n tigergraph

  NAME             READY   STATUS    RESTARTS   AGE
  test-cluster-0   0/1     Pending   0          58s
  test-cluster-1   0/1     Pending   0          58s
  test-cluster-2   0/1     Pending   0          58s
  ```

  Inspect the details of the pending pod to find the root cause at the bottom of the output:

  ```bash
  kubectl describe pod test-cluster-0 -n tigergraph

  Name:           test-cluster-0
  Namespace:      tigergraph
  Priority:       0
  Node:           <none>
  Labels:         app=test-cluster
                  controller-revision-hash=test-cluster-6c8cc9c557
                  statefulset.kubernetes.io/pod-name=test-cluster-0
                  tigergraph.com/cluster-pod=test-cluster
  Annotations:    openshift.io/scc: privileged
  Status:         Pending
  IP:             
  IPs:            <none>
  Controlled By:  StatefulSet/test-cluster
  Containers:
    tg:
      Image:       docker.io/tigergrah/tigergraph-k8s:3.8.0
      Ports:       9000/TCP, 14240/TCP, 22/TCP
      Host Ports:  0/TCP, 0/TCP, 0/TCP
      Requests:
        cpu:      16
        memory:   32Gi
        ......
      Medium:     
      SizeLimit:  <unset>
    kube-api-access-mnsw5:
      Type:                    Projected (a volume that contains injected data from multiple sources)
      TokenExpirationSeconds:  3607
      ConfigMapName:           kube-root-ca.crt
      ConfigMapOptional:       <nil>
      DownwardAPI:             true
      ConfigMapName:           openshift-service-ca.crt
      ConfigMapOptional:       <nil>
  QoS Class:                   Burstable
  Node-Selectors:              <none>
  Tolerations:                 node.kubernetes.io/memory-pressure:NoSchedule op=Exists
                              node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                              node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
  Events:
    Type     Reason            Age                  From               Message
    ----     ------            ----                 ----               -------
    Warning  FailedScheduling  65s (x3 over 2m23s)  default-scheduler  0/4 nodes are available: 1 node(s) had taint {node-role.kubernetes.io/master: }, that the pod didn't tolerate, 3 Insufficient cpu, 3 Insufficient memory. 
  ```

  You may encounter messages like `Insufficient CPU` or `Insufficient memory`. In this case, you should adjust the resource allocation for the cluster using the following command:

  ```bash
  kubectl tg update --cluster-name test-cluster --cpu 4 --memory 8Gi -n tigergraph
  ```

- Nodes don’t match nodeSelector/affinity

  Starting from Operator version 0.0.7, nodeSelector, affinity, and tolerations are supported. If you provide rules in the Custom Resource (CR), and your pod is in a `Pending` state, you can check for the following events:

  ```bash
  kubectl describe pod test-cluster-0 -n tigergraph
  ```

  Look for events like:

  ```bash
  Events:
    Type     Reason             Age                   From                Message
    ----     ------             ----                  ----                -------
    Warning  FailedScheduling   101s (x2 over 2m17s)  default-scheduler   0/6 nodes are available: 1 Insufficient cpu, 5 node(s) didn't match Pod's node affinity/selector. preemption: 0/6 nodes are available: 1 No preemption victims found for incoming pod, 5 Preemption is not helpful for scheduling.
  ```

  This indicates that the nodes cannot meet your affinity or nodeSelector rules. You can update your rules using the following command:

  ```bash
  kubectl tg update --cluster-name test-cluster -n tigergraph --affinity affinity-config.yaml
  ```

- Incorrect docker image

  If the TigerGraph Docker image version is incorrect, the pod status may be `ErrImagePull` or `ImagePullBackOff`. You can identify this issue by checking the pod status:

  ```bash
  kubectl get pod -l tigergraph.com/cluster-pod=test-cluster -n tigergraph
  
  NAME             READY   STATUS             RESTARTS   AGE
  test-cluster-0   0/1     ErrImagePull       0          63s
  test-cluster-1   0/1     ImagePullBackOff   0          63s
  test-cluster-2   0/1     ImagePullBackOff   0          63s
  ```

  Check the detailed error by examining the pod's events:

  ```bash
  kubectl describe pod test-cluster-0 -n tigergraph
  
  Name:         test-cluster-0
  Namespace:    tigergraph
  Priority:     0
  Node:         tg-k8s-openshift-777-rdj74-worker-d-pvrm2/10.0.128.2
  Start Time:   Mon, 27 Feb 2023 03:15:39 +0000
  Labels:       app=test-cluster
                controller-revision-hash=test-cluster-598bdbb6cb
                statefulset.kubernetes.io/pod-name=test-cluster-0
                tigergraph.com/cluster-pod=test-cluster
  .......
  Controlled By:  StatefulSet/test-cluster
  Containers:
    tg:
      Container ID:   
      Image:          docker.io/tigergrah/tigergraph-k8s:3.8.5
      Image ID:       
      Ports:          9000/TCP, 14240/TCP, 22/TCP
      Host Ports:     0/TCP, 0/TCP, 0/TCP
      State:          Waiting
        Reason:       ImagePullBackOff
      Ready:          False
      Restart Count:  0
      Requests:
        cpu:      2
        memory:   8Gi
        ......
      Environment:
        SERVICE_NAME:  <set to the key 'service.headless.name' of config map 'test-cluster-env-config'>  Optional: false
        POD_PREFIX:    <set to the key 'pod.prefix' of config map 'test-cluster-env-config'>             Optional: false
        NAMESPACE:     <set to the key 'namespace' of config map 'test-cluster-env-config'>              Optional: false
        CLUSTER_SIZE:  <set to the key 'cluster_size' of config map 'test-cluster-env-config'>           Optional: false
      Mounts:
        /home/tigergraph/tigergraph/data from tg-data (rw)
        /tmp/init_tg_cfg from config-volume (rw,path="init_tg_cfg")
        /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-vskns (ro)
  Conditions:
    Type              Status
    Initialized       True 
    Ready             False 
    ContainersReady   False 
    PodScheduled      True 
  Volumes:
    tg-data:
      Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
      ClaimName:  tg-data-test-cluster-0
      ReadOnly:   false
    config-volume:
      Type:      ConfigMap (a volume populated by a ConfigMap)
      Name:      test-cluster-init-config
      Optional:  false
    probe-data:
      Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
      Medium:     
      SizeLimit:  <unset>
    kube-api-access-vskns:
      Type:                    Projected (a volume that contains injected data from multiple sources)
      TokenExpirationSeconds:  3607
      ConfigMapName:           kube-root-ca.crt
      ConfigMapOptional:       <nil>
      DownwardAPI:             true
      ConfigMapName:           openshift-service-ca.crt
      ConfigMapOptional:       <nil>
  QoS Class:                   Burstable
  Node-Selectors:              <none>
  Tolerations:                 node.kubernetes.io/memory-pressure:NoSchedule op=Exists
                              node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                              node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
  Events:
    Type     Reason                  Age                  From                     Message
    ----     ------                  ----                 ----                     -------
    Normal   Scheduled               2m38s                default-scheduler        Successfully assigned tigergraph/test-cluster-0 to tg-k8s-openshift-777-rdj74-worker-d-pvrm2
    Normal   SuccessfulAttachVolume  2m34s                attachdetach-controller  AttachVolume.Attach succeeded for volume "pvc-96c90faf-3019-416a-ace9-200502f67b65"
    Normal   AddedInterface          2m30s                multus                   Add eth0 [10.130.0.33/23] from openshift-sdn
    Normal   Pulling                 71s (x4 over 2m29s)  kubelet                  Pulling image "docker.io/tigergrah/tigergraph-k8s:3.8.5"
    Warning  Failed                  71s (x4 over 2m29s)  kubelet                  Failed to pull image "docker.io/tigergrah/tigergraph-k8s:3.8.5": rpc error: code = Unknown desc = reading manifest 3.8.5 in docker.io/tigergrah/tigergraph-k8s: manifest unknown: manifest unknown
    Warning  Failed                  71s (x4 over 2m29s)  kubelet                  Error: ErrImagePull
    Warning  Failed                  59s (x6 over 2m29s)  kubelet                  Error: ImagePullBackOff
    Normal   BackOff                 44s (x7 over 2m29s)  kubelet                  Back-off pulling image "docker.io/tigergrah/tigergraph-k8s:3.8.5"
  ```

  Look for messages indicating issues with the image, such as `Error: ErrImagePull` You should correct the image version using the following command:

  ```bash
  kubectl tg update --cluster-name test-cluster --version 3.9.0 -n tigergraph
  ```

- Incorrect PVC with non-existent StorageClass

  If you specified a non-existent or unusable StorageClass when creating a cluster, the cluster's pods may be stuck in a `Pending` state. To diagnose the issue, first check the pod statuses:

  ```bash
  kubectl get pod -l app=test-cluster,tigergraph.com/cluster-pod=test-cluster -n tigergraph

  NAME             READY   STATUS    RESTARTS   AGE
  test-cluster-0   0/1     Pending   0          2m16s
  test-cluster-1   0/1     Pending   0          2m16s
  test-cluster-2   0/1     Pending   0          2m16s
  ```

  If the pods are in a `Pending` state, inspect the details of one of the pods to find the root cause:

  ```bash
  kubectl describe pod test-cluster-0 -n tigergraph

  Name:           test-cluster-0
  Namespace:      tigergraph
  Priority:       0
  Node:           <none>
  Labels:         app=test-cluster
                  controller-revision-hash=test-cluster-598bdbb6cb
                  statefulset.kubernetes.io/pod-name=test-cluster-0
                  tigergraph.com/cluster-pod=test-cluster
  Annotations:    openshift.io/scc: privileged
  Status:         Pending
  IP:             
  IPs:            <none>
  Controlled By:  StatefulSet/test-cluster
  Containers:
    tg:
      Image:       docker.io/tigergrah/tigergraph-k8s:3.8.5
      Ports:       9000/TCP, 14240/TCP, 22/TCP
      Host Ports:  0/TCP, 0/TCP, 0/TCP
      Requests:
        cpu:      2
        memory:   8Gi
        ......
      Environment:
        SERVICE_NAME:  <set to the key 'service.headless.name' of config map 'test-cluster-env-config'>  Optional: false
        POD_PREFIX:    <set to the key 'pod.prefix' of config map 'test-cluster-env-config'>             Optional: false
        NAMESPACE:     <set to the key 'namespace' of config map 'test-cluster-env-config'>              Optional: false
        CLUSTER_SIZE:  <set to the key 'cluster_size' of config map 'test-cluster-env-config'>           Optional: false
      Mounts:
        /home/tigergraph/tigergraph/data from tg-data (rw)
        /tmp/init_tg_cfg from config-volume (rw,path="init_tg_cfg")
        /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-8zb5z (ro)
  Conditions:
    Type           Status
    PodScheduled   False 
  Volumes:
    tg-data:
      Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
      ClaimName:  tg-data-test-cluster-0
      ReadOnly:   false
    config-volume:
      Type:      ConfigMap (a volume populated by a ConfigMap)
      Name:      test-cluster-init-config
      Optional:  false
    probe-data:
      Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
      Medium:     
      SizeLimit:  <unset>
    kube-api-access-8zb5z:
      Type:                    Projected (a volume that contains injected data from multiple sources)
      TokenExpirationSeconds:  3607
      ConfigMapName:           kube-root-ca.crt
      ConfigMapOptional:       <nil>
      DownwardAPI:             true
      ConfigMapName:           openshift-service-ca.crt
      ConfigMapOptional:       <nil>
  QoS Class:                   Burstable
  Node-Selectors:              <none>
  Tolerations:                 node.kubernetes.io/memory-pressure:NoSchedule op=Exists
                              node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                              node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
  Events:
    Type     Reason            Age                 From               Message
    ----     ------            ----                ----               -------
    Warning  FailedScheduling  58s (x4 over 3m8s)  default-scheduler  0/4 nodes are available: 4 pod has unbound immediate PersistentVolumeClaims.
  ```

  From the events of the pod, you can find out that the root cause is 0/4 nodes are available: 4 pod has unbound immediate PersistentVolumeClaims , this error indicates that the StorageClass is not existed or the capacity of PV is insufficient.

  Check the storage configuration of the cluster.

  ```bash
  kubectl get tg test-cluster -n tigergraph -o json|jq .spec.storage

  {
    "type": "persistent-claim",
    "volumeClaimTemplate": {
      "accessModes": [
        "ReadWriteOnce"
      ],
      "resources": {
        "requests": {
          "storage": "10G"
        }
      },
      "storageClassName": "test-storage-class",
      "volumeMode": "Filesystem"
    }
  }
  ```

  Check the PVC status of the cluster

  ```bash
  kubectl get pvc -l tigergraph.com/cluster-name=test-cluster -n tigergraph
  ```

  Ensure the STORAGECLASS exists and the capacity of PV is insufficient

  ```bash
  kubectl get STORAGECLASS

  NAME                 PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
  standard (default)   kubernetes.io/gce-pd    Delete          WaitForFirstConsumer   true                   34m
  standard-csi         pd.csi.storage.gke.io   Delete          WaitForFirstConsumer   true
  ```

  For this test case, the STORAGECLASS test-storage-class does not exist, you should recreate the cluster with a certain STORAGECLASS name in the above list.

  If there are not any pods in the above step, you can check the status of StatefulSet:

  ```bash
  kubectl get statefulset test-cluster -n tigergraph

  NAME           READY   AGE
  test-cluster   3/3     11h
  ```

- ebs csi driver not installed (only on EKS)

  Since some EKS versions do not install aws-ebs-csi-driver plugin by default, if you encounter the following issue when creating TigerGraph cluster with the dynamic persistent volume, you need to check it first.

  After deploying the TigerGraph cluster, all of the TigerGraph pods are in Pending status, and all of the PVC attached to the StatefulSet of TigerGraph are also in Pending status.

  ```bash
  # please replace the cluster name and namespace with yours.
  kubectl get pods -l tigergraph.com/cluster-name=test-cluster --namespace tigergraph

  NAME                                                      READY   STATUS    RESTARTS   AGE
  test-cluster-0                                            0/1     Pending   0          32s
  test-cluster-1                                            0/1     Pending   0          32s
  test-cluster-2                                            0/1     Pending   0          32s

  kubectl get pvc -l tigergraph.com/cluster-name=test-cluster --namespace tigergraph

  NAME                     STATUS    VOLUME   CAPACITY   ACCESS MODES   STORAGECLASS   AGE
  tg-data-test-cluster-0   Pending                                      gp2            37s
  tg-data-test-cluster-1   Pending                                      gp2            37s
  tg-data-test-cluster-2   Pending                                      gp2            37s

  kubectl describe pvc -l tigergraph.com/cluster-name=test-cluster --namespace tigergraph

  Name:          tg-data-test-cluster-0
  Namespace:     tigergraph
  StorageClass:  gp2
  Status:        Pending
  Volume:
  Labels:        tigergraph.com/cluster-name=test-cluster
                tigergraph.com/cluster-pod=test-cluster
  Annotations:   volume.beta.kubernetes.io/storage-provisioner: ebs.csi.aws.com
                volume.kubernetes.io/selected-node: ip-172-31-20-181.us-west-1.compute.internal
                volume.kubernetes.io/storage-provisioner: ebs.csi.aws.com
  Finalizers:    [kubernetes.io/pvc-protection]
  Capacity:
  Access Modes:
  VolumeMode:    Filesystem
  Used By:       test-cluster-0
  Events:
    Type    Reason                Age                    From                         Message
    ----    ------                ----                   ----                         -------
    Normal  WaitForFirstConsumer  8m9s                   persistentvolume-controller  waiting for first consumer to be created before binding
    Normal  ExternalProvisioning  2m35s (x25 over 8m9s)  persistentvolume-controller  waiting for a volume to be created, either by external provisioner "ebs.csi.aws.com" or manually created by system administrator
  ```

  check and install aws-ebs-csi-driver with following commands:

  > [!WARNING]
  > Please ensure that the IAM role for the Amazon EBS CSI driver has been created. You can refer to the official AWS documentation [Creating the Amazon EBS CSI driver IAM role](https://docs.aws.amazon.com/eks/latest/userguide/csi-iam-role.html) for detailed instructions.

  ```bash
  kubectl get deployment ebs-csi-controller -n kube-system

  aws eks create-addon --cluster-name ${YOUR_K8S_CLUSTER_NAME} --addon-name aws-ebs-csi-driver
  ```

### Check the initialize job of TG Cluster

If you've successfully created the StatefulSet and cluster pods for your TigerGraph cluster but encounter anomalies in the cluster's status, such as liveness and readiness staying unready for an extended period, you can follow these steps to troubleshoot the issue.

- Ensure initialize job has been created

  ```bash
  kubectl get job -l tigergraph.com/cluster-job=test-cluster-init-job -n tigergraph
  ```

- If initialize job is exist and the COMPLETIONS of the job are not 1/1, you need to check the pod status of the job

  ```bash
  kubectl get pod -l job-name=test-cluster-init-job -n tigergraph

  NAME                          READY   STATUS      RESTARTS   AGE
  test-cluster-init-job-p9lqr   0/1     Completed   0          12h
  ```

  If the pod status is incomplete, investigate further by checking the logs of the error pod (if it exists) to identify the root cause of the initialization job failure:

  ```bash
  # It equals kubectl logs test-cluster-init-job -n tigergraph
  kubectl logs -l job-name=test-cluster-init-job -n tigergraph

  Defaulted container "cluster-installer" out of: cluster-installer, init-tigergraph (init)
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
  ```

  Examine the logs for any error messages that might provide insights into the failure. It's essential to address these issues to ensure a successful initialization.

- Check the cluster status by logging into a pod

  If all the previous steps have been completed successfully, you can log into one of the cluster pods to check the detailed errors of the cluster using the `gadmin status -v` command. This can help identify any ongoing issues with the cluster:

  ```bash
  kubectl tg connect --cluster-name test-cluster -n tigergraph

  tigergraph@test-cluster-0:~$ gadmin status
  +--------------------+-------------------------+-------------------------+
  |    Service Name    |     Service Status      |      Process State      |
  +--------------------+-------------------------+-------------------------+
  |       ADMIN        |         Online          |         Running         |
  |        CTRL        |         Online          |         Running         |
  |        DICT        |         Online          |         Running         |
  |        ETCD        |         Online          |         Running         |
  |        EXE         |         Online          |         Running         |
  |        GPE         |         Warmup          |         Running         |
  |        GSE         |         Warmup          |         Running         |
  |        GSQL        |         Online          |         Running         |
  |        GUI         |         Online          |         Running         |
  |        IFM         |         Online          |         Running         |
  |       KAFKA        |         Online          |         Running         |
  |     KAFKACONN      |         Online          |         Running         |
  |    KAFKASTRM-LL    |         Online          |         Running         |
  |       NGINX        |         Online          |         Running         |
  |       RESTPP       |         Online          |         Running         |
  |         ZK         |         Online          |         Running         |
  +--------------------+-------------------------+-------------------------+
  ```

  The gadmin status command provides detailed information about the status of various TigerGraph services and processes. Review the output to check for any services or processes that are not running correctly.

  If the liveness check of the pod continues to fail, you can use a single command to get the cluster status:

  ```bash
  kubectl exec -it test-cluster-0 -n tigergraph -- /home/tigergraph/tigergraph/app/cmd/gadmin status -v

  +--------------------+-------------------------+-------------------------+-------------------------+
  |    Service Name    |     Service Status      |      Process State      |       Process ID        |
  +--------------------+-------------------------+-------------------------+-------------------------+
  |      ADMIN#1       |         Online          |         Running         |          44484          |
  |      ADMIN#2       |         Online          |         Running         |          9536           |
  |      ADMIN#3       |         Online          |         Running         |          3099           |
  |       CTRL#1       |         Online          |         Running         |           79            |
  |       CTRL#2       |         Online          |         Running         |           637           |
  |       CTRL#3       |         Online          |         Running         |           74            |
  |       DICT#1       |         Online          |         Running         |          43741          |
  |       DICT#2       |         Online          |         Running         |          8504           |
  |       DICT#3       |         Online          |         Running         |          2347           |
  |       ETCD#1       |         Online          |         Running         |          43731          |
  |       ETCD#2       |         Online          |         Running         |          8494           |
  |       ETCD#3       |         Online          |         Running         |          2337           |
  |       EXE_1        |         Online          |         Running         |           59            |
  |       EXE_2        |         Online          |         Running         |           512           |
  |       EXE_3        |         Online          |         Running         |           56            |
  |      GPE_1#1       |         Warmup          |         Running         |          44534          |
  |      GPE_1#2       |         Warmup          |         Running         |          9586           |
  |      GSE_1#1       |         Warmup          |         Running         |          44495          |
  |      GSE_1#2       |         Warmup          |         Running         |          9547           |
  |       GSQL#1       |         Online          |         Running         |          44802          |
  |       GSQL#2       |         Online          |         Running         |          9756           |
  |       GSQL#3       |         Online          |         Running         |          3385           |
  |       GUI#1        |         Online          |         Running         |          45096          |
  |       GUI#2        |         Online          |         Running         |          9919           |
  |       GUI#3        |         Online          |         Running         |          3698           |
  |       IFM#1        |         Online          |         Running         |          44997          |
  |       IFM#2        |         Online          |         Running         |          9874           |
  |       IFM#3        |         Online          |         Running         |          3573           |
  |      KAFKA#1       |         Online          |         Running         |           240           |
  |      KAFKA#2       |         Online          |         Running         |          1097           |
  |      KAFKA#3       |         Online          |         Running         |           238           |
  |    KAFKACONN#1     |         Online          |         Running         |          44615          |
  |    KAFKACONN#2     |         Online          |         Running         |          9663           |
  |    KAFKACONN#3     |         Online          |         Running         |          3196           |
  |   KAFKASTRM-LL_1   |         Online          |         Running         |          44562          |
  |   KAFKASTRM-LL_2   |         Online          |         Running         |          9611           |
  |   KAFKASTRM-LL_3   |         Online          |         Running         |          3142           |
  |      NGINX#1       |         Online          |         Running         |          44499          |
  |      NGINX#2       |         Online          |         Running         |          9553           |
  |      NGINX#3       |         Online          |         Running         |          3110           |
  |      RESTPP#1      |         Online          |         Running         |          44540          |
  |      RESTPP#2      |         Online          |         Running         |          9596           |
  |      RESTPP#3      |         Online          |         Running         |          3127           |
  |        ZK#1        |         Online          |         Running         |           108           |
  |        ZK#2        |         Online          |         Running         |           729           |
  |        ZK#3        |         Online          |         Running         |           103           |
  +--------------------+-------------------------+-------------------------+-------------------------+
  ```

## Troubleshooting Cases

### Conflict port for NodePort Listener type

If you encounter conflicts with port allocation when creating or updating a cluster with `LISTENER_TYPE=NodePort` and specified `nginx-node-port` values that conflict with in-use ports, you will receive an error message. To resolve this issue, specify available ports for these services:

```bash
# Create a cluster with --listener-type NodePort, and there is a tg cluster using the default port 30090, 30240
kubectl tg create --cluster-name tg-cluster-2 --listener-type NodePort --nginx-node-port 30240

# Check the CR, it indicates the provided port is already allocated. 
kubectl describe tigergraph.graphdb.tigergraph.com/tg-cluster-2 
Events:
  Type     Reason                                  Age                 From        Message
  ----     ------                                  ----                ----        -------
  Normal   Create init ConfigMap                   20s                 TigerGraph  Create a new init ConfigMap success
  Normal   Create env ConfigMap                    20s                 TigerGraph  Create a new env ConfigMap success
  Warning  Failed to create external rest service  10s (x11 over 20s)  TigerGraph  Failed to create external service: Service "tg-cluster-2-rest-external-service" is invalid: spec.ports[0].nodePort: Invalid value: 30240: provided port is already allocated  
```

### TigerGraph Status is empty and Pods are not created

This issue may happen when:

1. Upgrade operator by `kubectl tg upgrade` command
2. Resize Node Pool to 0 or delete Node Pool in GKE/EKS clusters

When you create a TigerGraph CR, and run `kubectl get tg -n $NAMESPACE`, you will find that the status of the cluster is empty:

```bash
$ kubectl get tg -n tigergraph -w
NAME            REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                             SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster                                           docker.io/tigergraph/tigergraph-k8s:3.9.3   LoadBalancer                                       1m
```

And when you run `kubectl get pods -n $NAMESPACE`, no pod has been created for this CR. The possible reason is that the reconcile is not triggered due to an issue of controller-runtime package. The log of operator will be like:

```bash
I0117 06:53:09.436291       1 request.go:690] Waited for 1.045716414s due to client-side throttling, not priority and fairness, request: GET:https://10.12.0.1:443/apis/autoscaling/v2?timeout=32s
2024-01-17T06:53:10Z    INFO    controller-runtime.metrics      Metrics server is starting to listen    {"addr": "127.0.0.1:8080"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a mutating webhook  {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraph", "path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraph"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraph"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a validating webhook        {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraph", "path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraph"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraph"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a mutating webhook  {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraphBackup", "path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraphbackup"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraphbackup"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a validating webhook        {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraphBackup", "path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraphbackup"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraphbackup"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a mutating webhook  {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraphBackupSchedule", "path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraphbackupschedule"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraphbackupschedule"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a validating webhook        {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraphBackupSchedule", "path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraphbackupschedule"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraphbackupschedule"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a mutating webhook  {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraphRestore", "path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraphrestore"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraphrestore"}
2024-01-17T06:53:10Z    INFO    controller-runtime.builder      Registering a validating webhook        {"GVK": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraphRestore", "path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraphrestore"}
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Registering webhook     {"path": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraphrestore"}
2024-01-17T06:53:10Z    INFO    setup   starting manager
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook.webhooks     Starting webhook server
2024-01-17T06:53:10Z    INFO    Starting server {"path": "/metrics", "kind": "metrics", "addr": "127.0.0.1:8080"}
2024-01-17T06:53:10Z    INFO    Starting server {"kind": "health probe", "addr": "[::]:8081"}
I0117 06:53:10.444024       1 leaderelection.go:248] attempting to acquire leader lease tigergraph/9d6fe668.tigergraph.com...
2024-01-17T06:53:10Z    INFO    controller-runtime.certwatcher  Updated current TLS certificate
2024-01-17T06:53:10Z    INFO    controller-runtime.webhook      Serving webhook server  {"host": "", "port": 9443}
2024-01-17T06:53:10Z    INFO    controller-runtime.certwatcher  Starting certificate watcher
I0117 06:53:28.295175       1 leaderelection.go:258] successfully acquired lease tigergraph/9d6fe668.tigergraph.com
2024-01-17T06:53:28Z    DEBUG   events  tigergraph-operator-controller-manager-65fbf7689b-zg6h9_301c1e20-6188-46f8-b548-feaeb28e542a became leader      {"type": "Normal", "object": {"kind":"Lease","namespace":"tigergraph","name":"9d6fe668.tigergraph.com","uid":"af0adb63-c1e8-441d-aa81-58507ab90c7f","apiVersion":"coordination.k8s.io/v1","resourceVersion":"1414049"}, "reason": "LeaderElection"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph", "source": "kind source: *v1alpha1.TigerGraph"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraphbackupschedule", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackupSchedule", "source": "kind source: *v1alpha1.TigerGraphBackupSchedule"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph", "source": "kind source: *v1.ConfigMap"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph", "source": "kind source: *v1.Service"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph", "source": "kind source: *v1.StatefulSet"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph", "source": "kind source: *v1.Job"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph", "source": "kind source: *v1.Ingress"}
2024-01-17T06:53:28Z    INFO    Starting Controller     {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraphbackupschedule", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackupSchedule", "source": "kind source: *v1.CronJob"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraphbackup", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackup", "source": "kind source: *v1alpha1.TigerGraphBackup"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraphbackup", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackup", "source": "kind source: *v1.Job"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraphrestore", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphRestore", "source": "kind source: *v1alpha1.TigerGraphRestore"}
2024-01-17T06:53:28Z    INFO    Starting EventSource    {"controller": "tigergraphrestore", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphRestore", "source": "kind source: *v1.Job"}
2024-01-17T06:53:28Z    INFO    Starting Controller     {"controller": "tigergraphrestore", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphRestore"}
2024-01-17T06:53:28Z    INFO    Starting Controller     {"controller": "tigergraphbackupschedule", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackupSchedule"}
2024-01-17T06:53:28Z    INFO    Starting Controller     {"controller": "tigergraphbackup", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackup"}
2024-01-17T06:53:28Z    INFO    Starting workers        {"controller": "tigergraphrestore", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphRestore", "worker count": 1}
2024-01-17T06:53:28Z    INFO    Starting workers        {"controller": "tigergraphbackupschedule", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackupSchedule", "worker count": 1}
2024-01-17T06:53:28Z    INFO    Starting workers        {"controller": "tigergraph", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraph", "worker count": 1}
2024-01-17T06:53:28Z    INFO    Starting workers        {"controller": "tigergraphbackup", "controllerGroup": "graphdb.tigergraph.com", "controllerKind": "TigerGraphBackup", "worker count": 1}
2024-01-17T06:54:19Z    DEBUG   controller-runtime.webhook.webhooks     received request        {"webhook": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraph", "UID": "89ee67ac-a8c5-4b8f-b147-eac94380f50c", "kind": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraph", "resource": {"group":"graphdb.tigergraph.com","version":"v1alpha1","resource":"tigergraphs"}}
2024-01-17T06:54:19Z    INFO    tigergraph-resource     validate delete {"name": "test-cluster", "namespace": "tigergraph"}
2024-01-17T06:54:19Z    DEBUG   controller-runtime.webhook.webhooks     wrote response  {"webhook": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraph", "code": 200, "reason": "", "UID": "89ee67ac-a8c5-4b8f-b147-eac94380f50c", "allowed": true}
2024-01-17T07:16:47Z    DEBUG   controller-runtime.webhook.webhooks     received request        {"webhook": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraph", "UID": "a571285b-c58f-4341-baeb-5a18db28c4a9", "kind": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraph", "resource": {"group":"graphdb.tigergraph.com","version":"v1alpha1","resource":"tigergraphs"}}
2024-01-17T07:16:47Z    DEBUG   controller-runtime.webhook.webhooks     wrote response  {"webhook": "/mutate-graphdb-tigergraph-com-v1alpha1-tigergraph", "code": 200, "reason": "", "UID": "a571285b-c58f-4341-baeb-5a18db28c4a9", "allowed": true}
2024-01-17T07:16:47Z    DEBUG   controller-runtime.webhook.webhooks     received request        {"webhook": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraph", "UID": "744ec62b-fe79-4bf1-868e-bb5946422bae", "kind": "graphdb.tigergraph.com/v1alpha1, Kind=TigerGraph", "resource": {"group":"graphdb.tigergraph.com","version":"v1alpha1","resource":"tigergraphs"}}
2024-01-17T07:16:47Z    INFO    tigergraph-resource     validate create {"name": "test-cluster2", "namespace": "tigergraph"}
2024-01-17T07:16:47Z    DEBUG   controller-runtime.webhook.webhooks     wrote response  {"webhook": "/validate-graphdb-tigergraph-com-v1alpha1-tigergraph", "code": 200, "reason": "", "UID": "744ec62b-fe79-4bf1-868e-bb5946422bae", "allowed": true}
```

You can see there are logs output by webhooks, which means webhooks work well. But when webhooks accept creation of TigerGraph CR, the reconcile for TigerGraph CR is not triggered. If you encounter this issue, you can uninstall the running operator by `kubectl tg uninstall --namespace $NAMESPACE`, and install it again. Then the reconcile will be triggered properly.

### The TigerGraph Pod was killed and restarted because its ephemeral local storage usage exceeded the total limit set for the containers

This issue may happen when:

1. Upgrade TigerGraph to version 4.2.0 or above using Operator 1.5.0 or above if the spec.resources.limits.ephemeral-storage field is configured with a value less than 20Gi.
2. A dedicated Persistent Volume is not configured for the directory path saving TigerGraph logs, and the logs were persisted in ephemeral local storage.

you can check the events when this error occurs. The relevant events are as follows:

```bash
kubectl get events --sort-by='.metadata.creationTimestamp' -A

tigergraph   85s         Warning   Evicted                       pod/test-cluster-0                                             Pod ephemeral local storage usage exceeds the total limit of containers 6Gi.
tigergraph   2m5s        Normal    Killing                       pod/test-cluster-0                                             Stopping container tigergraph
```

When this error occurs, you need to update `spec.resources.limits.ephemeral-storage` to a larger value. The recommended value is 20Gi, which is the required minimum starting from version 1.5.0.

On the other hand, it's best to configure a dedicated Persistent Volume using the following configuration:

```YAML
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: ${STORAGE_CLASS_NAME}
      volumeMode: Filesystem
    additionalStorages:
      - name: tg-log
        storageClassName: ${STORAGE_CLASS_NAME}
        storageSize: 10Gi
```

For details on mounting multiple persistent volumes, please refer to [Multiple persistent volumes mounting](../03-deploy/multiple-persistent-volumes-mounting.md)

### The TigerGraph CR was created successfully, but the TigerGraph pods and other dependent resources are not created

This issue may occur if you install a namespaced-scoped TigerGraph Operator and create a TigerGraph CR in a different namespace than where the Operator is installed.

When you create a TigerGraph CR and run `kubectl get tg -n $NAMESPACE`, you will notice that all cluster statuses are empty.

```bash
$ kubectl get tg
NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION   SERVICE-TYPE   REGION-AWARENESS   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster                                                                                                                                  111s
```

Check the events of the TigerGraph CR; they will also be empty.

```bash
$ kubectl describe tg test-cluster
Name:         test-cluster
Namespace:    default
Labels:       <none>
Annotations:  tigergraph.com/debug-mode: false
API Version:  graphdb.tigergraph.com/v1alpha1
Kind:         TigerGraph
Metadata:
  Creation Timestamp:  2025-03-03T10:09:03Z
  Finalizers:
    tigergraph.com/tg-protection
  Generation:        1
  Resource Version:  639744822
  UID:               7d580c3d-cee8-4dd3-bb83-d39785aef093
Spec:
  Ha:                 2
  Image:              docker.io/tigergraph/tigergraph-k8s:4.1.2
  Image Pull Policy:  IfNotPresent
  Image Pull Secrets:
    Name:   tigergraph-image-pull-secret
  License:  xxxxxxx
  Listener:
    Type:  LoadBalancer
  Pod Labels:
    tigergraph.com/customLabel:  custom
  Private Key Name:              ssh-key-secret
  Replicas:                      3
  Resources:
    Requests:
      Cpu:     4
      Memory:  8Gi
  Security Context:
    Privileged:    false
    Run As Group:  1000
    Run As User:   1000
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
Events:                    <none>
```

For this issue, you need to check the scope type of the TierGraph Operator with the following commands:

check the namesapce where the operator insalled:

```bash
$ helm ls -A
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                           APP VERSION                
tg-operator     tigergraph      1               2025-03-03 10:07:29.901237917 +0000 UTC deployed        tg-operator-1.5.0               1.5.0
```

Check the clusterScope and watchNameSpaces value in the TigerGraph Operator's values.yaml file.

```bash
$ helm get values tg-operator -n tigergraph
USER-SUPPLIED VALUES:
clusterScope: false
image: docker.io/tigergraph/tigergraph-k8s-operator:1.5.0
jobImage: docker.io/tigergraph/tigergraph-k8s-init:1.5.0
maxConcurrentReconcilesOfBackup: 2
maxConcurrentReconcilesOfBackupSchedule: 2
maxConcurrentReconcilesOfRestore: 2
maxConcurrentReconcilesOfTG: 2
pullPolicy: IfNotPresent
replicas: 3
resources:
  requests:
    cpu: 500m
    memory: 512Mi
watchNameSpaces: tigergraph
```

The watch namespace is set to tigergraph, and clusterScope is false, indicating that the TigerGraph Operator is installed in the tigergraph namespace with a namespaced scope.

To resolve this, either create a TigerGraph CR in the same namespace as the TigerGraph Operator or reinstall a cluster-scoped operator to monitor TigerGraph CRs across all namespaces.

### Webhook Certificate Verification Failure in TigerGraph Cluster creation

When creating a TigerGraph Custom Resource (CR) immediately after installing the Operator, there is a slight chance of encountering the following errors:

```bash
Error from server (InternalError): error when creating "STDIN": Internal error occurred: failed calling webhook "mtigergraph.kb.io": failed to call webhook: Post "https://tigergraph-operator-webhook-service.tigergraph.svc:443/mutate-graphdb-tigergraph-com-v1alpha1-tigergraph?timeout=10s": tls: failed to verify certificate: x509: certificate signed by unknown authority
```

The TigerGraph Operator uses cert-manager to manage webhook certificates. If cert-manager is installed and configured properly, the above error can be resolved by following these steps:

- Restart the TigerGraph Operator to reload the certificate:

```bash
kubectl rollout restart deployment tigergraph-operator-controller-manager -n ${NAMESPACE_OF_OPERATION}$
```

If the issue persists, consider reinstalling the TigerGraph Operator.
