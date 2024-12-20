# TigerGraph Operator 1.4.0 Release notes

## Overview

**TigerGraph Operator 1.4.0** is now available, designed to work seamlessly with **TigerGraph version 4.1.2**.

This release introduces significant improvements and bug fixes, including:

- Support for customizing pod labels and annotations in the PodTemplateSpec of the StatefulSet managing TigerGraph pods.

- Support for retaining the last successfully scheduled backup CR in case the latest scheduled backups consistently fail

- Support for restoring clusters even when they are in a NotReady or Degraded state.

- Resolve the issue of inconsistent host lists during configuration updates with region awareness enabled.

- Fix the issue causing a rolling update to be triggered when the cluster is in a NotReady or Degraded state.

- Fix an issue where the file system owner could not be changed to user tigergraph during TigerGraph Init Container execution.

- Fix an issue of label and annotation parsing in kubectl-tg plugin.

For more details, refer to the section below.

> [!IMPORTANT]
> TigerGraph Operator has had a breaking change since version 1.0.0. If you are still using a version older than 1.0.0, it is strongly recommended that you upgrade to version 1.4.0. Versions older than 1.0.0 have been deprecated.

### kubectl plugin installation

To install the kubectl plugin for TigerGraph Operator 1.4.0, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.4.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### TigerGraph Operator upgrading

#### Upgrading from TigerGraph Operator 1.0.0 and later versions to version 1.4.0

There are no breaking changes in the TigerGraph CRDs for version 1.4.0 compared to versions 1.0.0 and above. You can upgrade the TigerGraph Operator by following these steps if an older version (1.0.0 or above) is installed.

> [!NOTE]
> There is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss. It is necessary to upgrade TigerGraph CRDs manually for the operator version prior to 1.3.0. However, starting from Operator version 1.3.0, we use [Helm chart’s pre-upgrade hook](https://helm.sh/docs/topics/charts_hooks/) to upgrade the CRDs automatically. You can ignore the first step if you upgrade the operator to version 1.3.0 or above.

> [!IMPORTANT]
> Please ensure that you have installed the `kubectl-tg` version 1.4.0 before upgrading TigerGraph Operator to version 1.4.0.

Ensure you have installed the correct version of kubectl-tg:

```bash
kubectl tg version

Version: 1.4.0
Default version of TigerGraph cluster: 4.1.2
```

Upgrade TigerGraph Operator using kubectl-tg plugin:

```bash
kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version 1.4.0
```

#### Upgrading from TigerGraph Operator versions prior to 1.0.0 to version 1.0.0 and above

This TigerGraph Operator version upgrade introduces breaking changes if you are upgrading from TigerGraph Operator versions prior to 1.0.0. You need to upgrade the TigerGraph Operator, CRD, and the TigerGraph cluster following specific steps.

Refer to the documentation [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md) for details.

## Improvements

- Support for customizing pod labels and annotations in the PodTemplateSpec of the StatefulSet managing TigerGraph pods.

- Support for retaining the last successfully scheduled backup CR in case the latest scheduled backups consistently fail.

- Support restoring the cluster when it is in NotReady or Degraded status.

## Bug Fixes

- Resolve the issue of inconsistent host lists during configuration updates with region awareness enabled.

- Fix the issue causing a rolling update to be triggered when the cluster is in a NotReady or Degraded state.

- Fix an issue where the file system owner could not be changed to user tigergraph during TigerGraph Init Container execution.

- Fix an issue of label and annotation parsing in kubectl-tg plugin.
