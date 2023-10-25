# Operator 0.0.9 Release notes

## Overview

**Operator 0.0.9** has been released in conjunction with **TigerGraph version 3.9.3**.

In this release, **Operator 0.0.9** introduces support for two essential features: CompressLevel and DecompressProcessNumber, newly introduced in **TigerGraph 3.9.3**. To leverage these capabilities, it is imperative to upgrade both the Custom Resource Definition (CRD) and the Operator itself.

Operator 0.0.9 has disabled TG downgrades from a higher version (e.g., 3.9.3) to any lower version (e.g., 3.9.2). Therefore, the upgrade job will fail if you attempt to downgrade.

A significant security enhancement has been implemented in the TigerGraph 3.9.3 Docker image. This enhancement disables access to TigerGraph pods through the use of a static password. Consequently, it is important to note that installations of TigerGraph versions 3.9.3 and higher are only supported with Operator version 0.0.9 and above.

Additionally, Operator 0.0.9 introduces the Controller.ServiceManager.AutoRestart feature during cluster initialization. This enhancement ensures that services will automatically restart when using gadmin start/restart in the TigerGraph container.

### kubectl plugin installation

To install the kubectl plugin for Operator 0.0.9, please execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/0.0.9/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### CRD upgrading

To upgrade the Custom Resource Definition for Operator 0.0.9, please use the following command:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/0.0.9/tg-operator-crd.yaml
```

### Operator upgrading

> [!WARNING]
> For TigerGraph 3.9.3 and later versions, the use of passwords to log in to Pods is disabled, which enhances security. If you plan to upgrade your TigerGraph cluster to version 3.9.3, it is essential to first upgrade the Operator to version 0.0.9.

To upgrade the Operator to version 0.0.9, please use the following command:

```bash
kubectl tg upgrade --namespace ${NAMESPACE_OF_OPERATOR} --operator-version 0.0.9
```

## New features

- CompressLevel is now supported in `TigerGraphBackup` and `TigerGraphBackupSchedule`, with support for DecompressProcessNumber in TigerGraphRestore. These features require a cluster version of 3.9.3 or higher.([TP-4017](https://graphsql.atlassian.net/browse/TP-4017))

## Improvements

- The help message menu for the `kubectl-tg` plugin has been enhanced. ([TP-3915](https://graphsql.atlassian.net/browse/TP-3915))

- The `.spec.initTGConfig.version` field in TigerGraph CR is now optional. You no longer need to specify this field when creating or updating the CR. ([TP-3910](https://graphsql.atlassian.net/browse/TP-3910))

- Static passwords have been replaced with private keys for executing cluster operations jobs. ([TP-3792](https://graphsql.atlassian.net/browse/TP-3792))

- The make command has been added to support the installation of tsar, and password usage has been disabled when building the TG docker image. ([TP-3786](https://graphsql.atlassian.net/browse/TP-3786))

- Support for automatic restart of TigerGraph service under any circumstances has been introduced. ([TP-3848](https://graphsql.atlassian.net/browse/TP-3848) Database change)

- Service auto-restart in the Operator can now be enabled by setting the TG configuration Controller.ServiceManager.AutoRestart. ([TP-4045](https://graphsql.atlassian.net/browse/TP-4045))

## Bugfixes

- A situation where the cluster was cloned again when a restore had already succeeded has been rectified. ([TP-3948](https://graphsql.atlassian.net/browse/TP-3948))

- A problem with error handling in the TG container's PostStart Handler script has been resolved. ([TP-3914](https://graphsql.atlassian.net/browse/TP-3914))

- A restpp status refresh issue has been addressed. ([CORE-1905](https://graphsql.atlassian.net/browse/CORE-1905))

- GSQL jobs no longer get stuck when some related services are down. ([GLE-5365](https://graphsql.atlassian.net/browse/GLE-5365))

- An issue where expansion was stuck at importing gsql/gui has been fixed. ([TOOLS-2306](https://graphsql.atlassian.net/browse/TOOLS-2306))
