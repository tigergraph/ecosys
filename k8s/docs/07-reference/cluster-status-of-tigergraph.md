# Cluster status of TigerGraph on k8s

## Get the status of TigerGraph cluster

You can get the status of TigerGraph clusters in a specific namespace by running the following command:

```bash
# if you have installed kubectl tg command, use this one
kubectl tg list -n $NAMESPACE

# alternatively, you can use kubectl get command
kubectl get tg -n $NAMESPACE
```

The output will be like this:

```bash
NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                                      SERVICE-TYPE   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster0   3          3              2           docker.io/tigergraph/tigergraph-k8s:3.10.0           LoadBalancer   Normal           True               4d1h
test-cluster1   3          3              1           docker.io/tigergraph/tigergraph-k8s:3.9.3            Ingress        InitializePost   False              15m
test-cluster2   4          3              1           docker.io/tigergraph/tigergraph-k8s:3.9.3            NodePort       ExpandPost       Unknown            12h
```

You can also get the status of a specific cluster by running the following command:

```bash
# if you have installed kubectl tg command, use this one
kubectl tg status -n $NAMESPACE -c $CLUSTER_NAME
# alternatively, you can use kubectl describe command
kubectl describe tg $CLUSTER_NAME -n $NAMESPACE
```

Check the `Status` field:

```bash
Status:
  Cluster Size:  3
  Conditions:
    Last Transition Time:  2023-12-29T07:27:13Z
    Message:               Cluster is in Normal condition type and status is True
    Reason:                ClusterNormalTrue
    Status:                True
    Type:                  Normal
```

The field CONDITION-TYPE shows the current condition of the cluster. The field CONDITION-STATUS shows the status of the condition, the possible values are: True, False, Unknown. These two fields are used to indicate the current status of the cluster. We can use (*CONDITION-TYPE,CONDITION-STATUS*) to represent the status of the cluster. When the CONDITION-STATUS is True, the cluster is in a good status. When the CONDITION-STATUS is False, the cluster is in a bad status which means some errors occurred. When the CONDITION-STATUS is Unknown, the cluster is doing some operations, such as initializing, expanding, shrinking, etc. It will then transfer to True if the operation is successful, or False if the operation failed.

For example, when you create a new cluster, the status will be (*InitializeRoll,Unknown*). In this status all pods will be created and initialized. After all pods are ready, the status will be (*InitializeRoll,True*) and then transfer to (*InitializePost,Unknown*) immediately. In this status, an init-job will be created by operator to initialize TigerGraph system. If the init-job is successful, the status will be (*InitializePost,True*) and transfer to (*Normal,True*) immediately. If the init-job failed, the status will be (*InitializePost,False*).

## All possible status of TigerGraph

To make it clear, we list all possible status of TigerGraph cluster here, and describe the meaning of each status.

| Condition Type | Possible Condition Status | Description | Note |
| ---------------|---------------------------|-------------|------|
| Normal | True | TigerGraph cluster is in ready state, it's allowed to do any cluster operations | The normal state doesn’t mean that all of TG services like RESTPP, GSE and so on are ready, even though Operator mark the cluster status to normal, client application still need to check the service status by itself. |
| InitializeRoll | Unknown,True | Create all pods and wait for all pods to be ready | It is allowed to update or correct the configurations(CPU, memory, cluster size and so on) in this status |
| InitializeRegionAwarePre | Unknown,False,True | TigerGraph Operator is performing the region uniform check  | It is allowed to update cluster configuration if this condition status is False. |
| InitializePost | Unknown,True,False | Create an init-job to initialize TigerGraph system | The service status of TG is uncertain until initialization is completed. |
| UpdateRoll | Unknown,True | TigerGraph cluster is in rolling update that indicates you update the CPU, Memory, and other TG pod configurations. | The RESTPP and GUI services are available during UpdateRoll status. |
| UpgradeRoll | Unknown,True | TigerGraph cluster is in rolling update to pull new version image | The service status of TG is uncertain. |
| UpgradePost | Unknown,True,False | Create an upgrade-job to upgrade TigerGraph system | The service status of TG is uncertain until upgrade-job is completed. |
| ExpandRoll | Unknown,True | TigerGraph cluster is in rolling update to create more pods for expansion | The service status of TG is uncertain |
| ExpandRegionAwarePre | Unknown,False,True | TigerGraph Operator is performing the region uniform check  | It is allowed to update cluster configuration if this condition status is False. |
| ExpandPost | Unknown,True,False | Create an expand-job to expand TigerGraph cluster | The service status of TG is uncertain until expand-job is completed. |
| ExpandRollBack | Unknown,True | When expand-job failed, you can set the cluster size back to original one, then cluster will transfer to this status to remove unnecessary pods | |
| ShrinkRegionAwarePre | Unknown,False,True | TigerGraph Operator is performing the region uniform check  | It is allowed to update cluster configuration if this condition status is False. |
| ShrinkPre | Unknown,True,False | Create a shrink-pre-job to shrink TigerGraph cluster | The service status of TG is uncertain until shrink-pre-job is completed |
| ShrinkRoll | Unknown,True | TigerGraph cluster is in rolling update to remove pods for shrinking | |
| ConfigUpdate | Unknown,True,False |Create a config-update-job to update configurations and license of TigerGraph system | All services will be restarted to apply new configurations. |
| PauseRoll | Unknown,True | Delete all TigerGraph pods and services and wait them to be terminated | In this status, it's not allowed to update size/ha/image/tigergraphConfig in TigerGraph CR |
| Paused | True | All TigerGraph pods and services are terminated | In this status, it's not allowed to update size/ha/image/tigergraphConfig in TigerGraph CR |
| ResumeRoll | Unknown,True | Create all pods and wait for all pods to be ready | You can update `.spec.pause` to true to pause the cluster again |
| HAUpdate | Unknown,True,False | Create a ha-update-job to update HA of TigerGraph system | Updating cluster size is not allowed when cluster is in HAUpdate |
| HAUpdateRegionAwarePre | Unknown,False,True | TigerGraph Operator is performing the region uniform check  | It is allowed to update cluster configuration if this condition status is False. |
| StorageExpandCheck | Unknown,True,False | Check if the PVCs can be expanded | |
| StorageExpandPre | Unknown,True | Delete all TigerGraph pods to pause the cluster | The TigerGraph services are unavailable |
| StorageExpanding | Unknown,True | Expand PVCs of TigerGraph clusters | When cluster is in this status, there is not StatefulSet and Pod. So all TigerGraph services are unavailable |
| StorageExpandPost | Unknown,True | Recreate TigerGraph to resume the cluster, finalize the storage expansion | |

Meaning of condition status:

* True: The cluster is in good status, the operation has been performed successfully.
* False: The cluster is in bad status, the operation is failed. You need to check logs of the failed pod to find the reason.
* Unknown: The cluster is doing some operations, such as initializing, expanding, shrinking, etc. It will then transfer to True if the operation is successful, or False if the operation failed.
