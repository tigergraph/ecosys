# TigerGraph Operator 1.8.0 Release notes

## Overview

**TigerGraph Operator 1.8.0** is now available, designed to work seamlessly with **TigerGraph version 4.3.0**.

This release introduces new features, enhancements, and bug fixes, including:

- Support backup and restore with EKS IRSA based S3 access and cross-region buckets.
- Support Storage/Compute Separation (SCS) clusters with Google Cloud.
- Support controller-runtime and other libraries for K8S 1.35.

For further details, see the sections below.

> [!IMPORTANT]
> TigerGraph Operator has had a breaking change since version 1.0.0. If you are still using a version older than 1.0.0, it is strongly recommended that you upgrade to version 1.8.0. Versions older than 1.0.0 have been deprecated.

### kubectl plugin installation

To install the kubectl plugin for TigerGraph Operator 1.8.0, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.8.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### TigerGraph Operator upgrading

#### Upgrading from TigerGraph Operator 1.0.0+ to 1.8.0

There are no breaking changes in the Custom Resource Definitions (CRDs) for version 1.8.0 compared to versions 1.0.0 and above. If you are running Operator 1.0.0 or later, upgrade using the following command:

> [!NOTE]
> There is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss. It is necessary to upgrade TigerGraph CRDs manually for the operator version prior to 1.3.0. However, starting from Operator version 1.3.0, we use [Helm chart's pre-upgrade hook](https://helm.sh/docs/topics/charts_hooks/) to upgrade the CRDs automatically. You can ignore the first step if you upgrade the operator to version 1.3.0 or above.

> [!IMPORTANT]
> Please ensure that you have installed the `kubectl-tg` version 1.8.0 before upgrading TigerGraph Operator to version 1.8.0.

Ensure you have installed the correct version of kubectl-tg:

```bash
kubectl tg version

Version: 1.8.0
Default version of TigerGraph cluster: 4.3.0
```

Upgrade TigerGraph Operator using kubectl-tg plugin:

```bash
kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version 1.8.0
```

#### Upgrading from TigerGraph Operator Versions Prior to 1.0.0

This TigerGraph Operator version upgrade introduces breaking changes if you are upgrading from TigerGraph Operator versions prior to 1.0.0. You need to upgrade the TigerGraph Operator, CRD, and the TigerGraph cluster following specific steps.

Refer to the documentation [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md) for details.

## New features

- Support IAM role based S3 access for backup and restore with the new `s3Bucket.iamAuth` field. See [Backup and restore with EKS IRSA](../04-manage/backup-and-restore/backup-restore-with-eks-irsa.md).
- Support multi-region aware backup and restore with the new `s3Bucket.region` field, so you can restore a backup into a cluster that runs in a different region from the bucket that holds it.
- Support Storage/Compute Separation (SCS) clusters with Google Cloud.

## Improvements

- Upgrade the operator dependencies to controller-runtime v0.23.4 and the Kubernetes 1.35 client libraries.

## Bug Fixes

- CVE fix: bump `golang.org/x/net` to v0.60.0 in the operator.
- CVE fix: bump `google.golang.org/grpc` to v1.83.2 and `github.com/google/cel-go` in the operator.
- CVE fix: bump the Go toolchain and fix the reported vulnerabilities of the operator and the initialization container images.
