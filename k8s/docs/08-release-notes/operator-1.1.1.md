# Operator 1.1.1 Release notes

## Overview

**Operator 1.1.1** is now available, designed to work seamlessly with **TigerGraph version 3.10.2**.

Operator 1.1.1 is a patch version of 1.1.0. There are no new features, just some improvements and bug fixes.

### kubectl plugin installation

To install the kubectl plugin for Operator 1.1.1, execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/1.1.1/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### Operator upgrading

#### Upgrading from Operator 1.0.0 and 1.1.0

There are no changes in CRD for 1.1.1, you can upgrade the operator directly if you have an old operator version 1.0.0 or 1.1.0 installed.

Upgrade Operator using kubectl-tg plugin:

```bash
kubectl tg upgrade --namespace ${YOUR_NAMESPACE_OF_OPERATOR} --operator-version 1.1.1
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

- Validate the format of the backup tag in Webhook ([TP-5374](https://graphsql.atlassian.net/browse/TP-5374))

- Support setting the ExternalTrafficPolicy of external services to local or cluster based on the TG version in the operator ([TP-5425](https://graphsql.atlassian.net/browse/TP-5425))

## Bugfixes

- Support values in JSON array format in field tigergraph.spec.tigergraphConfig ([TP-5365](https://graphsql.atlassian.net/browse/TP-5425))

- Fixed the issue with running the gcollect command of TigerGraph on Kubernetes ([TP-6341](https://graphsql.atlassian.net/browse/TP-5425))
