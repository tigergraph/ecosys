# TigerGraph Cluster Rolling Update Troubleshoot

This document provides solutions for common issues that may arise during a rolling update of a TigerGraph cluster.

## Prerequisite knowledge

### Cluster Operations Triggering Rolling Updates

A rolling update of a TigerGraph cluster on Kubernetes can be triggered by the following cluster operations:

- Updating the resources (CPU and Memory) of TigerGraph Pods
- Adding or updating the init or sidecar container for TigerGraph Pods
- Adding or updating Node/Pod Affinity
- Updating the TigerGraph Docker image version to trigger the TigerGraph cluster upgrade
- Changing default configurations of TigerGraph Pod and container when upgrading the operator version. For example, in Operator version 0.0.7, the TerminationGracePeriodSeconds of TG Pod was updated to 6 minutes from 1 minute. This change triggers a rolling update after upgrading the Operator successfully.

### Rolling update strategy of StatefulSet

The Kubernetes Operator uses a StatefulSet to manage TigerGraph Pods during rolling updates. Key points of the rolling update strategy include:

- The StatefulSet controller deletes and recreates each Pod in the StatefulSet.
- Pods are updated in order of termination, from the largest ordinal to the smallest.
- Each Pod is updated one at a time.
- The Kubernetes control plane waits until an updated Pod is in the Running and Ready state before updating its predecessor. This can occasionally result in the rolling update process getting stuck.

### Factors Affecting TigerGraph Pod's Running and Ready State

- The requested resources (CPU, Memory, and Persistent Volume) of the TigerGraph Pod must be met for it to be in the Running state.
- The PostStart Handler of the TigerGraph container must run without errors for the Pod to be considered Ready.
- The readiness check of the TigerGraph container must pass for the Pod to be Ready.
- The liveness check of the TigerGraph container must pass for the Pod to be Ready.
- All Init containers of the TigerGraph pod must complete successfully; otherwise, the TigerGraph container will not start running.

## How to Recover from Rolling Update Failure

### Pod Stuck in Pending State Due to Unmet Resource Needs

The Pod may be stuck in pending status due to unmet resource needs, the typical example is the following:

```bash
kubectl get pods
NAME                                                     READY   STATUS    RESTARTS      AGE
test-cluster-0                                           1/1     Running   1 (11m ago)   42h
test-cluster-1                                           0/1     Pending   0             41h
test-cluster-2                                           1/1     Running   1 (11m ago)   42h
tigergraph-operator-controller-manager-9868c59f6-w58mh   2/2     Running   3 (10m ago)   41h
```

You can check the Pod event to find the specific resources needs that are unmet:

```bash
$ kubectl describe pod test-cluster-1
Name:           test-cluster-1
Namespace:      tigergraph
Priority:       0
Node:           <none>
Labels:         controller-revision-hash=test-cluster-7f7b4c5599
                statefulset.kubernetes.io/pod-name=test-cluster-1
                tigergraph.com/cluster-name=test-cluster
                tigergraph.com/cluster-pod=test-cluster
Annotations:    <none>
Status:         Pending
IP:             
IPs:            <none>
Controlled By:  StatefulSet/test-cluster
Init Containers:
  init-tigergraph:
    Image:      alpine:3.17.2
    Port:       <none>
    Host Port:  <none>
    Command:
      sh
      -c
      chown -R 1000:1000 /home/tigergraph/tigergraph/data
    Environment:  <none>
    Mounts:
      /home/tigergraph/tigergraph/data from tg-data (rw)
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-n9jg7 (ro)
Containers:
  tigergraph:
    Image:       docker.io/tigergraph/tigergraph-k8s:3.9.2
    Ports:       9000/TCP, 14240/TCP, 22/TCP
    Host Ports:  0/TCP, 0/TCP, 0/TCP
    Requests:
      cpu:      2
      memory:   6Gi
   ...
Conditions:
  Type           Status
  PodScheduled   False 
Volumes:
  tg-data:
    Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
    ClaimName:  tg-data-test-cluster-1
    ReadOnly:   false
  config-volume:
    Type:      ConfigMap (a volume populated by a ConfigMap)
    Name:      test-cluster-init-config
    Optional:  false
  probe-data:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:     
    SizeLimit:  <unset>
  private-key-volume:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  ssh-key-secret
    Optional:    false
  tg-log:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:     
    SizeLimit:  <unset>
  kube-api-access-n9jg7:
    Type:                    Projected (a volume that contains injected data from multiple sources)
    TokenExpirationSeconds:  3607
    ConfigMapName:           kube-root-ca.crt
    ConfigMapOptional:       <nil>
    DownwardAPI:             true
QoS Class:                   Burstable
Node-Selectors:              <none>
Tolerations:                 node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                             node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
  Type     Reason            Age                  From               Message
  ----     ------            ----                 ----               -------
  Warning  FailedScheduling  17h (x298 over 41h)  default-scheduler  0/1 nodes are available: 1 Insufficient cpu. preemption: 0/1 nodes are available: 1 No preemption victims found for incoming pod.
  Warning  FailedScheduling  11m                  default-scheduler  0/1 nodes are available: 1 Insufficient cpu. preemption: 0/1 nodes are available: 1 No preemption victims found for incoming pod.
```

From the events mentioned above, you can identify that there is an insufficient CPU resource. In such cases, you need to add new nodes first, and the rolling update process will resume once the resource requirements are fulfilled. Additionally, you have the option to reduce the desired CPU or memory resources.

### TG container is in PostStartHookError or CrashLoopBackOff

The PostStart Handle script must execute successfully when restarting the TigerGraph container. If it does not, the TigerGraph pod will enter either the PostStartHookError or CrashLoopBackOff (after multiple failures) state. A typical example of failure is as follows:

```bash
kubectl get pods
NAME                                                      READY   STATUS               RESTARTS     AGE
test-cluster-0                                            1/1     Running              0            9m
test-cluster-1                                            0/1     PostStartHookError   0 (4s ago)   23s
test-cluster-2                                            1/1     Running              0            88s
test-cluster-init-job-4sjjm                               0/1     Completed            0            6m12s
tigergraph-operator-controller-manager-6745f8c5bc-jfw9r   2/2     Running  
$ kubectl get pods
NAME                                                      READY   STATUS             RESTARTS      AGE
test-cluster-0                                            1/1     Running            0             9m27s
test-cluster-1                                            0/1     CrashLoopBackOff   1 (14s ago)   50s
test-cluster-2                                            1/1     Running            0             115s
test-cluster-init-job-4sjjm                               0/1     Completed          0             6m39s
tigergraph-operator-controller-manager-6745f8c5bc-jfw9r   2/2     Running            0            0
```

You can check the logs of PostStart Handle script first by following the command:(**Required TG docker image version 3.9.3, and operator version 0.0.9**)

> [!NOTE]
> The failure examples below are intentionally simulated, In practice you probably won't have this problem.

```bash
$ kubectl logs test-cluster-1 
Defaulted container "tigergraph" out of: tigergraph, init-tigergraph (init)
[Sun Jul 23 03:59:38 UTC 2023] tigergraph container is running now
[Sun Jul 23 03:59:52 UTC 2023] the config file /home/tigergraph/.tg.cfg is not exist

$ kct logs test-cluster-1 
Defaulted container "tigergraph" out of: tigergraph, init-tigergraph (init)
Name:   test-cluster-1.test-cluster-internal-service.tigergraph.svc.cluster.local
Address: 10.244.0.196

Server:         10.96.0.10
Address:        10.96.0.10#53

Name:   test-cluster-2.test-cluster-internal-service.tigergraph.svc.cluster.local
Address: 10.244.0.198

[Wed Jul 12 04:36:02 UTC 2023] tigergraph container is running now
[Wed Jul 12 04:36:17 UTC 2023] try to start all services on the current node
[   Info] Starting EXE
[Wed Jul 12 04:36:18 UTC 2023] start service EXE_3 of current node successfully
[   Info] Starting CTRL
[Wed Jul 12 04:36:18 UTC 2023] failed to start service CTRL of all nodes for 1 times
[   Info] Starting CTRL
[Wed Jul 12 04:37:18 UTC 2023] failed to start service CTRL of all nodes for 2 times
[   Info] Starting CTRL
[Wed Jul 12 04:38:18 UTC 2023] failed to start service CTRL of all nodes for 3 times
[Wed Jul 12 04:38:18 UTC 2023] failed to start all services of current node
```

The PostStart Handle script attempts to start all services on the current Pod. If it fails, the TigerGraph container will be restarted. In such cases, we need to identify the root cause by examining the logs of the current container and addressing any specific issues first.

You can use the following command to log in to the TigerGraph pod and review the logs of the failed services.

- Log into the TigerGraph cluster pod

  ```bash
  # the following command equals `kubectl exec it test-cluster-0 -- bash`
  kubectl tg connect --cluster-name test-cluster --namespace tigergraph
  ```

- Check the TigerGraph services status

  ``` bash
  gadmin status
  # for example, if GSE is down, you can check the log path of it.
  gadmin log GSE
  ```

- Start the down service

  ```bash
  gadmin start GSE
  ```

### Readiness or Liveness Check of TigerGraph Container Fails

The liveness check will monitor the listener port of the executor. If it remains down after four retry attempts, the container will be terminated and then restarted.

Therefore, if the executor cannot be started as expected, you should log in to the Pod to review the error logs of the executor and address those issues first.

Regarding the readiness check for the TigerGraph container, it assesses the service status of the current TG container. If any unexpected issues arise, causing the liveness check to fail, the rolling update will remain stalled until the liveness check succeeds.

During a rolling update, you can intentionally stop all services to trigger a readiness check failure, resulting in the rolling update becoming stuck at the Pod named `test-cluster-1`.

```bash
kubectl get pods
NAME                                                      READY   STATUS      RESTARTS      AGE
test-cluster-0                                            0/1     Running     0             39m
test-cluster-1                                            0/1     Running     7 (18m ago)   32m
test-cluster-2                                            0/1     Running     0             33m
test-cluster-init-job-62qwz                               0/1     Completed   0             37m
tigergraph-operator-controller-manager-6745f8c5bc-kfssx   2/2     Running     0             39m
```

Check the events of Pod test-cluster-1:

```bash
$ kubectl describe pod test-cluster-1 
Name:         test-cluster-1
Namespace:    tigergraph
Priority:     0
Node:         tg-control-plane/172.18.0.2
Start Time:   Sun, 23 Jul 2023 05:00:10 +0000
Labels:       controller-revision-hash=test-cluster-568859648
              statefulset.kubernetes.io/pod-name=test-cluster-1
              tigergraph.com/cluster-name=test-cluster
              tigergraph.com/cluster-pod=test-cluster
              tigergraph.com/gui-service=true
              tigergraph.com/nginx-service=true
              tigergraph.com/restpp-service=true
Annotations:  <none>
Status:       Running
IP:           10.244.0.39
IPs:
  IP:           10.244.0.39
Controlled By:  StatefulSet/test-cluster
Init Containers:
  init-tigergraph:
    Container ID:  containerd://0f1b4141969080be757a8cbf1d4e62122dc2846ea0968416a790b95cadcacc3f
    Image:         alpine:3.17.2
    Image ID:      docker.io/library/alpine@sha256:ff6bdca1701f3a8a67e328815ff2346b0e4067d32ec36b7992c1fdc001dc8517
    Port:          <none>
    Host Port:     <none>
    Command:
      sh
      -c
      chown -R 1000:1000 /home/tigergraph/tigergraph/data
    State:          Terminated
      Reason:       Completed
      Exit Code:    0
      Started:      Sun, 23 Jul 2023 05:00:11 +0000
      Finished:     Sun, 23 Jul 2023 05:00:11 +0000
    Ready:          True
    Restart Count:  0
    Environment:    <none>
    Mounts:
      /home/tigergraph/tigergraph/data from tg-data (rw)
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-cftws (ro)
Containers:
  tigergraph:
    Container ID:   containerd://f47028197d9376f558a088b5b88cb34e0f00f6639a433d115b29b493b54c2e87
    Image:          docker.io/tigergraph/tigergraph-k8s:3.9.3
    Image ID:       docker.io/tigergraph/tigergraph-k8s@sha256:dd3dd058fbef7eae77cf51e622c467d290ceeaf9644b8392b5b0eec4920b84de
    Ports:          9000/TCP, 14240/TCP, 22/TCP
    Host Ports:     0/TCP, 0/TCP, 0/TCP
    State:          Running
      Started:      Sun, 23 Jul 2023 05:19:38 +0000
    Last State:     Terminated
      Reason:       Error
      Exit Code:    143
      Started:      Sun, 23 Jul 2023 05:13:13 +0000
      Finished:     Sun, 23 Jul 2023 05:14:33 +0000
    Ready:          False
    Restart Count:  7
    Requests:
      cpu:      2
      memory:   7Gi
...
Events:
  Type     Reason             Age                From               Message
  ----     ------             ----               ----               -------
  Normal   Scheduled          34m                default-scheduler  Successfully assigned tigergraph/test-cluster-1 to tg-control-plane
  Normal   Pulled             34m                kubelet            Container image "alpine:3.17.2" already present on machine
  Normal   Started            34m                kubelet            Started container init-tigergraph
  Normal   Created            34m                kubelet            Created container init-tigergraph
  Normal   Pulled             34m                kubelet            Successfully pulled image "docker.io/tigergraph/tigergraph-k8s:3.9.2" in 2.034685698s
  Normal   Pulled             32m                kubelet            Successfully pulled image "docker.io/tigergraph/tigergraph-k8s:3.9.2" in 338.940713ms
  Warning  FailedPreStopHook  31m (x2 over 32m)  kubelet            Exec lifecycle hook ([/bin/bash -c 
                  if [ "$(ls -A /home/tigergraph/tigergraph/data/|grep -v lost|tail -1)" ]; then
                    export PATH=/home/tigergraph/tigergraph/app/cmd:$PATH
          PROCESS_ALL=$(ghostname|awk '{$1=""}1'| \
          awk '{for(x=1;x<=NF;x++)if(x % 2)printf "%s", $x (x == NF || x == (NF-1)?"\n":" ")}')
          if [ $? != 0 ]; then exit 0; fi
          gadmin stop $PROCESS_ALL -y
                  fi]) for Container "tigergraph" in Pod "test-cluster-1_tigergraph(7f81bbfb-c425-493f-881a-2e1cf502d44d)" failed - error: command '/bin/bash -c 
                  if [ "$(ls -A /home/tigergraph/tigergraph/data/|grep -v lost|tail -1)" ]; then
                    export PATH=/home/tigergraph/tigergraph/app/cmd:$PATH
          PROCESS_ALL=$(ghostname|awk '{$1=""}1'| \
          awk '{for(x=1;x<=NF;x++)if(x % 2)printf "%s", $x (x == NF || x == (NF-1)?"\n":" ")}')
          if [ $? != 0 ]; then exit 0; fi
          gadmin stop $PROCESS_ALL -y
                  fi' exited with 1: , message: "ExternalError (Failed to get the APP root from config; The file ~/.tg.cfg either does not exist or is a broken link. Please create a new symlink at this location and point it to the tg.cfg file located in the 'configs' directory of System.DataRoot. This can be done using the following command: ln -s /path/to/System.DataRoot/configs/tg.cfg ~/.tg.cfg; open /home/tigergraph/.tg.cfg: no such file or directory)\n"
  Normal   Killing              31m (x2 over 32m)  kubelet  FailedPostStartHook
  Normal   Pulling              31m (x3 over 34m)  kubelet  Pulling image "docker.io/tigergraph/tigergraph-k8s:3.9.2"
  Normal   Pulled               31m                kubelet  Successfully pulled image "docker.io/tigergraph/tigergraph-k8s:3.9.2" in 315.864405ms
  Normal   Created              31m (x3 over 34m)  kubelet  Created container tigergraph
  Normal   Started              31m (x3 over 34m)  kubelet  Started container tigergraph
  Warning  BackOff    18m (x41 over 31m)    kubelet  Back-off restarting failed container
  Warning  Unhealthy  13m                   kubelet  Readiness probe failed: command "/bin/bash -c \n\t\t\t\t\t\t\t\t\t\t\texport PATH=/home/tigergraph/tigergraph/app/cmd:$PATH\n\t\t\t\t\t\t\t\t\t\t\tcommand gadmin > /dev/null 2>&1\n\t\t\t\t\t\t\t\t\t\t\tif [ $? != 0 ]; then exit 0; fi\n\t\t\t\t\t\t\t\t\t\t\tDATA_ROOT=$(gadmin config get system.dataroot --file ~/.tg.cfg)\n\t\t\t\t\t\t\t\t\t\t\tCFG_FILE=${DATA_ROOT}/configs/tg.cfg\n\t\t\t\t\t\t\t\t\t\t\tif [ ! -f $CFG_FILE ]; then\n\t\t\t\t\t\t\t\t\t\t\t  exit 0\n\t\t\t\t\t\t\t\t\t\t\tfi\n\t\t\t\t\t\t\t\t\t\t\tPROCESS_ALL=$(ghostname|awk '{$1=\"\"}1'| awk '{for(x=1;x<=NF;x++)if(x % 2)printf \"%s\", \\\n\t\t\t\t\t\t\t\t\t\t\t$x (x == NF || x == (NF-1)?\"\\n\":\" \")}')\n\t\t\t\t\t\t\t\t\t\t\tif [ $? != 0 ]; then exit 0; fi\n\t\t\t\t\t\t\t\t\t\t\tgadmin status -v $PROCESS_ALL|grep -v Online|head -n -1|tail -n +4 | \\\n\t\t\t\t\t\t\t\t\t\t\tawk '{print $2,$4,$6}'| while read -r service_info;\n\t\t\t\t\t\t\t\t\t\t\tdo\n\t\t\t\t\t\t\t\t\t\t\t  service_name=$(echo $service_info|awk '{print $1}')\n\t\t\t\t\t\t\t\t\t\t\t  service_status=$(echo $service_info|awk '{print $2}')\n\t\t\t\t\t\t\t\t\t\t\t  process_status=$(echo $service_info|awk '{print $3}')\n\t\t\t\t\t\t\t\t\t\t\t  if [ \"$service_status\" != \"Warmup\" ]; then\n\t\t\t\t\t\t\t\t\t\t\t\techo \"service status of $service_name is $service_status, exit error.\"\n\t\t\t\t\t\t\t\t\t\t\t\texit 1\n\t\t\t\t\t\t\t\t\t\t\t  else\n\t\t\t\t\t\t\t\t\t\t\t\tif [[ $service_name =~ ^GPE.* ]] || [[ $service_name =~ ^GSE.* ]]; then\n\t\t\t\t\t\t\t\t\t\t\t\t  if ! test -f ~/tigergraph/data/gstore/0/part/config.yaml; then\n\t\t\t\t\t\t\t\t\t\t\t\t\tcontinue\n\t\t\t\t\t\t\t\t\t\t\t\t  else\n\t\t\t\t\t\t\t\t\t\t\t\t\techo \"service status of $service_name should not be Warmup, exit error\"\n\t\t\t\t\t\t\t\t\t\t\t\t\texit 1\n\t\t\t\t\t\t\t\t\t\t\t\t  fi\n\t\t\t\t\t\t\t\t\t\t\t\telse\n\t\t\t\t\t\t\t\t\t\t\t\t  echo \"service status of $service_name is $service_status, not Online, exit error\"\n\t\t\t\t\t\t\t\t\t\t\t\t  exit 1\n\t\t\t\t\t\t\t\t\t\t\t\tfi\n\t\t\t\t\t\t\t\t\t\t\t  fi\n\t\t\t\t\t\t\t\t\t\t\tdone\n\t\t\t\t\t\t\t\t\t\t\t" timed out
  Warning  Unhealthy  3m54s (x35 over 12m)  kubelet  Readiness probe failed: service status of ADMIN#2 is Down, exit error.
[Warning] Status might not be up-to-date due to sync data error; failed to get latest offset, err is "kafka: client has run out of available brokers to talk to (Is your cluster reachable?)"
```

You can execute the following command to start all down services, then the rolling update will continue:

```bash
kubectl exec -it test-cluster-0 -- /home/tigergraph/tigergraph/app/cmd/gadmin start all --auto-restart
Defaulted container "tigergraph" out of: tigergraph, init-tigergraph (init)
[   Info] Starting EXE
[   Info] Starting CTRL
[   Info] Starting ZK ETCD DICT KAFKA ADMIN GSE NGINX GPE RESTPP KAFKASTRM-LL KAFKACONN GSQL IFM GUI
```

If the above command executes failed due to some other issues, you can rerun it till all service are online.

```bash
kubectl exec -it test-cluster-0 -- /home/tigergraph/tigergraph/app/cmd/gadmin status -v
Defaulted container "tigergraph" out of: tigergraph, init-tigergraph (init)
+--------------------+-------------------------+-------------------------+-------------------------+
|    Service Name    |     Service Status      |      Process State      |       Process ID        |
+--------------------+-------------------------+-------------------------+-------------------------+
|      ADMIN#1       |         Online          |         Running         |          2118           |
|      ADMIN#2       |         Online          |         Running         |         380177          |
|      ADMIN#3       |         Online          |         Running         |          73481          |
|       CTRL#1       |         Online          |         Running         |           849           |
|       CTRL#2       |         Online          |         Running         |         375698          |
|       CTRL#3       |         Online          |         Running         |          72151          |
|       DICT#1       |         Online          |         Running         |           988           |
|       DICT#2       |         Online          |         Running         |         376859          |
|       DICT#3       |         Online          |         Running         |          72319          |
|       ETCD#1       |         Online          |         Running         |           955           |
|       ETCD#2       |         Online          |         Running         |         376824          |
|       ETCD#3       |         Online          |         Running         |          72307          |
|       EXE_1        |         Online          |         Running         |           820           |
|       EXE_2        |         Online          |         Running         |          28447          |
|       EXE_3        |         Online          |         Running         |          41917          |
|      GPE_1#1       |         Warmup          |         Running         |          2167           |
|      GPE_1#2       |         Warmup          |         Running         |         380278          |
|      GSE_1#1       |         Warmup          |         Running         |          2128           |
|      GSE_1#2       |         Warmup          |         Running         |         380186          |
|       GSQL#1       |         Online          |         Running         |          2399           |
|       GSQL#2       |         Online          |         Running         |          8187           |
|       GSQL#3       |         Online          |         Running         |          73919          |
|       GUI#1        |         Online          |         Running         |          2555           |
|       GUI#2        |         Online          |         Running         |          8338           |
|       GUI#3        |         Online          |         Running         |          74265          |
|       IFM#1        |         Online          |         Running         |          2499           |
|       IFM#2        |         Online          |         Running         |          8287           |
|       IFM#3        |         Online          |         Running         |          74125          |
|      KAFKA#1       |         Online          |         Running         |          1056           |
|      KAFKA#2       |         Online          |         Running         |         377205          |
|      KAFKA#3       |         Online          |         Running         |          72386          |
|    KAFKACONN#1     |         Warmup          |         Running         |          2224           |
|    KAFKACONN#2     |         Online          |         Running         |         362949          |
|    KAFKACONN#3     |         Online          |         Running         |          73630          |
|   KAFKASTRM-LL_1   |         Online          |         Running         |          2186           |
|   KAFKASTRM-LL_2   |         Online          |         Running         |          8073           |
|   KAFKASTRM-LL_3   |         Online          |         Running         |          73526          |
|      NGINX#1       |         Online          |         Running         |          2142           |
|      NGINX#2       |         Online          |         Running         |         380188          |
|      NGINX#3       |         Online          |         Running         |          73501          |
|      RESTPP#1      |         Online          |         Running         |          2169           |
|      RESTPP#2      |         Online          |         Running         |         380319          |
|      RESTPP#3      |         Online          |         Running         |          73512          |
|        ZK#1        |         Online          |         Running         |           864           |
|        ZK#2        |         Online          |         Running         |         376115          |
|        ZK#3        |         Online          |         Running         |          72166          |
+--------------------+-------------------------+-------------------------+-------------------------+ 
```

The rolling update process will continue and eventually succeed.

```bash
kubectl get pods -w
NAME                                                      READY   STATUS      RESTARTS      AGE
test-cluster-0                                            0/1     Running     0             48m
test-cluster-1                                            0/1     Running     7 (27m ago)   42m
test-cluster-2                                            0/1     Running     0             43m
test-cluster-init-job-62qwz                               0/1     Completed   0             47m
tigergraph-operator-controller-manager-6745f8c5bc-kfssx   2/2     Running     0             48m
test-cluster-1                                            1/1     Running     7 (27m ago)   42m
test-cluster-0                                            1/1     Running     0             49m
test-cluster-2                                            1/1     Running     0             44m
test-cluster-0                                            1/1     Terminating   0             49m
test-cluster-0                                            0/1     Terminating   0             49m
test-cluster-0                                            0/1     Terminating   0             49m
test-cluster-0                                            0/1     Terminating   0             49m
test-cluster-0                                            0/1     Pending       0             0s
test-cluster-0                                            0/1     Pending       0             0s
test-cluster-0                                            0/1     Init:0/1      0             0s
test-cluster-0                                            0/1     PodInitializing   0             1s
test-cluster-0                                            0/1     PodInitializing   0             13s
test-cluster-0                                            0/1     Running           0             2m6s
test-cluster-0                                            1/1     Running           0             2m7s
```

> [!IMPORTANT]
> There are some best practices you can use for rolling update failover.

1. Identify the root cause by examining Pod events and TigerGraph container logs if the rolling update becomes stuck in the Pending or PostStartHookError state
2. Avoid executing gadmin stop all -y during a rolling update, as its behavior is undefined.
3. If the rolling update becomes stuck due to a service shutdown, execute gadmin start all --auto-restart.
4. If executing gadmin start all --auto-restart fails to start all services, log in to the Pods to address the issues with specific services that are down first.
