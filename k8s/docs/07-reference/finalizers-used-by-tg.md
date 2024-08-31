# Finalizers used by TigerGraph Operator

The TigerGraph Operator uses [finalizers](https://kubernetes.io/docs/concepts/overview/working-with-objects/finalizers/) to ensure that resources are cleaned up when CRs are deleted. The following finalizers are used by the TigerGraph Operator:

| Finalizer | Used by | Description |
| --- | --- | --- |
| `tigergraph.com/tg-protection` | TigerGraph | Ensure that the TigerGraph Pods are cleaned before the CR is deleted. |
| `tigergraph.com/tgbackup-protection` | TigerGraphBackup | Ensure that the backup files are cleaned before the CR is deleted when `.spec.cleanPolicy` is `Delete`. |
