# TigerGraph Operator

TigerGraph Operator stands as an automated operations system meticulously designed to streamline the management of TigerGraph clusters within Kubernetes environments.
Its comprehensive suite of functionalities encompasses every aspect of the TigerGraph lifecycle, spanning deployment, upgrades, scaling, backups, restoration, and fail-over processes.
Whether you're operating in a public cloud setting or within a self-hosted environment, TigerGraph Operator ensures that your TigerGraph instances function seamlessly within Kubernetes clusters.

> [!IMPORTANT]
> Kubernetes Operator support is currently a Preview Feature. Preview Features give users an early look at future production-level features. Preview Features should not be used for production deployments.

Understanding the intricate synergy between TigerGraph, TigerGraph Operator, and Kubernetes versions is pivotal. This relationship is as follows:

| TigerGraph Operator version | TigerGraph version  | Kubernetes version |
|----------|----------|----------|
| 0.0.9 | TigerGraph >= 3.6.0 |1.23, 1.24, 1.25, 1.26, **1.27**|
| 0.0.7 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.2|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.6 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.1|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.5 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.1|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.4 | TigerGraph >= 3.6.0 && TigerGraph <= 3.9.0|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.3 | TigerGraph >= 3.6.0 && TigerGraph <= 3.8.0|1.22, 1.23, 1.24, 1.25, 1.26|
| 0.0.2 | TigerGraph >= 3.6.0 && TigerGraph <= 3.7.0|1.22, 1.23, 1.24, 1.25, 1.26|

## Manage TigerGraph clusters using TigerGraph Operator

TigerGraph Operator offers several deployment options for TigerGraph clusters on Kubernetes, catering to both test and production environments:

- For test environment

  - [Getting started using Kind](docs/02-get-started/get_started.md)

- For production environment

  - On public cloud:
    - [Deploy TigerGraph on AWS EKS](docs/03-deploy/tigergraph-on-eks.md)
    - [Deploy TigerGraph on Google Cloud GKE](docs/03-deploy/tigergraph-on-gke.md)
    - [Deploy TigerGraph on Red Hat OpenShift](docs/03-deploy/tigergraph-on-openshift.md)
    - [Deploy TigerGraph on K8s without internet access](docs/03-deploy/deploy-without-internet.md)

Once your deployment is complete, refer to the following documents for guidance on using, operating, and maintaining your TigerGraph clusters on Kubernetes:

- [Backing Up and Restoring TigerGraph Clusters](docs/04-manage/backup-and-restore/README.md)
- [Configuring TigerGraph Clusters on K8s using TigerGraph CR](docs/07-reference/configure-tigergraph-cluster-cr-with-yaml-manifests.md)
- [Configuring NodeSelectors, Affinities, and Toleration](docs/03-deploy/configure-affinity-by-kubectl-tg.md)
- [Working with InitContainers, Sidecar Containers, and Custom Volumes](docs/03-deploy/use-custom-containers-by-kubectl-tg.md)

Additionally, refer to the following documents for advanced operations and requirements:

- [Resizing Persistent Volumes for TigerGraph](docs/07-reference/expand-persistent-volume.md)
- [Utilizing Static & Dynamic Persistent Volume Storage](docs/07-reference/static-and-dynamic-persistent-volume-storage.md)
- [Integrate Envoy Sidecar](docs/07-reference/integrate-envoy-sidecar.md)
- [Labels used by TigerGraph Operator](docs/07-reference/labels-used-by-tg.md)

In case issues arise and your cluster requires diagnosis, you have two valuable resources:

Refer to [TigerGraph FAQs on Kubernetes](docs/06-FAQs/README.md) for potential solutions.

Explore [Troubleshoot TigerGraph on Kubernetes](docs/05-troubleshoot/README.md) to address any challenges.

Lastly, when a new version of TigerGraph Operator becomes available, consult [Upgrade TigerGraph Operator](docs/04-manage/operator-upgrade.md) for a seamless transition to the latest version.

For detailed information about the features, improvements, and bug fixes introduced in a specific Operator version, refer to the [release notes](docs/08-release-notes/README.md).
