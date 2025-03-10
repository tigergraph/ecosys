# Backup & Restore cluster by CR

> [!IMPORTANT]
> There are many examples on different conditions of backup and restore. Some fields in the YAML format CR is optional, a mark `# Optional` is put above them. All fields without the optional mark is required.

> [!WARNING]
> If you are using CRs to backup and restore, we highly recommend you to avoid modifying the backup configurations by directly excuting `gadmin config` in the pod.
> If you do that, the configurations set by CR and gadmin config may conflict, leading to some unknown behavior.

- [Backup \& Restore cluster by CR](#backup--restore-cluster-by-cr)
  - [Guarantee the access to S3 Bucket](#guarantee-the-access-to-s3-bucket)
    - [Use AWS Access Key and Secret Access Key](#use-aws-access-key-and-secret-access-key)
    - [Create a cluster with access to S3 (Supported from Operator version 1.2.0 and TigerGraph 4.1.0)](#create-a-cluster-with-access-to-s3-supported-from-operator-version-120-and-tigergraph-410)
  - [TigerGraphBackup](#tigergraphbackup)
    - [Backup to local storage](#backup-to-local-storage)
    - [Backup to S3 bucket](#backup-to-s3-bucket)
      - [Use RoleARN instead of access key to access S3 Bucket](#use-rolearn-instead-of-access-key-to-access-s3-bucket)
    - [About cleanPolicy](#about-cleanpolicy)
    - [About backoffRetryPolicy](#about-backoffretrypolicy)
  - [TigerGraphBackupSchedule](#tigergraphbackupschedule)
    - [Schedule backup to local storage](#schedule-backup-to-local-storage)
    - [Schedule backup to S3 bucket](#schedule-backup-to-s3-bucket)
      - [Use RoleARN instead of access key to access S3 Bucket in TigerGraphBackupSchedule](#use-rolearn-instead-of-access-key-to-access-s3-bucket-in-tigergraphbackupschedule)
  - [TigerGraphRestore](#tigergraphrestore)
    - [Restore from local backup](#restore-from-local-backup)
    - [Restore from backup in S3 bucket](#restore-from-backup-in-s3-bucket)
      - [Use RoleARN instead of access key to access S3 Bucket in TigerGraphRestore](#use-rolearn-instead-of-access-key-to-access-s3-bucket-in-tigergraphrestore)
    - [Cross-cluster restore in existing cluster](#cross-cluster-restore-in-existing-cluster)
      - [Cluster version \>=3.9.2](#cluster-version-392)
    - [Clone a cluster(Create a new cluster and do cross-cluster restore)](#clone-a-clustercreate-a-new-cluster-and-do-cross-cluster-restore)
      - [Clone Cluster version \>=3.9.2](#clone-cluster-version-392)

## Guarantee the access to S3 Bucket

When working with backup and restore operations involving S3 buckets, you need to guarantee the access to the S3 bucket. Currently we support two ways to achieve this:

### Use AWS Access Key and Secret Access Key

Create a Kubernetes Secret to securely store your AWS access credentials. Here's how you can create an S3 Secret:

1. **Encode AWS Access Key ID and Secret Access Key**:

   Before creating the Kubernetes Secret, you need to encode your AWS access key ID and secret access key in base64 format. You can use the following commands to do that:

   ```bash
   # Replace YOUR_ACCESS_KEY_ID with your actual AWS access key ID
   echo -n "YOUR_ACCESS_KEY_ID" | base64

   # Replace YOUR_SECRET_ACCESS_KEY with your actual AWS secret access key
   echo -n "YOUR_SECRET_ACCESS_KEY" | base64
   ```

   Note down the base64 encoded strings generated for the access key ID and secret access key.

2. **Create S3 Secret YAML**:

   Create a YAML file (e.g., `s3-secret.yaml`) with the following content. Replace `YOUR_BASE64_ENCODED_ACCESS_KEY_ID` and `YOUR_BASE64_ENCODED_SECRET_ACCESS_KEY` with the actual base64 encoded values from step 1:

   ```yaml
   apiVersion: v1
   kind: Secret
   metadata:
     name: s3-secret
   type: Opaque
   data:
     accessKeyID: YOUR_BASE64_ENCODED_ACCESS_KEY_ID
     secretAccessKey: YOUR_BASE64_ENCODED_SECRET_ACCESS_KEY
   ```

3. **Apply the Secret**:

   Use the `kubectl apply` command within the same namespace as the cluster you intend to backup. This ensures that the secret is accessible to the backup and restore processes within that specific namespace.

   ```bash
   kubectl apply -f s3-secret.yaml -n YOUR_NAMESPACE
   ```

   This will create the Kubernetes Secret named `s3-secret` containing your AWS access credentials.

By creating an S3 Secret in this manner, you ensure that your AWS access credentials are securely stored and can be easily referenced when needed for backup and restore tasks involving S3 buckets.

### Create a cluster with access to S3 (Supported from Operator version 1.2.0 and TigerGraph 4.1.0)

If you want to use RoleARN instead of access key in TigerGraphBackup/TigerGraphRestore CR, you can create a cluster with access to S3. You can refer to [Create a TigerGraph cluster with access to S3](./create-tg-with-access-to-s3.md).

## TigerGraphBackup

For optimal organization, we recommend using the naming convention `${CLUSTER-NAME}-backup-${TAG}` for your backup CR.

### Backup to local storage

Certainly, here's the modified YAML file for performing a backup to local storage. You can save this content to a file (e.g., backup-local.yaml), and then run `kubectl apply -f backup-local.yaml -n YOUR_NAMESPACE` to create the backup.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphBackup
metadata:
  name: test-cluster-backup-local
spec:
  # Specify which cluster to backup in the SAME NAMESPACE as the backup job
  clusterName: test-cluster
  # Specify where to store the backup data
  destination:
    storage: local
    # Use this field if type is local
    local:
      path: /home/tigergraph/tigergraph/data/backup
  
  # Configure the name of backup files and the path storing temporary files
  backupConfig:
    # Optional: Set the tag of the backup, if not set, the tag will be the name of this CR
    # Note: this field is Required for TigerGraph Operator < 1.1.0
    tag: local
    # Optional: Set the path for temporary staging files
    stagingPath: /home/tigergraph/tigergraph/data
    # Optional: If 'incremental' is set to true, incremental backup will be performed
    incremental: false
    # Optional: Set the timeout value for the backup process (default is 18000 seconds)
    timeout: 18000
    # Optional: Specify the number of processes to use for compression (0 uses the number of CPU cores)
    compressProcessNumber: 0
    # Optional: (Requires TigerGraph Operator >= 0.0.9 and TigerGraph >= 3.9.3)
    # Choose the compression level for the backup: DefaultCompression/BestSpeed/BestCompression
    compressLevel: DefaultCompression # Choose from DefaultCompression/BestSpeed/BestCompression
  
  # Optional: Set the policy for cleaning up backup package when deleting the backup CR
  # Choose from Delete/Retain
  # The default behavior is to retain the backup package.
  # If you want to delete the backup package when deleting the backup CR, 
  # you can set the cleanPolicy to Delete. 
  # With Delete policy, 
  # TigerGraph Operator will create a backup-clean-job when the backup CR is deleted, 
  # to make sure that the backup package is removed before deleting the backup CR.
  cleanPolicy: Delete

  # Optional: Set the retry policy for backup CR
  backoffRetryPolicy:
    # set maxRetryTimes for backup CR
    maxRetryTimes: 3
    # set the min duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    minRetryDuration: 5s
    # set the max duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    maxRetryDuration: 10s
    # If the value is true, the deletion of backup CR won't be blocked by failed backup-clean-job
    # that means, when backup-clean-job exceeds the maxRetryTimes
    # the backup CR will be deleted directly, the backup package still exists in cluster
    forceDeleteAfterMaxRetries: false
```

> [!NOTE]
> Please use subpath of `/home/tigergraph/tigergraph/data/` as local path for backup since this path is mounted with PV. For example, you can use `/home/tigergraph/tigergraph/data/mybackup` .If you do not use that, you will lose your backup data if the pod restarts.
>
> And be careful that don’t use the same path for local path as the staging path. If you don’t configure staging path, the default staging path is `/home/tigergraph/tigergraph/data/backup`(version < 3.10.0) or `/home/tigergraph/tigergraph/data/backup_staging_dir/backup` (version >= 3.10.0),
> if you set local path as `/home/tigergraph/tigergraph/data/backup` for TigerGraph < 3.10.0 or `/home/tigergraph/tigergraph/data/backup_staging_dir/backup` for TigerGraph >= 3.10.0, the backup will fail.
>
> If you configure staging path by `.spec.backupConfig.stagingPath`, the actual staging path will be `${stage_path}/backup`. For example, if you set `stagingPath: /home/tigergraph/temp`, the actual staging path will be `/home/tigergraph/temp/backup`. And you should not use `/home/tigergraph/temp/backup` as local path.

> [!IMPORTANT]
> Please remember which local path you use and use the same path if you want to restore the backup package you create.

### Backup to S3 bucket

Assume that you have already created an S3 Secret named `s3-secret` containing your AWS access credentials following the instructions in the previous section [Use AWS Access Key and Secret Access Key](#use-aws-access-key-and-secret-access-key).

Certainly, here's the YAML file for performing a backup to an S3 bucket using a previously created Secret named `s3-secret`. You can save this content to a file (e.g., `backup-s3.yaml`), and then run `kubectl apply -f backup-s3.yaml -n YOUR_NAMESPACE` to create the backup.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphBackup
metadata:
  name: test-cluster-backup-s3
spec:
  # Specify which cluster to backup in the SAME NAMESPACE as the backup job
  clusterName: test-cluster
  destination:
    storage: s3Bucket
    s3Bucket:
      # Specify the name of the S3 bucket you want to use
      bucketName: operator-backup
      # Specify the Secret containing the S3 access key and secret access key
      secretKeyName: s3-secret

  # Configure the name of backup files and the path storing temporary files
  backupConfig:
    # Optional: Set the tag of the backup, if not set, the tag will be the name of this CR
    # Note: this field is Required for TigerGraph Operator < 1.1.0
    tag: s3
    # Optional: Set the path for temporary staging files
    stagingPath: /home/tigergraph/tigergraph/data
    # Optional: If 'incremental' is set to true, incremental backup will be performed
    incremental: false
    # Optional: Set the timeout value for the backup process (default is 18000 seconds)
    timeout: 18000
    # Optional: Specify the number of processes to use for compression (0 uses the number of CPU cores)
    compressProcessNumber: 0
    # Optional: (Requires TigerGraph Operator >= 0.0.9 and TigerGraph >= 3.9.3)
    # Choose the compression level for the backup: DefaultCompression/BestSpeed/BestCompression
    compressLevel: DefaultCompression # Choose from DefaultCompression/BestSpeed/BestCompression

  # Optional: Set the policy for cleaning up backup package when deleting the backup CR
  # Choose from Delete/Retain
  # The default behavior is to retain the backup package.
  # If you want to delete the backup package when deleting the backup CR, 
  # you can set the cleanPolicy to Delete. 
  # With Delete policy, 
  # TigerGraph Operator will create a backup-clean-job when the backup CR is deleted, 
  # to make sure that the backup package is removed before deleting the backup CR.
  cleanPolicy: Delete

  # Optional: Set the retry policy for backup CR
  backoffRetryPolicy:
    # set maxRetryTimes for backup CR
    maxRetryTimes: 3
    # set the min duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    minRetryDuration: 5s
    # set the max duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    maxRetryDuration: 10s
    # If the value is true, the deletion of backup CR won't be blocked by failed backup-clean-job
    # that means, when backup-clean-job exceeds the maxRetryTimes
    # the backup CR will be deleted directly, the backup package still exists in cluster
    forceDeleteAfterMaxRetries: false
```

#### Use RoleARN instead of access key to access S3 Bucket

Assume that you have already created a cluster with access to S3 following the instructions in the previous section [Create a cluster with access to S3](#create-a-cluster-with-access-to-s3-supported-from-operator-version-120-and-tigergraph-410).

Then you can replace the `.spec.destination` field with the following content:

```yaml
spec:
  destination:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      roleARN: arn:aws:iam::123456789012:role/role-name
```

> [!NOTE]
> In TigerGraph Operator 1.2.0, you are allowed to set `roleARN` and `secretKeyName` at the same time.
> If you set both of them, TigerGraph will only use `roleARN` and ignore the `secretKeyName`.
> So we highly recommend you to use just one of them.

### About cleanPolicy

> [!IMPORTANT]
> If you want to delete a backup CR whose cleanPolicy is Delete, you should make sure that
> the cluster is running and the backup package is not used by any other incremental backup CR. Otherwise, the `backup-clean-job` will fail and the deletion of the backup CR will be blocked,
> because when you set the clean policy to Delete, TigerGraph Operator will make sure that the backup package is removed before deleting the backup CR.
>
> If for some reason, the backup package cannot be deleted, and you do not want the deletion of backup CR to be blocked, you can configure `forceDeleteAfterMaxRetries: true` when creating the backup CR. Or update the `cleanPolicy` to `Retain`. When the backup CR is successfully deleted, you need to manually clear the backup package yourself.

### About backoffRetryPolicy

> [!NOTE]
> When backup job failed, the backup CR will perform exponential backoff retry. The duration between two retries will start from `minRetryDuration` and double every time until it reaches `maxRetryDuration`.
> The backup CR will retry util exceeding `maxRetryTimes`. If the backup job is still failed after exceeding `maxRetryTimes`, the backup CR will be marked as failed.

## TigerGraphBackupSchedule

Since Operator version 1.1.0, the backup schedule CRs will create TigerGraphBackup CR at scheduled time to achieve creating backup periodically. The backup schedule CRs will manage the backup CRs created by the schedule according to the strategies. The backup schedule CRs will delete the oldest backup CRs when the number of backup CRs exceeds the `maxBackupFiles` or the backup CRs exist for more than `maxReservedDays` days. The backup schedule CRs will also retry the backup CRs when the backup CRs failed according to the `backoffRetryPolicy`

> [!WARNING]
> When a backup schedule is deleted, the backup CRs created by the schedule will also be deleted. If you want to keep the backup CRs, you should update the backup clean policy to Retain for all backup CRs
> created by the schedule before deleting the schedule. Or you can pause the backup schedule instead of deleting it.

The field `.spec.schedule` uses the cron schedule expression. You can refer to [https://crontab.guru/](https://crontab.guru/).

The field `.spec.backupTemplate` is the same as the `.spec` of TigerGraphBackup

### Schedule backup to local storage

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphBackupSchedule
metadata:
  name: test-cluster-schedule-daily
spec:
  # Cronjob shedule
  schedule: "0 0 * * *"
  # Strategies for managing backups
  # We will delete oldest backups according to the strategies automatically
  strategy:
    # We will only retain 20 backups
    maxBackupFiles: 20
    # A backup can only exist for 3 days
    maxReservedDays: 3
    maxRetry: 10 
  # Optional : is pause is true, the cronjob will be suspended
  pause: false
  backupTemplate:
  # Specify which cluster to backup in the SAME NAMESPACE as the backup job
    clusterName: test-cluster
    # Specify where to store the backup data
    destination:
      storage: local
      # Use this field if type is local
      local:
        path: /home/tigergraph/tigergraph/data/backup
    
    # Configure the name of backup files and the path storing temporary files
    backupConfig:
      # Optional: Set the tag of the backup, if not set, the tag will be the name of this CR
      # Note: this field is Required for TigerGraph Operator < 1.1.0
      tag: daily
      # Optional
      stagingPath: /home/tigergraph/tigergraph/data
      # Optional :if incremental is true, incremental backup will be performed
      incremental: false
      # Optional
      timeout: 18000
      # Optional :specify the number of process to do compress
      compressProcessNumber: 0
      # Optional: (TigerGraph Operator >=0.0.9 and TigerGraph >=3.9.3) specify the compress level for backup
      compressLevel: DefaultCompression #choose from DefaultCompression/BestSpeed/BestCompression
      
    # Optional: Set the policy for cleaning up backup package when deleting the backup CR
    # Choose from Delete/Retain
    # For backup CR created by backup schedule, 
    # the default behavior is to delete the backup package. 
    # The backup schedule CR use this feature to keep maxBackupFiles.
    # If you want to keep all backup packages created by backup schedule,
    # you can set the cleanPolicy to Retain. 
    # But at the same time, the maxBackupFiles and maxReservedDays won't work properly.
    cleanPolicy: Delete

    # Optional: Set the retry policy for backup CR
    backoffRetryPolicy:
      # set maxRetryTimes for backup CR
      maxRetryTimes: 3
      # set the min duration between two retries, 
      # the format is like "5s","10m","1h","1h20m5s"
      minRetryDuration: 5s
      # set the max duration between two retries, 
      # the format is like "5s","10m","1h","1h20m5s"
      maxRetryDuration: 10s
      # If the value is true, the deletion of backup CR won't be blocked by failed backup-clean-job
      # that means, when backup-clean-job exceeds the maxRetryTimes
      # the backup CR will be deleted directly, the backup package still exists in cluster
      forceDeleteAfterMaxRetries: false
```

### Schedule backup to S3 bucket

Assume that you have already created an S3 Secret named `s3-secret` containing your AWS access credentials following the instructions in the previous section [Use AWS Access Key and Secret Access Key](#use-aws-access-key-and-secret-access-key).

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphBackupSchedule
metadata:
  name: test-cluster-schedule-daily
spec:
  # Cronjob shedule
  schedule: "0 0 * * *"
  # Strategies for managing backups
  # We will delete oldest backups according to the strategies automatically
  strategy:
    # We will only retain 20 backups
    maxBackupFiles: 20
    # A backup can only exist for 3 days
    maxReservedDays: 3
    maxRetry: 10 
  # Optional : is pause is true, the cronjob will be suspended
  pause: false
  backupTemplate:
    clusterName: test-cluster
    destination:
      storage: s3Bucket
      s3Bucket:
        # specify the bucket you want to use
        bucketName: operator-backup
        secretKeyName: s3-secret
    # Configure the name of backup files and the path storing temporary files
    backupConfig:
      # Optional: Set the tag of the backup, if not set, the tag will be the name of this CR
      # Note: this field is Required for TigerGraph Operator < 1.1.0
      tag: s3-daily
      # Optional
      stagingPath: /home/tigergraph/tigergraph/data/backup-staging
      # Optional :if incremental is true, incremental backup will be performed
      incremental: false
      # Optional
      timeout: 18000
      # Optional :specify the number of process to do compress
      compressProcessNumber: 0
      # Optional: (TigerGraph Operator>=0.0.9 and TigerGraph>=3.9.3) specify the compress level for backup
      compressLevel: DefaultCompression #choose from DefaultCompression/BestSpeed/BestCompression

    # Optional: Set the policy for cleaning up backup package when deleting the backup CR
    # Choose from Delete/Retain
    # For backup CR created by backup schedule, 
    # the default behavior is to delete the backup package. 
    # The backup schedule CR use this feature to keep maxBackupFiles.
    # If you want to keep all backup packages created by backup schedule,
    # you can set the cleanPolicy to Retain. 
    # But at the same time, the maxBackupFiles and maxReservedDays won't work properly.
    cleanPolicy: Delete

    # Optional: Set the retry policy for backup CR
    backoffRetryPolicy:
      # set maxRetryTimes for backup CR
      maxRetryTimes: 3
      # set the min duration between two retries, 
      # the format is like "5s","10m","1h","1h20m5s"
      minRetryDuration: 5s
      # set the max duration between two retries, 
      # the format is like "5s","10m","1h","1h20m5s"
      maxRetryDuration: 10s
      # If the value is true, the deletion of backup CR won't be blocked by failed backup-clean-job
      # that means, when backup-clean-job exceeds the maxRetryTimes
      # the backup CR will be deleted directly, the backup package still exists in cluster
      forceDeleteAfterMaxRetries: false
```

#### Use RoleARN instead of access key to access S3 Bucket in TigerGraphBackupSchedule

Assume that you have already created a cluster with access to S3 following the instructions in the previous section [Create a cluster with access to S3](#create-a-cluster-with-access-to-s3-supported-from-operator-version-120-and-tigergraph-410).

Then you can replace the `.spec.backupTemplate.destination` field with the following content:

```yaml
spec:
  backupTemplate:
    destination:
      storage: s3Bucket
      s3Bucket:
        bucketName: operator-backup
        roleARN: arn:aws:iam::123456789012:role/role-name
```

> [!NOTE]
> In TigerGraph Operator 1.2.0, you are allowed to set `roleARN` and `secretKeyName` at the same time.
> If you set both of them, TigerGraph will only use `roleARN` and ignore the `secretKeyName`.
> So we highly recommend you to use just one of them.

## TigerGraphRestore

### Restore from local backup

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: restore-from-local
spec:
  restoreConfig:
    # We can use tag to restore from backup in the same cluster
    tag: daily-2021-11-04T120000
    # Optional
    stagingPath: /home/tigergraph/tigergraph/data/restore-staging
    # Optional: (TigerGraph Operator>=0.0.9 and TigerGraph>=3.9.3) should be >=0
    decompressProcessNumber: 2
  source:
    storage: local
    local:
      path: /home/tigergraph/tigergraph/data/backup
  # Specify the name of cluster
  clusterName: test-cluster

  # Optional: Set the retry policy for restore CR
  backoffRetryPolicy:
    # set maxRetryTimes for restore CR
    maxRetryTimes: 3
    # set the min duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    minRetryDuration: 5s
    # set the max duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    maxRetryDuration: 10s
```

### Restore from backup in S3 bucket

Assume that you have already created an S3 Secret named `s3-secret` containing your AWS access credentials following the instructions in the previous section [Use AWS Access Key and Secret Access Key](#use-aws-access-key-and-secret-access-key).

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: restore-from-s3
spec:
  restoreConfig:
    tag: daily-2021-11-04T120000
    # Optional
    stagingPath: /home/tigergraph/tigergraph/data/restore-staging
    # Optional: (TigerGraph Operator>=0.0.9 and TigerGraph>=3.9.3) should be >=0
    decompressProcessNumber: 2
  source:
    storage: s3Bucket 
    s3Bucket:
      # specify the bucket you want to use
      bucketName: operator-backup
      secretKeyName: s3-secret
  # Specify the name of cluster
  clusterName: test-cluster

  # Optional: Set the retry policy for restore CR
  backoffRetryPolicy:
    # set maxRetryTimes for restore CR
    maxRetryTimes: 3
    # set the min duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    minRetryDuration: 5s
    # set the max duration between two retries, 
    # the format is like "5s","10m","1h","1h20m5s"
    maxRetryDuration: 10s
```

#### Use RoleARN instead of access key to access S3 Bucket in TigerGraphRestore

Assume that you have already created a cluster with access to S3 following the instructions in the previous section [Create a cluster with access to S3](#create-a-cluster-with-access-to-s3-supported-from-operator-version-120-and-tigergraph-410).

Then you can replace the `.spec.source` field with the following content:

```yaml
spec:
  source:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      roleARN: arn:aws:iam::123456789012:role/role-name
```

### Cross-cluster restore in existing cluster

We recommend using `kubectl tg restore` command to do this(See [Cross-Cluster Restore from Backup](./backup-restore-by-kubectl-tg.md#cross-cluster-restore-from-backup)). Since it is complicated to get metadata of backup and put it in CR.

You should use `kubectl tg backup list --meta` to get metadata, and put it in field`.spec.restoreConfig.meta`

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: tigergraphrestore-sample
spec:
  clusterName: test-cluster-new
  source:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      secretKeyName: s3-secret
  restoreConfig:
    meta: |
      {
        "Tag": "daily-2022-10-13T022218",
        "Files": [
          {
            "Instance": {
              "ServiceName": "GSE",
              "Replica": 0,
              "Partition": 1
            },
            "Name": "GSE_1_1.tar.lz4",
            "Checksum": "ecbddb2312346506",
            "RawSize": 946248,
            "Size": 5287,
            "TargetPaths": [
              ""
            ]
          },
          {
            "Instance": {
              "ServiceName": "GPE",
              "Replica": 0,
              "Partition": 1
            },
            "Name": "GPE_1_1.tar.lz4",
            "Checksum": "282f94df17d3ea35",
            "RawSize": 13286,
            "Size": 751,
            "TargetPaths": [
              ""
            ]
          },
          {
            "Instance": {
              "ServiceName": "GSQL",
              "Replica": 0,
              "Partition": 0
            },
            "Name": "GSQL.tar.lz4",
            "Checksum": "97dbfd62825bfd3f",
            "RawSize": 4522912,
            "Size": 1687264,
            "TargetPaths": null
          },
        "Time": "2022-10-13 02:22:19"
      }
    stagingPath: /home/tigergraph/data
```

#### Cluster version >=3.9.2

If you are using a TigerGraph cluster whose version >=3.9.2, the CR could be simplified. You don't need to put the metadata into it, you only need to specify the tag

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: restore-from-s3
spec:
  restoreConfig:
    tag: daily-2022-10-13T022218
    # Optional
    stagingPath: /home/tigergraph/tigergraph/data/restore-staging
    # Optional: (TigerGraph Operator>=0.0.9 and TigerGraph>=3.9.3) should be >=0
    decompressProcessNumber: 2
  source:
    storage: s3Bucket 
    s3Bucket:
      # specify the bucket you want to use
      bucketName: operator-backup
      secretKeyName: s3-secret
  # Specify the name of cluster
  clusterName: test-cluster-new
```

### Clone a cluster(Create a new cluster and do cross-cluster restore)

We recommend using `kubectl tg restore` command to do this(See [Clone Cluster from Backup](./backup-restore-by-kubectl-tg.md#clone-cluster-from-backup)). Since it is complicated to get metadata of backup , clusterTemplate of the original cluster and put them in CR.

You should use `kubectl tg backup list --cluster-name source-cluster -n tigergraph --meta` to get metadata, and put it in field`.spec.restoreConfig.meta`. Then use `kubectl tg export --cluster-name source-cluster -n tigergraph` to get cluster template of the original cluster, and put it in field `.spec.clusterTemplate`

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: tigergraphrestore-sample
spec:
  clusterName: test-cluster-new
  source:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      secretKeyName: s3-secret
  restoreConfig:
    meta: |
      {
        "Tag": "daily-2022-10-13T022218",
        "Files": [
          {
            "Instance": {
              "ServiceName": "GSE",
              "Replica": 0,
              "Partition": 1
            },
            "Name": "GSE_1_1.tar.lz4",
            "Checksum": "ecbddb2312346506",
            "RawSize": 946248,
            "Size": 5287,
            "TargetPaths": [
              ""
            ]
          },
          {
            "Instance": {
              "ServiceName": "GPE",
              "Replica": 0,
              "Partition": 1
            },
            "Name": "GPE_1_1.tar.lz4",
            "Checksum": "282f94df17d3ea35",
            "RawSize": 13286,
            "Size": 751,
            "TargetPaths": [
              ""
            ]
          },
          {
            "Instance": {
              "ServiceName": "GSQL",
              "Replica": 0,
              "Partition": 0
            },
            "Name": "GSQL.tar.lz4",
            "Checksum": "97dbfd62825bfd3f",
            "RawSize": 4522912,
            "Size": 1687264,
            "TargetPaths": null
          },
        "Time": "2022-10-13 02:22:19"
      }
    stagingPath: /home/tigergraph/data
  clusterTemplate:
    replicas: 3
    image: docker.io/tigergraph/tigergraph-k8s:3.9.3
    imagePullPolicy: IfNotPresent
    listener:
      type: LoadBalancer
    resources:
      requests:
        cpu: 2
        memory: 8Gi
    storage:
      type: persistent-claim
      volumeClaimTemplate:
        storageClassName: standard
        resources:
          requests:
            storage: 10G
    ha: 1
    license: "YOUR_LICENSE"
```

#### Clone Cluster version >=3.9.2

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: tigergraphrestore-sample
spec:
  clusterName: test-cluster-new
  source:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      secretKeyName: s3-secret
  restoreConfig:
    tag: daily-2022-10-13T022218
    stagingPath: /home/tigergraph/data
  clusterTemplate:
    replicas: 3
    image: docker.io/tigergraph/tigergraph-k8s:3.9.3
    imagePullPolicy: IfNotPresent
    listener:
      type: LoadBalancer
    resources:
      requests:
        cpu: 2
        memory: 8Gi
    storage:
      type: persistent-claim
      volumeClaimTemplate:
        storageClassName: standard
        resources:
          requests:
            storage: 10G
    ha: 1
    license: "YOUR_LICENSE"
```
