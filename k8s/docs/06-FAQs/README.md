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

## How can I know the status of cluster management? Do I need to confirm stat before modifying TigerGraph cluster CR configuration?

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

## What is the purpose and functionality of the custom resource definition in the current setup? 
Custom Resource Definition(CRD) is used to define and describe the specifications of creating TG on K8s. When implemented, the end user only needs to care about CRD instead of the native K8s resources such as StatefulSet, and Job.
 
## Is it feasible to install multiple instances of TigerGraph, such as development, testing, and production environments, on a single EKS cluster?  
Yes, the current version supports this feature. The simple way is to install one operator with the cluster scope option enabled `kubectl tg init --cluster-scope true`, or you can install a namespace-scoped operator `kubectl tg init --cluster-scope false` which only watches and manages resources for a specific namespace. When choosing this option, different teams can manage their TG cluster in a certain namespace with their operator.(https://github.com/tigergraph/ecosys/blob/master/k8s/docs/03-deploy/tigergraph-on-eks.md#install-tigergraph-operator)
 
## Is the cert manager directly involved with the webhooks, and if so, how? What is the cert manager doing? 
In Kubernetes, webhooks are typically used for validating admission requests, such as validating that a resource being created or modified adheres to certain policies. These policies can include security, compliance, or custom business rules. Cert Manager can be used to manage TLS certificates for services that expose webhooks.
 
## Why are there multiple TigerGraph operators in the TigerGraph namespace? We observed three pods labeled as "operator" – are these simply scaled replicas, or do they serve distinct functions? 
For a cluster-scoped Operator, there will be only one Operator in a specific namespace. The namespace-scoped operator will be installed for each namespace that you want to manage and deploy TigerGraph on. Operator installation will create a Deployment resource on K8s, and multiple pods that are labeled as “operator” belong to this Deployment, which is used to enable High availability of Operators. You can specify and update the pod numbers of the Operator with `kubectl tg init/upgrade --operator-size 3`, the default value is 3.
 
## How does Kubernetes handle situations where the GSQL leader or another node fails? 
K8s will schedule new pods to another available node if node failures happen. The High Availability of GSQL is the same as we have on-premise; leader switch will be done automatically if your GSQL replication is more than 1.
 
## Does using `kubectl destroy` change the cluster configuration to 1 by 3, or is it necessary to modify the YAML file for this purpose? 
We don't have to necessarily do it only via YAML edit - we can also go from a 1x1 to a 1x3 with the operator by running `kubectl tg update --cluster-name ${YOUR_CLUSTER_NAME} --size 3 --ha 1 --namespace ${YOUR_NAMESPACE}`  We support YAML and kubeclt-tg two modes to manage TG clusters on K8s
 
## Is it possible to restore a 3 node cluster from a 2 node cluster configuration, and if so, how? 
A simple rule for restore: partition must be consistent, replication can be different. (https://graphsql.atlassian.net/wiki/spaces/infra/pages/3082879046/Scenarios+that+support+restore)
