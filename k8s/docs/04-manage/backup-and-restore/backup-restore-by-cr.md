# Backup & Restore cluster by CR

- [Backup \& Restore cluster by CR](#backup--restore-cluster-by-cr)
  - [Creating an S3 Secret for Backup and Restore](#creating-an-s3-secret-for-backup-and-restore)
  - [TigerGraphBackup](#tigergraphbackup)
    - [Backup to local storage](#backup-to-local-storage)
    - [Backup to S3 bucket](#backup-to-s3-bucket)
  - [TigerGraphBackupSchedule](#tigergraphbackupschedule)
    - [Schedule backup to local storage](#schedule-backup-to-local-storage)
    - [Schedule backup to S3 bucket](#schedule-backup-to-s3-bucket)
  - [TigerGraphRestore](#tigergraphrestore)
    - [Restore from local backup](#restore-from-local-backup)
    - [Restore from backup in S3 bucket](#restore-from-backup-in-s3-bucket)
    - [Cross-cluster restore in existing cluster](#cross-cluster-restore-in-existing-cluster)
      - [Cluster version \>=3.9.2](#cluster-version-392)
    - [Clone a cluster(Create a new cluster and do cross-cluster restore)](#clone-a-clustercreate-a-new-cluster-and-do-cross-cluster-restore)
      - [Clone Cluster version \>=3.9.2](#clone-cluster-version-392)

## Creating an S3 Secret for Backup and Restore

When working with backup and restore operations involving S3 buckets, you need to create a Kubernetes Secret to securely store your AWS access credentials. Here's how you can create an S3 Secret:

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

## TigerGraphBackup

> [!NOTE]
> There are many examples on different conditions of backup and restore. Some fields in the YAML format CR is optional, a mark `# optional` is put above them. All fields without the optional mark is required.

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
    tag: local
    # Optional: Set the path for temporary staging files
    stagingPath: /home/tigergraph/tigergraph/data
    # Optional: If 'incremental' is set to true, incremental backup will be performed
    incremental: false
    # Optional: Set the timeout value for the backup process (default is 18000 seconds)
    timeout: 18000
    # Optional: Specify the number of processes to use for compression (0 uses the number of CPU cores)
    compressProcessNumber: 0
    # Optional: (Requires operator version >= 0.0.9 and TigerGraph version >= 3.9.3)
    # Choose the compression level for the backup: DefaultCompression/BestSpeed/BestCompression
    compressLevel: DefaultCompression # Choose from DefaultCompression/BestSpeed/BestCompression
```

### Backup to S3 bucket

Certainly, here's the YAML file for performing a backup to an S3 bucket using a previously created Secret named `s3-secret`. You can save this content to a file (e.g., `backup-s3.yaml`), and then run `kubectl apply -f backup-s3.yaml -n YOUR_NAMESPACE` to create the backup.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphBackup
metadata:
  name: test-cluster-backup-s3
spec:
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
    tag: s3
    # Optional: Set the path for temporary staging files
    stagingPath: /home/tigergraph/tigergraph/data
    # Optional: If 'incremental' is set to true, incremental backup will be performed
    incremental: false
    # Optional: Set the timeout value for the backup process (default is 18000 seconds)
    timeout: 18000
    # Optional: Specify the number of processes to use for compression (0 uses the number of CPU cores)
    compressProcessNumber: 0
    # Optional: (Requires operator version >= 0.0.9 and TigerGraph version >= 3.9.3)
    # Choose the compression level for the backup: DefaultCompression/BestSpeed/BestCompression
    compressLevel: DefaultCompression # Choose from DefaultCompression/BestSpeed/BestCompression
```

## TigerGraphBackupSchedule

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
  # optional : is pause is true, the cronjob will be suspended
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
      tag: daily
      # optional
      stagingPath: /home/tigergraph/tigergraph/data
      # optional :if incremental is true, incremental backup will be performed
      incremental: false
      # optional
      timeout: 18000
      # optional :specify the number of process to do compress
      compressProcessNumber: 0
      # optional: (operator>=0.0.9 and tg>=3.9.3) specify the compress level for backup
      compressLevel: DefaultCompression #choose from DefaultCompression/BestSpeed/BestCompression
```

### Schedule backup to S3 bucket

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
  # optional : is pause is true, the cronjob will be suspended
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
      tag: s3-daily
      # optional
      stagingPath: /home/tigergraph/tigergraph/data/backup-staging
      # optional :if incremental is true, incremental backup will be performed
      incremental: false
      # optional
      timeout: 18000
      # optional :specify the number of process to do compress
      compressProcessNumber: 0
      # optional: (operator>=0.0.9 and tg>=3.9.3) specify the compress level for backup
      compressLevel: DefaultCompression #choose from DefaultCompression/BestSpeed/BestCompression
```

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
    # optional
    stagingPath: /home/tigergraph/tigergraph/data/restore-staging
    # optional: (operator>=0.0.9 and tg>=3.9.3) should be >=0
    decompressProcessNumber: 2
  source:
    storage: local
    local:
      path: /home/tigergraph/tigergraph/data/backup
  # Specify the name of cluster
  clusterName: test-cluster
```

### Restore from backup in S3 bucket

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphRestore
metadata:
  name: restore-from-s3
spec:
  restoreConfig:
    tag: daily-2021-11-04T120000
    # optional
    stagingPath: /home/tigergraph/tigergraph/data/restore-staging
    # optional: (operator>=0.0.9 and tg>=3.9.3) should be >=0
    decompressProcessNumber: 2
  source:
    storage: s3Bucket 
    s3Bucket:
      # specify the bucket you want to use
      bucketName: operator-backup
      secretKeyName: s3-secret
  # Specify the name of cluster
  clusterName: test-cluster
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
    # optional
    stagingPath: /home/tigergraph/tigergraph/data/restore-staging
    # optional: (operator>=0.0.9 and tg>=3.9.3) should be >=0
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
