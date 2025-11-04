# TigerGraph Operator 1.7.0 Release notes

## Overview

**TigerGraph Operator 1.7.0** is now available, designed to work seamlessly with **TigerGraph version 4.3.0**.

This release introduces significant new features, enhancements, and bug fixes, including:

- Monitoring and Alerting Service Provisioning on the TigerGraph Operator.
- Customizing Prometheus rules and AlertManager configuration through the TigerGraph monitor CR.
- Creating default alerting rules and corresponding Grafana dashboards during Operator installation.
- Support exposing TigerGraph metrics to Prometheus when the SSL of the Nginx service is enabled.
- Support point-in-time restore in Kubernetes Operator.
- Optimize the Dependencies Management between the incremental backup and the full backup.
- Add support for enabling mTLS for TigerGraph on Kubernetes.
- Support configuring Nginx SSL by the TigerGraph Kubernetes operator.
- Enhance Error Handling for Cluster Operations with DB Freeze Mode.
- Skip the license status check in the readiness probe for TigerGraph versions that support keeping all services online after the license expires.

For further details, see the sections below.

> [!IMPORTANT]
> TigerGraph Operator has had a breaking change since version 1.0.0. If you are still using a version older than 1.0.0, it is strongly recommended that you upgrade to version 1.7.0. Versions older than 1.0.0 have been deprecated.

### kubectl plugin installation

To install the kubectl plugin for TigerGraph Operator 1.7.0, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.7.0/kubectl-TigerGraph  -o kubectl-TigerGraph
sudo install kubectl-TigerGraph /usr/local/bin/
```

### TigerGraph Operator upgrading

#### Upgrading from TigerGraph Operator 1.0.0+ to 1.7.0

There are no breaking changes in the Custom Resource Definitions (CRDs) for version 1.7.0 compared to versions 1.0.0 and above. If you are running Operator 1.0.0 or later, upgrade using the following command:

> [!NOTE]
> There is currently no support for upgrading or deleting CRDs when upgrading or uninstalling the TigerGraph Operator due to the risk of unintentional data loss. It is necessary to upgrade TigerGraph CRDs manually for the operator version prior to 1.3.0. However, starting from Operator version 1.3.0, we use [Helm chart's pre-upgrade hook](https://helm.sh/docs/topics/charts_hooks/) to upgrade the CRDs automatically. You can ignore the first step if you upgrade the operator to version 1.3.0 or above.

> [!IMPORTANT]
> Please ensure that you have installed the `kubectl-TigerGraph` version 1.7.0 before upgrading TigerGraph Operator to version 1.7.0.

Ensure you have installed the correct version of kubectl-TigerGraph:

```bash
kubectl TigerGraph version

Version: 1.7.0
Default version of TigerGraph cluster: 4.3.0
```

Upgrade TigerGraph Operator using kubectl-TigerGraph plugin:

```bash
kubectl TigerGraph upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version 1.7.0
```

#### Upgrading from TigerGraph Operator Versions Prior to 1.0.0

This TigerGraph Operator version upgrade introduces breaking changes if you are upgrading from TigerGraph Operator versions prior to 1.0.0. You need to upgrade the TigerGraph Operator, CRD, and the TigerGraph cluster following specific steps.

Refer to the documentation [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md) for details.

## New features

- Support Point-in-Time Restore in TigerGraph Operator.
- Add support for enabling mutual TLS for TigerGraph on Kubernetes.
- Support configuring Nginx SSL by the Kubernetes operator.
- Customizing Prometheus rules and configuring AlertManager alerts through TigerGraph monitor CR.
- Monitoring and Alerting Service Provisioning on the TigerGraph Operator.

## Improvements

- Support exposing TigerGraph metrics to Prometheus when the SSL of the Nginx service is enabled.
- Make sure the Webhook of the namespaced operator only handles requests from a specific namespace.
- Enhance Error Handling for Cluster Operations with DB Freeze Mode.
- Skip the license status check in the readiness probe for TigerGraph versions that support keeping all services online after the license expires.
- Optimize the Dependencies Management between the incremental backup and the full backup.
- Added support for installing TigerGraph Operator using Helm charts with a default values.yaml.

## Bug Fixes

- Recover the changed backup config before running the cleanup job.
- Remove the restore staging path from the restore job while it is retrying.
