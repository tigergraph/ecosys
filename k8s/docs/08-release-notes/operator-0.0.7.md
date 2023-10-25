# Operator 0.0.7 Release notes

## Overview

**Operator 0.0.7** has been released in conjunction with **TigerGraph 3.9.2**.

This release of **Operator 0.0.7** brings several noteworthy features and improvements. It supports custom labels and annotations for external services, simplifies the backup and restore process for the TigerGraph cluster by eliminating the need to specify meta files, and allows for license updates through the **kubectl-tg** plugin.

### kubectl plugin installation

To install the kubectl plugin for **Operator 0.0.7**, please execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/0.0.7/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### CRD upgrading

To upgrade the Custom Resource Definition for Operator 0.0.7, please use the following command:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/0.0.7/tg-operator-crd.yaml
```

### Operator upgrading

To upgrade the Operator to version 0.0.7, please use the following command:

```bash
kubectl tg upgrade --namespace ${NAMESPACE_OF_OPERATOR} --operator-version 0.0.7
```

## New features

- Operator 0.0.7 now supports the addition of custom labels and annotations to external services.

- Backup and restore processes for the TigerGraph cluster have been streamlined, eliminating the need to specify meta files.

- You can now conveniently update your TigerGraph license using the kubectl tg plugin.

## Improvements

- Enhancements have been made to improve the handling of overlapping operations between expansion/shrinking and upgrading.

- A retry interval for failed jobs has been added, improving job reliability.

- The terminationGracePeriodSeconds of the TigerGraph container has been increased to 300 seconds from 60 seconds for smoother termination.

## Bugfixes

- Resolved issues related to deploying the cluster using static PV with the local filesystem.

- Fixed the problem of external services updating twice, which led to errors.

- Corrected unexpected config updates when executing an overlap operation between upgrade and expansion. ([TP-3646](https://graphsql.atlassian.net/browse/TP-3646))

- Addressed an incorrect error exit issue in the upgrade script. ([TP-3869](https://graphsql.atlassian.net/browse/TP-3869))

- Fixed the issue of cluster status checking during expansion and shrinking. It now checks the service of all nodes, not just the client node.

- Graph query responses no longer encounter errors after successful execution of the expansion. ([GLE-5195](https://graphsql.atlassian.net/jira/software/c/projects/GLE/issues/GLE-5195), TigerGraph 3.9.2)

- Resolved the issue of cluster size limits during cluster expansion. ([TP-3768](https://graphsql.atlassian.net/browse/TP-3768))

- Fixed unnecessary rolling update problems that occurred when upgrading from 3.7.0 and below to 3.9.0 and above. ([TP-3765](https://graphsql.atlassian.net/browse/TP-3765) & [CORE-2585](https://graphsql.atlassian.net/browse/CORE-2585))
