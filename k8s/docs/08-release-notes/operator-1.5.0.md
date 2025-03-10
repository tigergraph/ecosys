# TigerGraph Operator 1.5.0 Release notes

## Overview

**TigerGraph Operator 1.5.0** is now available, designed to work seamlessly with **TigerGraph version 4.2.0**.

This release introduces significant new features, enhancements, and bug fixes, including:

- Cross-Region Replication (CRR) support within the same Kubernetes (K8s) cluster and across clusters.
- Automated monitoring of TigerGraph clusters upon creation.
- Pre-check support for upgrades, ensuring smooth TigerGraph upgrades on K8s.
- Rolling upgrades for maintenance releases on K8s.
- Flexible job retry mechanism with a backoff delay for cluster operation jobs.
- Improved Rolling Upgrade orchestration by the TigerGraph Operator.
- Separation of upgrade pre-check from the switch_version.sh script.
- Cluster name and namespace visibility in the IFM metrics endpoint.
- Validation webhook to reject invalid ephemeral local storage limits for TigerGraph containers.
- Base image upgrade for TigerGraph K8s Docker image to Ubuntu 22.04.
- PostStart script enhancements, including retrying gadmin start --local.
- Predelete action bypass when an image pull fails.
- Updated SSH configuration (UsePAM=no) to allow setting allowPrivilegeEscalation=false for the TigerGraph container’s SecurityContext.

For further details, see the sections below.

> [!IMPORTANT]
> TigerGraph Operator has had a breaking change since version 1.0.0. If you are still using a version older than 1.0.0, it is strongly recommended that you upgrade to version 1.5.0. Versions older than 1.0.0 have been deprecated.

### kubectl plugin installation

To install the kubectl plugin for TigerGraph Operator 1.5.0, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.5.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### TigerGraph Operator upgrading

#### Upgrading from TigerGraph Operator 1.0.0+ to 1.5.0

There are no breaking changes in the Custom Resource Definitions (CRDs) for version 1.5.0 compared to versions 1.0.0 and above. If you are running Operator 1.0.0 or later, upgrade using the following command:

> [!NOTE]
> There is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss. It is necessary to upgrade TigerGraph CRDs manually for the operator version prior to 1.3.0. However, starting from Operator version 1.3.0, we use [Helm chart’s pre-upgrade hook](https://helm.sh/docs/topics/charts_hooks/) to upgrade the CRDs automatically. You can ignore the first step if you upgrade the operator to version 1.3.0 or above.

> [!IMPORTANT]
> Please ensure that you have installed the `kubectl-tg` version 1.5.0 before upgrading TigerGraph Operator to version 1.5.0.

Ensure you have installed the correct version of kubectl-tg:

```bash
kubectl tg version

Version: 1.5.0
Default version of TigerGraph cluster: 4.2.0
```

Upgrade TigerGraph Operator using kubectl-tg plugin:

```bash
kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version 1.5.0
```

#### Upgrading from TigerGraph Operator Versions Prior to 1.0.0

This TigerGraph Operator version upgrade introduces breaking changes if you are upgrading from TigerGraph Operator versions prior to 1.0.0. You need to upgrade the TigerGraph Operator, CRD, and the TigerGraph cluster following specific steps.

Refer to the documentation [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md) for details.

## New features

- CRR support on K8s within the same cluster and across clusters.
- Automated cluster monitoring during creation.
- Upgrade pre-check support for TigerGraph upgrades on K8s.
- Rolling upgrades for maintenance releases on K8s.
- Backoff delay for job retries to improve cluster operation resilience.
- Supports debug mode for continuous pod restarts caused by an unrecoverable PostStartHookError

## Improvements

- Enhanced Rolling Upgrade orchestration by the Operator.
- Upgrade pre-check separated from switch_version.sh.
- Cluster name and namespace visibility in IFM metrics endpoint.
- Validation webhook added to reject invalid ephemeral local storage limits.
- Base image upgraded to Ubuntu 22.04 for TigerGraph K8s Docker image.
- Retry mechanism for gadmin start --local in PostStart script.
- Predelete action skipped when an image pull fails.

## Bug Fixes

- SSH configuration update (UsePAM=no) to allow setting allowPrivilegeEscalation=false for SecurityContext.
