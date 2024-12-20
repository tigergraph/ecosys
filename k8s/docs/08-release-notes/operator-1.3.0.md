# TigerGraph Operator 1.3.0 Release notes

## Overview

**TigerGraph Operator 1.3.0** is now available, designed to work seamlessly with **TigerGraph version 4.1.1**.

Operator 1.3.0 introduces several significant new features, including achieving GA release for AKS, support for customizing external service ports for TG listeners and sidecar listeners and adding a pre-upgrade hook in the Operator Helm Chart to refresh CRDs during operator upgrades automatically.

> [!IMPORTANT]
> TigerGraph Operator has had a breaking change since version 1.0.0. If you are still using a version older than 1.0.0, it is strongly recommended that you upgrade to version 1.3.0. Versions older than 1.0.0 have been deprecated.

### kubectl plugin installation

To install the kubectl plugin for TigerGraph Operator 1.3.0, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.3.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### TigerGraph Operator upgrading

#### Upgrading from TigerGraph Operator 1.0.0 and later versions to version 1.3.0

There are no breaking changes in the TigerGraph CRDs for version 1.3.0 compared to versions 1.0.0 and above. You can upgrade the TigerGraph Operator by following these steps if an older version (1.0.0 or above) is installed.

> [!NOTE]
> There is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss. It is necessary to upgrade TigerGraph CRDs manually for the operator version prior to 1.3.0. However, starting from Operator version 1.3.0, we use [Helm chart’s pre-upgrade hook](https://helm.sh/docs/topics/charts_hooks/) to upgrade the CRDs automatically. You can ignore the first step if you upgrade the operator to version 1.3.0 or above.

Upgrade the TigerGraph CRDs to the latest version(It's required for the operator version prior to 1.3.0)

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/1.3.0/tg-operator-crd.yaml
```

> [!IMPORTANT]
> Please ensure that you have installed the `kubectl-tg` version 1.3.0 before upgrading TigerGraph Operator to version 1.3.0.

Ensure you have installed the correct version of kubectl-tg:

```bash
kubectl tg version

Version: 1.3.0
Default version of TigerGraph cluster: 4.1.1
```

Upgrade TigerGraph Operator using kubectl-tg plugin:

```bash
kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version 1.3.0
```

#### Upgrading from TigerGraph Operator versions prior to 1.0.0 to version 1.0.0 and above

This TigerGraph Operator version upgrade introduces breaking changes if you are upgrading from TigerGraph Operator versions prior to 1.0.0. You need to upgrade the TigerGraph Operator, CRD, and the TigerGraph cluster following specific steps.

Refer to the documentation [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md) for details.

## New features

- Operator 1.3.0 achieves GA for AKS, ensuring improved stability and support for Azure Kubernetes Service.

- Support for customizing external service port for TG listener and sidecar listener.

- Add a pre-upgrade hook in operator helm chart to refresh CRD automatically during operator upgrade.

## Improvements

- Add a new status Degraded in operator to indicate that a region-aware cluster is partially available.

- Make the maximum number of retries for the init-job configurable via annotations.

- Add namespace as a suffix of HostName in HostList on K8s.

- Support for configuring nodeSelector for operator pods.

- Avoid too many events on SuccessfulDeleteService.

- Improve log of Operator Jobs by removing redundant gadmin output.

## Bug Fixes

- Inconsistent pre-check error message for a non-region-aware cluster in k8s operator.

- Verify the existence of S3 secret for restoring from s3.

- Don't set default backup compress process number.
