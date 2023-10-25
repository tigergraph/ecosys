# TigerGraph FAQs on Kubernetes

## Are hardware(on-premise) licenses valid on Kubernetes and how do I renew the license?

If you have deployed TigerGraph cluster on-premise with a hardware license, you can't reuse the license on Kubernetes.

The hardware license is invalid on Kubernetes, and you need to apply a special license for TigerGraph on K8s.

The easiest way to update the license is to use `kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --license`, you can also log in to one TigerGraph pod and execute `gadmin config set System.License && gadmin config apply -y`.

## Whether to support TigerGraph downgrade with Operator？

The TigerGraph downgrade is not recommended and the behavior is undefined, you can find the solution in the [troubleshoot section](../05-troubleshoot/cluster-management.md) if you accidentally performed a downgrade.

## Does TigerGraph Operator support resizing the persistent volume for an existing TigerGraph cluster on K8s?

At present, Kubernetes offers automatic volume resizing for persistent volumes, but not for volumes linked to StatefulSets. Since some of the CSI does not support ALLOWVOLUMEEXPANSION, the Operator doesn't support to resize it automatically, you can refer to [persistent volume resizing](../07-reference/expand-persistent-volume.md) to do it manually.

## Does TigerGraph cluster support high availability when performing cluster management such as resource update, upgrade, scale and backup?

TigerGraph's exceptional performance comes with certain considerations regarding high availability during upgrading and scaling operations. Presently, TigerGraph does not offer high availability support specifically for these processes. However, it's important to note that high availability is maintained for other operations.

For your optimal experience, it is strongly recommended to start a backup operation before starting any upgrade or scaling activities. This precautionary measure ensures the safety of your data and system integrity.

## How to know the status of cluster management? Do I need to confirm stat before modifying TigerGraph cluster CR configuration?

In essence, TigerGraph does not inherently maintain a record of the cluster status throughout its lifecycle. However, understanding the status of the TigerGraph cluster is pivotal for the TigerGraph Operator. This insight empowers the Operator to determine which operations can be executed at any given moment.

To facilitate this, the TigerGraph Operator takes on the responsibility of managing the cluster status based on the user's operational configurations. This orchestration ensures a cohesive interaction between the TigerGraph cluster and the Operator, leading to effective and accurate execution of desired operations.

By keeping a dynamic grasp of the cluster status, the TigerGraph Operator optimizes the functionality of your TigerGraph environment, streamlining operations and contributing to the overall efficiency of your workflow.

Currently, TigerGraph Operator will divide the cluster status into six types, and it will probably add new types according to the requirements.

TigerGraph cluster status in Operator are following as:

| State of TigerGraph on Kubernetes | Description |
|----------|----------|
| Normal | TigerGraph cluster is in ready state, it's allowed to do any cluster operations |
| Initialization | Prepare for TigerGraph pods and init TigerGraph cluster |
| Update | TigerGraph cluster is in rolling update that indicate you update the CPU, Memory, and other pod configurations. |
| Upgrade | TigerGraph cluster is in upgrading process, pulling new version image and performing upgrade job|
| Expand | TigerGraph cluster is in scale up process, preparing for new pods and performing expansion job|
| Shrink | TigerGraph cluster is in scale down process, performing shrinking job and scale down pods|

You can execute the following command to check the status of TigerGraph cluster on Kubernetes:

```bash
kubectl get tg ${TIGERGRAPH_CLUSTER_NAME} -o yaml -n ${NAMESPACE}|yq .status
clusterSize: 3
clusterTopology:
  test-cluster-0:
    - gui
    - nginx
    - restpp
  test-cluster-1:
    - gui
    - nginx
    - restpp
  test-cluster-2:
    - gui
    - nginx
    - restpp
conditions:
  - lastProbeTime: "2023-08-23T08:37:00Z"
    status: "True"
    type: Normal
  - lastProbeTime: "2023-08-24T05:46:24Z"
    message: Hello GSQL
    status: "True"
    type: test-cluster-0-rest-Available
  - lastProbeTime: "2023-08-24T05:46:24Z"
    message: Hello GSQL
    status: "True"
    type: test-cluster-1-rest-Available
  - lastProbeTime: "2023-08-24T05:46:24Z"
    message: Hello GSQL
    status: "True"
    type: test-cluster-2-rest-Available
ha: 2
image: docker.io/tginternal/tigergraph-k8s:3.9.2
listener:
  type: LoadBalancer
replicas: 3
```
