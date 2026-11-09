# Backup and restore with EKS IRSA (and multi-region DR)

Starting with TigerGraph Operator `1.8.0` and TigerGraph `4.3.0`, the operator
supports authenticating S3 backup and restore via the AWS default credential
chain. This is opted into per-CR by setting `s3Bucket.iamAuth: true`. On EKS
this means **IAM Roles for Service Accounts (IRSA)**: the TigerGraph cluster
pods receive a projected web-identity token, and the AWS SDK inside
`gadmin` exchanges it for temporary credentials. No static access
keys, no hand-rolled `~/.aws/config`.

This document covers two related capabilities:

1. **IRSA-based S3 authentication** for backup and restore.
2. **Multi-region DR**: backing up to a bucket in one region, restoring into a
   cluster running in another region, using the new `s3Bucket.region` field.

For the older approach using a Kubernetes `Secret` with access keys or a
manually-mounted `~/.aws/config`, see
[Create TigerGraph cluster with access to S3](./create-tg-with-access-to-s3.md).

## Prerequisites

- An EKS cluster with an IAM OIDC provider associated. See the
  [AWS docs](https://docs.aws.amazon.com/eks/latest/userguide/enable-iam-roles-for-service-accounts.html).
- The `eksctl` CLI, or equivalent IAM tooling.

## Step 1 — Create the IAM role and policy

Create an IAM policy granting S3 read/write on the backup bucket:

```bash
aws iam create-policy --policy-name TigerGraphBackupS3Policy --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::operator-backup",
                "arn:aws:s3:::operator-backup/*"
            ]
        }
    ]
}'
```

Create a Kubernetes `ServiceAccount` bound to an IAM role whose trust policy
permits the cluster's OIDC subject for that SA:

```bash
eksctl create iamserviceaccount \
  --name tg-irsa-sa \
  --namespace tigergraph \
  --cluster your-eks-cluster \
  --role-name TigerGraphBackupRole \
  --attach-policy-arn arn:aws:iam::123456789012:policy/TigerGraphBackupS3Policy \
  --approve
```

This creates a `ServiceAccount` named `tg-irsa-sa` annotated with
`eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/TigerGraphBackupRole`.

## Step 2 — Point the TigerGraph cluster at the ServiceAccount

In your `TigerGraph` CR, set `spec.serviceAccountName`:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  serviceAccountName: tg-irsa-sa
  # ... other fields ...
```

Backup, restore, and backup-clean Jobs created by the operator inherit this
ServiceAccount, so the EKS admission webhook automatically projects the
web-identity token into each pod at
`/var/run/secrets/eks.amazonaws.com/serviceaccount/token`.

## Step 3 — Create a TigerGraphBackup with `iamAuth: true`

Set `iamAuth: true` on the S3 destination. Leave `secretKeyName` and `roleARN`
unset — they are ignored when IAMAuth is true. The operator skips mounting
any S3 secret volume, and `gadmin` falls back to the AWS SDK default
credential chain, which on EKS resolves the IRSA web-identity token.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphBackup
metadata:
  name: daily-backup
  namespace: tigergraph
spec:
  clusterName: test-cluster
  destination:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      iamAuth: true
      region: us-west-2          # explicit region, optional
  backupConfig:
    tag: daily
    timeout: 18000
```

See `config/samples/graphdb_v1alpha1_tigergraphbackup_s3_irsa.yaml`.

## Step 4 — Cross-region restore (multi-region DR)

To restore into a DR cluster running in a different region than where the
backup was written, point the restore CR at the same bucket and set
`s3Bucket.region` to the bucket's region.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: dr-restore
  namespace: tigergraph
spec:
  clusterName: dr-cluster      # cluster in region us-east-1
  source:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      iamAuth: true
      region: us-west-2        # backup was written here
  restoreConfig:
    tag: daily
```
