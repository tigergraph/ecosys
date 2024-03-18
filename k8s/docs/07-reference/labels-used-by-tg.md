# Labels used by TigerGraph Operator

TigerGraph utilizes specific labels for different purposes in Kubernetes:

## TigerGraph Cluster Pods

| Label                                  | Usage                                                               |
|----------------------------------------|---------------------------------------------------------------------|
| `tigergraph.com/cluster-name=CLUSTER_NAME` | Indicates which cluster the pod belongs to.                       |
| `tigergraph.com/cluster-pod=CLUSTER_NAME`  | Indicates that the pod belongs to a cluster and not a Job.        |
| `tigergraph.com/gui-service=true`         | Labeled on pods running the GUI service.                          |
| `tigergraph.com/restpp-service=true`      | Labeled on pods running the RESTPP service.                       |

## TigerGraph Job Pods

| Label                                           | Usage                                                                        |
|-------------------------------------------------|------------------------------------------------------------------------------|
| `tigergraph.com/cluster-name=CLUSTER_NAME`      | Indicates which cluster the job is for.                                     |
| `tigergraph.com/cluster-job={CLUSTER_NAME}-{JOB_TYPE}-job` | Specifies the type of job and the cluster it's associated with (JOB_TYPE: init, upgrade, expand, shrink-pre, config-update, ha-update). |

## TigerGraph Backup/Restore Job Pods

| Label                                            | Usage                                                                        |
|--------------------------------------------------|------------------------------------------------------------------------------|
| `tigergraph.com/backup-cluster=CLUSTER_NAME`     | Labeled on pods running backup jobs for the specified cluster.              |
| `tigergraph.com/restore-cluster=CLUSTER_NAME`    | Labeled on pods running restore jobs for the specified cluster.             |

These labels help identify the purpose and affiliation of various pods within the Kubernetes environment, making it easier to manage and monitor different components of TigerGraph clusters, jobs, backups, and restores.
