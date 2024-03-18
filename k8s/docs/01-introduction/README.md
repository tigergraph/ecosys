# TigerGraph Operator Overview

TigerGraph Operator stands as an automated operations system meticulously designed to streamline the management of TigerGraph clusters within Kubernetes environments. Its comprehensive suite of functionalities encompasses every aspect of the TigerGraph lifecycle, spanning deployment, upgrades, scaling, backups, restoration, and fail-over processes. Whether you're operating in a public cloud setting or within a self-hosted environment, TigerGraph Operator ensures that your TigerGraph instances function seamlessly within Kubernetes clusters.

Understanding the intricate synergy between TigerGraph, TigerGraph Operator, and Kubernetes versions is pivotal. This relationship is as follows:

| TigerGraph Operator version | TigerGraph version  | Kubernetes version |
|----------|----------|----------|
| 1.0.0 | TigerGraph >= 3.6.0 |1.24, 1.25, 1.26, 1.27, **1.28**|
| 0.0.9 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.3|1.23, 1.24, 1.25, 1.26, 1.27|
| 0.0.7 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.2|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.6 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.1|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.5 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.1|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.4 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.0|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.3 | TigerGraph >= 3.6.0 && TigerGraph <= 3.8.0|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.2 | TigerGraph >= 3.6.0 && TigerGraph <= 3.7.0|1.22, 1.23, 1.24, 1.25, 1.26|

## Manage TigerGraph clusters using TigerGraph Operator

TigerGraph Operator offers several deployment options for TigerGraph clusters on Kubernetes, catering to both test and production environments:

- For test environment

  - [Getting started using Kind](../02-get-started/get_started.md)

- For production environment

  - On public cloud:
    - [Deploy TigerGraph on AWS EKS](../03-deploy/tigergraph-on-eks.md)
    - [Deploy TigerGraph on Google Cloud GKE](../03-deploy/tigergraph-on-gke.md)
    - [Deploy TigerGraph on Red Hat OpenShift](../03-deploy/tigergraph-on-openshift.md)
    - [Deploy TigerGraph on K8s without internet access](../03-deploy/deploy-without-internet.md)

Once your deployment is complete, refer to the following documents for guidance on using, operating, and maintaining your TigerGraph clusters on Kubernetes:

- [Configuring TigerGraph Clusters on K8s using TigerGraph CR](../07-reference/configure-tigergraph-cluster-cr-with-yaml-manifests.md)
- [Utilizing Static & Dynamic Persistent Volume Storage](../07-reference/static-and-dynamic-persistent-volume-storage.md)
- [Configuring NodeSelectors, Affinities, and Toleration](../03-deploy/configure-affinity-by-kubectl-tg.md)
- [Working with InitContainers, Sidecar Containers, and Custom Volumes](../03-deploy/use-custom-containers-by-kubectl-tg.md)
- [Resizing Persistent Volumes for TigerGraph](../07-reference/expand-persistent-volume.md)
- [Backing Up and Restoring TigerGraph Clusters](../04-manage/backup-and-restore/README.md)
- [Pause and Resume TigerGraph Clusters](../04-manage/pause-and-resume.md)
- [Customize TigerGraph Pods and Containers](../03-deploy/customize-tigergraph-pod.md)
- [Lifecycle of TigerGraph](../03-deploy/lifecycle-of-tigergraph.md)
- [Multiple persistent volumes mounting](../03-deploy/multiple-persistent-volumes-mounting.md)
- [Cluster status of TigerGraph on k8s](../07-reference/cluster-status-of-tigergraph.md)

In case issues arise and your cluster requires diagnosis, you have two valuable resources:

Refer to [TigerGraph FAQs on Kubernetes](../06-FAQs/README.md) for potential solutions.

Explore [Troubleshoot TigerGraph on Kubernetes](../05-troubleshoot/README.md) to address any challenges.

Lastly, when a new version of TigerGraph Operator becomes available, consult [Upgrade TigerGraph Operator](../04-manage/operator-upgrade.md) for a seamless transition to the latest version.

For detailed information about the features, improvements, and bug fixes introduced in a specific Operator version, refer to the [release notes](../08-release-notes/README.md).
