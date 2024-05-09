# Operator 1.1.0 Release notes

## Overview

**Operator 1.1.0** is now available, designed to work seamlessly with **TigerGraph version 3.10.1**.

Kubernetes Operator support is now **generally available** in Operator version 1.1.0, suitable for production deployments.

### kubectl plugin installation

To install the kubectl plugin for Operator 1.1.0, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.1.0/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### Operator upgrading

#### Upgrading from Operator 1.0.0

There are no changes in CRD for 1.1.0, you can upgrade the operator directly if you have an old operator version 1.0.0 installed.

Upgrade Operator using kubectl-tg plugin:

```bash
kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version 1.1.0
```

#### Upgrading from Operator versions prior to 1.0.0

This new operator version upgrade brings breaking changes if you upgrade it from from Operator versions prior to 1.0.0.

Refer to the documentation [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md) for details.

- Delete the existing TG cluster and retain the PVCs:

```bash
# You should take note of the cluster size, HA and so on before you delete it, you'll use it when you recreate the cluster
# You can export the yaml resource file of TG cluster for the later restoring
kubectl tg export --cluster-name ${YOUR_CLUSTER_NAME} -n ${NAMESPACE_OF_CLUSTER}
kubectl tg delete --cluster-name ${YOUR_CLUSTER_NAME} -n ${NAMESPACE_OF_CLUSTER}
```

- Uninstall the old version of the Operator:

```bash
kubectl tg uninstall -n ${NAMESPACE_OF_OPERATOR}
```

- Delete old versions of TG CRDs:

```bash
kubectl delete crd tigergraphs.graphdb.tigergraph.com
kubectl delete crd tigergraphbackups.graphdb.tigergraph.com
kubectl delete crd tigergraphbackupschedules.graphdb.tigergraph.com
kubectl delete crd tigergraphrestores.graphdb.tigergraph.com
```

- Reinstall the new version of the Operator:

```bash
kubectl tg init -n ${NAMESPACE_OF_OPERATOR}
```

- Recreate the TigerGraph cluster if necessary:

Extract parameters from the backup YAML resource file generated in step 1, or modify the YAML resource file and apply it directly.

```bash
# You can get the following parameters from the backup yaml resoure file in step 1
kubectl tg create --cluster-name ${YOUR_CLUSTER_NAME} -n ${NAMESPACE_OF_CLUSTER} \
--size ${CLUSTER_SIZE} --ha ${CLUSTER_HA} --private-key-secret ${YOUR_PRIVATE_KEY_SECRET} \
--version ${TG_VERSION} --storage-class ${YOUR_STORAGE_CLASS} --storage-size ${YOUR_STORAGE_SIZE} --cpu 6000m --memory 10Gi
```

## Improvements

- Support overlap between ConfigUpdate and ConfigUpdate. Now when a config-update job is running, users are able to change .spec.tigergraphConfig. After the running job completes, another config-update job will run to apply the changes.([TP-4699](https://graphsql.atlassian.net/browse/TP-4699))

## Bugfixes

- Kubectl-tg plugin cannot remove tigergraphConfig/podLabels/podAnnotations fields of TigerGraph CR.([TP-5091](https://graphsql.atlassian.net/browse/TP-5091))

- Fix the watch namespace update issue of the operator in kubectl-tg plugin.([TP-5280](https://graphsql.atlassian.net/browse/TP-5280))

- Fix the issue of Nginx DNS cache for tigergraph on K8s ([TP-5360](https://graphsql.atlassian.net/browse/TP-5360))
