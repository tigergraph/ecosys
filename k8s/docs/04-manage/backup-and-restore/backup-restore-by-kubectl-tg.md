<h1>Backup & Restore clustey kubectl-tg plugin</h1>

If you have experience with Custom Resources in Kubernetes (K8S), you can leverage CRs to initiate backup or restore processes. We provide a dedicated document detailing the steps for performing backup and restore using Custom Resources (CRs). [Backup & restore by CR](backup-restore-by-cr.md)

- [Prerequisite](#prerequisite)
- [Utilizing `kubectl tg` Command for Backup](#utilizing-kubectl-tg-command-for-backup)
  - [Creating and Updating Backups](#creating-and-updating-backups)
    - [Backup to Local Storage](#backup-to-local-storage)
    - [Backup to an S3 Bucket](#backup-to-an-s3-bucket)
    - [\[Preview\] Performing Incremental Backup](#preview-performing-incremental-backup)
    - [Updating Backup Custom Resources](#updating-backup-custom-resources)
      - [Changing Backup Types](#changing-backup-types)
    - [Creating Another Backup](#creating-another-backup)
  - [Listing Backup Custom Resources](#listing-backup-custom-resources)
  - [Displaying Backup Process Status](#displaying-backup-process-status)
  - [Delete Backup Custom Resource (CR)](#delete-backup-custom-resource-cr)
  - [Listing Backups](#listing-backups)
  - [Removing Backups](#removing-backups)
- [Creating and Managing Backup Schedules](#creating-and-managing-backup-schedules)
    - [Specifying Backup Schedule](#specifying-backup-schedule)
    - [Creating Backup Schedules](#creating-backup-schedules)
      - [Creating a Local Backup Schedule](#creating-a-local-backup-schedule)
      - [Creating an S3 Backup Schedule](#creating-an-s3-backup-schedule)
    - [Updating a Backup Schedule](#updating-a-backup-schedule)
    - [Listing All Backup Schedules](#listing-all-backup-schedules)
    - [Deleting a Backup Schedule](#deleting-a-backup-schedule)
    - [Showing Backup Schedule Status](#showing-backup-schedule-status)
    - [Pausing and Resuming a Backup Schedule](#pausing-and-resuming-a-backup-schedule)
    - [Backup Strategy Overview](#backup-strategy-overview)
- [Utilizing `kubectl tg` for Restore](#utilizing-kubectl-tg-for-restore)
    - [Restore within the Same Cluster](#restore-within-the-same-cluster)
    - [Cross-Cluster Restore from Backup](#cross-cluster-restore-from-backup)
    - [Clone Cluster from Backup](#clone-cluster-from-backup)
    - [Cross-Cluster Restore and Cluster Clone (Cluster Version \< 3.9.2)](#cross-cluster-restore-and-cluster-clone-cluster-version--392)
      - [Restore an Existing Cluster from Backup Created by Another Cluster (Cluster version \< 3.9.2)](#restore-an-existing-cluster-from-backup-created-by-another-cluster-cluster-version--392)
      - [Clone a Cluster (Cluster version \< 3.9.2)](#clone-a-cluster-cluster-version--392)
    - [Show Status of Restore](#show-status-of-restore)
    - [Delete Restore Job](#delete-restore-job)


Prerequisite
============

The successful execution of the `kubectl tg backup|restore|backup-schedule` command relies on the presence of several dependencies: `kubectl`, `helm`, `jq`, and `yq`. It is imperative to ensure that all these components are properly installed on your system.

Furthermore, prior to using the backup command, it is essential to have the TigerGraph Kubectl Plugin installed(please refer to [Install kubectl-tg plugin](../../02-get-started/get_started.md#install-kubectl-tg-plugin)). Additionally, you must create your cluster as a prerequisite step.

Utilizing `kubectl tg` Command for Backup
==========================================

To maintain coherence between the `kubectl-tg` command and custom resources presented in YAML format, the `--name` option is employed to specify the name of the custom resources to be created or managed.

Creating and Updating Backups
------------------------------

```
Usage:
  kubectl tg backup [create|update] [OPTIONS]

Options:
  -h|--help :                   Display this message.
  -n|--namespace :              Define the namespace for TG cluster deployment. If not set, the 
                                default namespace from the context will be used.
  --name :                      (required) Specify the name of the backup.
  -c|--cluster-name :           Define the cluster name for TG cluster deployment. No default value.
  --tag :                       Specify the tag for backup files. For example, if you specify 
                                --tag daily, the backup file will be named daily-20xx-xx-xxTxxxxxx.
  --staging-path :              Specify the location to store temporary files.
  --timeout :                   Set the backup timeout in seconds. Default: 18000.
  --compress-process-number :   Determine the number of concurrent processes used for compression 
                                during backup. A value of 0 indicates that the number of compression processes will match the number of CPU cores on the nodes. The default value is 0.
  --compress-level :            Choose from options: BestSpeed, DefaultCompression, and 
                                BestCompression. Only supported for TG clusters >=3.9.3.
  --incremental :               Perform incremental backup.
  --full :                      Perform a full backup (full backup is the default behavior).
  --destination :               Specify the destination for storing backup files. Currently 
                                supports local and S3 storage.
  -y :                          Provide a positive response to all questions.

  Configuration details for different destinations:
  If the destination is local, you should provide:
    --local-path :              Specify the local path where backup files will be stored.
  If the destination is S3:
    --s3-bucket :               Specify the name of the S3 Bucket.
    --aws-secret :              Provide the name of the AWS secret. 
                                The secret should contain accessKeyID and secretAccessKey.
```

### Backup to Local Storage

use the following command to backup cluster whose name is test-cluster and store backup files in local storage

```
 kubectl tg backup create --name backup-to-local \
   --cluster-name test-cluster --tag testlocal -n tigergraph \
  --destination local --local-path /home/tigergraph/tigergraph/data/mybackup 
```

you can also customize timeout, staging path, the compress level and the compress process number

```
 kubectl tg backup create --name backup-to-local --cluster-name test-cluster \
  --tag testlocal -n tigergraph --destination local \
  --local-path /home/tigergraph/tigergraph/data/mybackup  --staging-path /home/tigergraph/temp \
  --timeout 18000 --compress-process-number 0 --compress-level BestSpeed
```
> [!NOTE]
> 1.  Please use subpath of `/home/tigergraph/tigergraph/data/` as local path for backup since this path is mounted with PV. For example, you can use `/home/tigergraph/tigergraph/data/mybackup` .If you do not use that, you will lose your backup data if the pod restarts. And be careful that don’t use the same path for local path as the staging path. If you don’t configure staging path, the default staging path is `/home/tigergraph/tigergraph/data/backup`, if you set local path as `/home/tigergraph/tigergraph/data/backup`, the backup will fail.
> 2.  Please remember which path you use and use the same path if you want to restore the backup file you create.
    

### Backup to an S3 Bucket

Follow the steps below to back up a cluster named "test-cluster" and store the backup files in an S3 bucket. Make sure you provide the S3 bucket name, access key ID, and secret key for S3.

1. First, create a Kubernetes secret containing the access key ID and secret key:
   
   ```bash
   kubectl create secret generic aws-secret \
       --from-literal=accessKeyID=AWSACCESSKEY \
       --from-literal=secretAccessKey='AWSSECRETKEY' 
   ```

2. Next, create a backup to the S3 bucket:

   ```bash
   kubectl tg backup create --name backup-to-s3 -n tigergraph \
     --cluster-name test-cluster --destination s3Bucket --tag testS3 \
     --s3-bucket tgbackup  \
     --aws-secret aws-secret
   ```

You can also customize the following parameters: timeout, staging path, and the number of compression processes:

```bash
kubectl tg backup create --name backup-to-s3  -n tigergraph \
  --cluster-name test-cluster --tag testS3 --destination s3Bucket \
  --s3-bucket tgbackup \
  --aws-secret aws-secret \
  --staging-path /home/tigergraph/temp \
  --timeout 18000 --compress-process-number 0 --compress-level BestSpeed
```

> [!NOTE]
> Ensure that you have created the necessary Kubernetes secret containing the access key ID and secret key before initiating the backup process to the S3 bucket.

### [Preview] Performing Incremental Backup
> [!NOTE]
> For TigerGraph version 3.9, performing an incremental backup requires the existence of at least one previous backup for the cluster. Without a prior full backup, attempting an incremental backup will result in failure. To verify the presence of a full backup, you can utilize the command `kubectl tg backup list`.

To initiate an incremental backup, incorporate the `--incremental` option into the following command:

```bash
kubectl tg backup create --cluster-name test-cluster -n tigergraph --name incremental-backup \
  --incremental --tag testlocal \
  --destination local \
  --local-path /home/tigergraph/tigergraph/data/mybackup
```

### Updating Backup Custom Resources

If you have previously created a backup using the `kubectl tg backup create` command, you can modify the backup configuration by employing the `kubectl tg backup update` command. Once the `update` command is executed, the backup process will be triggered immediately with the updated settings.

Suppose you've already generated a backup using the following command:

```bash
kubectl tg backup create --name backup-to-local \
  --cluster-name test-cluster --tag testlocal -n tigergraph \
  --destination local \
  --local-path /home/tigergraph/backup  --staging-path /home/tigergraph/temp \
  --timeout 18000 --compress-process-number 0
```

To adjust the backup timeout, you can execute:

```bash
kubectl tg backup update --name backup-to-local -n tigergraph \
--timeout 20000
```

Subsequently, the timeout value will be updated to 20000, and a backup process with the revised timeout setting will be immediately initiated.

#### Changing Backup Types

You have the flexibility to switch between full and incremental backups using the following commands:

- To convert a full backup configuration to an incremental backup, use:

  ```bash
  kubectl tg backup update --name backup-to-local --incremental
  ```

- To transform an incremental backup configuration to a full backup, use:

  ```bash
  kubectl tg backup update --name incremental-backup --full
  ```

These commands allow you to seamlessly modify the backup type based on your evolving requirements.



### Creating Another Backup

If you have previously initiated a backup using the `kubectl tg backup create` command:

```bash
kubectl tg backup create --name backup-to-local \
  --cluster-name test-cluster --tag testlocal -n tigergraph \
  --destination local \
  --local-path /home/tigergraph/backup 
```

And you wish to create a new backup with the same configuration, you can execute:

```bash
kubectl tg backup update --name backup-to-local -n tigergraph
```

The system will prompt you to confirm whether you want to initiate the backup again. You should type "y" to proceed.

Alternatively, you can employ the `-y` option, indicating "yes to all questions," to immediately start the backup:

```bash
kubectl tg backup update --name backup-to-local -n tigergraph -y
```

Listing Backup Custom Resources
----
To retrieve a list of all backup Custom Resources (CRs) within a specific namespace, utilize the following command:

```bash
kubectl get tgbackup --namespace tigergraph
```

This command will provide you with an overview of the backup CRs present in the designated namespace.


Displaying Backup Process Status
----
Upon executing `kubectl tg backup create/update`, a backup job will be generated within the Kubernetes (k8s) environment. To facilitate monitoring, we offer the `kubectl tg backup status` command, allowing you to assess the status of the backup process. Should you encounter errors or warnings, refer to the [How to Debug Backup & Restore](#how-to-debug-backup--restore) section for troubleshooting guidance.

To display the status of all backup processes within the `tigergraph` namespace, use the following command:

```bash
kubectl tg backup status --namespace tigergraph
```

The output will resemble the following:

```
NAME                        CLUSTER        TAG     STORAGE   INCREMENTAL   STARTTIME   COMPLETIONTIME
test-cluster-backup-daily   test-cluster   daily   local                   3d12h       
test-cluster-backup-local   test-cluster   local   local                   16s         5s
```

If the `COMPLETIONTIME` field is not empty, it indicates a successful backup process.

For detailed information about a specific backup process, execute:

```bash
kubectl tg backup status --name test-cluster-backup-daily \
  --namespace tigergraph
```

The output provides a comprehensive overview of the backup process, including configurations and status details. You'll find events that indicate the progress and outcome of the backup job.

The output is like this:

```
kubectl tg backup status --cluster-name test-cluster --tag daily
Name:         test-cluster-backup-daily
Namespace:    default
Labels:       <none>
Annotations:  <none>
API Version:  graphdb.tigergraph.com/v1alpha1
Kind:         TigerGraphBackup
Metadata:
  Creation Timestamp:  2022-12-13T09:52:38Z
  Generation:          1
  ...
  Resource Version:  905382
  UID:               6c97ae4a-e7fb-49e1-8c45-e8e09286865b
Spec:
  Backup Config:
    Compress Process Number:  0
    Tag:                      daily
    Timeout:                  18000
  Cluster Name:               test-cluster
  Destination:
    Local:
      Path:   /home/tigergraph/backup
    Storage:  local
Status:
  Conditions:
    Last Transition Time:  2022-12-16T13:44:24Z
    Message:               Failed to backup cluster
    Reason:                BackupFailed
    Status:                True
    Type:                  Failed
  Start Time:              2022-12-16T13:44:03Z
  Target Ready:            true
Events:
  Type     Reason                Age                   From              Message
  ----     ------                ----                  ----              -------
  Normal   Target cluster ready  31m (x35 over 3d12h)  TigerGraphBackup  Target cluster is ready for backup
  Warning  Backup job failed     31m (x12 over 3d12h)  TigerGraphBackup  Failed to backup cluster test-cluster
```

You can identify the occurrence of events marked as "Backup job failed," which indicates that the respective backup task has encountered a failure.

Delete Backup Custom Resource (CR)
-------------------------------------

To remove a backup Custom Resource (CR), execute the following command:

```
kubectl tg backup delete --name backup-to-local --namespace tigergraph
```

Listing Backups
---------------

To list available backups, utilize the command:

```
Usage:
  kubectl tg backup list [OPTIONS]

Options:
  --cluster-name :  (required) Set the name of the target cluster.
  -n, --namespace : Set the namespace of the target cluster.
  --tag :           Specify the tag of the backup.
  --json :          Output in JSON format.
  --meta :          Retrieve the metadata of the backup.
```


To examine the existing backups for a particular cluster, you can employ the following commands to list all backups associated with the "test-cluster":

```
kubectl tg backup list --cluster-name test-cluster -n tigergraph
```

If you prefer to obtain the backup list in JSON format, use:

```
kubectl tg backup list --cluster-name test-cluster -n tigergraph --json
```


In the context of a cross-cluster restore, acquiring backup metadata is essential. To accomplish this, utilize the tag obtained from the `kubectl tg backup list` command. Run the following command:

```
kubectl tg backup list --cluster-name test-cluster -n tigergraph \
  --tag tests3-2022-10-31T031005 --meta
```

This command will display the metadata in the standard output. If you wish to store this metadata in a file, execute:

```
kubectl tg backup list --cluster-name test-cluster -n tigergraph --tag tests3 --meta > metadata
```



Removing Backups
------------------

To eliminate backups that are no longer needed, follow these steps:

Use the following command to remove specific backups associated with the "test-cluster" and located in the "tigergraph" namespace:

```bash
kubectl tg backup remove --cluster-name test-cluster --namespace tigergraph \
  --tag daily-20xx-xx-xxTxxxxx
```
   
This command enables you to selectively remove backups based on their tags. Please ensure you accurately specify the relevant cluster name, namespace, and backup tag when executing this command.


Creating and Managing Backup Schedules
====
The `kubectl tg backup-schedule` command enables you to create, update, monitor, list, delete, pause, and resume backup schedules for specific clusters. This comprehensive set of options empowers you to effortlessly manage your backup scheduling requirements. 

```
Usage:
  kubectl tg backup-schedule [create|update|status|list|delete|pause|resume] [OPTIONS]

Commands:
  create                        Create a backup schedule to schedule backup for specific cluster
  update                        Update a backup schedule
  status                        Show status of backup schedule
  list                          List existing backup schedules
  delete                        Delete a backup schedule (backups created by the schedule won't be deleted)
  pause                         Pause the backup schedule
  resume                        Resume the backup schedule

Options:
  -h|--help:                    show this message
  -n|--namespace :              set namespace to deploy TG cluster, if not set, use the default namespace in context
  --name :                      (required)specify name of backup schedule
  -c|--cluster-name :           set cluster-name to deploy TG cluster, no default
  --tag :                       specify the tag of backup files. e.g. if you specify --tag daily, the backup file will be daily-20xx-xx-xxTxxxxxx
  --staging-path :              specify where to store the temporary files
  --timeout :                   the backup timeout in seconds,default: 18000
  --compress-process-number :   the number of concurrent process for compression during backup 
                                value 0 means the number of processes used to compress equals 
                                the number of the node's CPU cores. And the default value is 0
  --compress-level :            choose from BestSpeed,DefaultCompression and BestCompression. Only support TG cluster >=3.9.3
  --schedule :                  specify the schedule of backup in cron format. e.g. '* * * * *' is backup every minute
  --destination :               set the destination to store backup files, support local and s3 now
  --incremental :               do incremental backup
  --full :                      do full backup (full backup is performed by default)
  --max-retry :                 set max times of retry for each backup
  --max-backup-file :           set the max number of files you want to retain
  --max-reserved-day :          set the max number of days you want to retain these backups
  -y :                          yes to all questions

  Followings are about the configuration of different destination:
  If destination is local,you should provide:
    --local-path :              set the local path where to store backup files
  If destination is s3:
    --s3-bucket :               S3 Bucket name
    --aws-secret :              name of secret for aws, the secret should contain  accessKeyID and secretAccessKey
```


### Specifying Backup Schedule

To define a backup schedule, utilize a cron expression to set the timing. You can conveniently generate cron expressions using tools like [https://crontab.guru/](https://crontab.guru/), which provides an intuitive interface for creating intricate schedules. 

For instance, if you desire to execute a backup once daily at 00:00, you would specify the following cron expression:

```bash
--schedule '0 0 * * *'
```

Please ensure to enclose the cron expression in single quotation marks (`'`) to prevent unintended filename expansion.


### Creating Backup Schedules

#### Creating a Local Backup Schedule

   To create a schedule that performs daily backups for the "test-cluster" at 00:00, storing backup files locally, execute the following command:

   ```bash
   kubectl tg backup-schedule create --name backupsch-local \
     --cluster-name test-cluster -n tigergraph \
     --tag localdaily --schedule '0 0 * * *' \
     --destination local --local-path /home/tigergraph/backup
   ```
#### Creating an S3 Backup Schedule

   For a schedule that conducts hourly backups for the "test-cluster" at minute 0, storing backup files in an S3 bucket, proceed as follows:

   First, create a secret in Kubernetes containing access key id and secret key:

   ```bash
   kubectl create secret generic aws-secret \
       --from-literal=accessKeyID=AWSACCESSKEY \
       --from-literal=secretAccessKey='AWSSECRETKEY' 
   ```

   Next, establish the backup schedule:

   ```bash
   kubectl tg backup-schedule create --name backupsch-s3 \ 
     --cluster-name test-cluster -n tigergraph \
     --tag s3daily --schedule '0 * * * *' --destination s3Bucket\
     --s3-bucket tgbackup \
     --aws-secret aws-secret
   ```

By executing these commands, you'll set up automatic backup schedules tailored to your requirements.


<!-- Now we hide this part for user because incremental backup is a Preview feature
### Creating an Incremental Backup Schedule

To establish an incremental backup schedule, please consult the details provided in [Performing incremental backup](#performing-incremental-backup). It is essential to ensure the existence of a pre-existing full backup for the cluster before initiating incremental backups.

Use the `--incremental` option to schedule an incremental backup:

```bash
kubectl tg backup-schedule create --cluster-name test-cluster -n tigergraph \
 --tag localdaily --schedule '0 0 * * *' --incremental \
 --destination local --local-path /home/tigergraph/backup
```
-->

### Updating a Backup Schedule
When updating a backup schedule, ensure you provide the correct name.

For instance, to adjust the schedule for daily backups at 12:00, execute the following:

```bash
kubectl tg backup-schedule update --name backupsch-local \
 --tag localdaily --schedule '0 12 * * *'
```

Please note that ongoing backup jobs remain unaffected by configuration changes. The new configuration will take effect during the subsequent schedule.


### Listing All Backup Schedules

To view a comprehensive list of all existing backup schedules within a specific namespace, employ the following command:

```bash
kubectl tg backup-schedule list --namespace tigergraph
```

### Deleting a Backup Schedule

To remove a backup schedule, execute the following command:

```bash
kubectl tg backup-schedule delete --name backupsch-local \
  --namespace tigergraph
```

### Showing Backup Schedule Status

To retrieve the status of a backup schedule, use the following command:

```bash
kubectl tg backup-schedule status --name test-cluster-schedule-daily \
  --namespace tigergraph
```

The output will provide insights into the status of the specified backup schedule, allowing you to monitor its progress and execution.


```
Name:         test-cluster-schedule-daily
Namespace:    default
Labels:       <none>
Annotations:  <none>
API Version:  graphdb.tigergraph.com/v1alpha1
Kind:         TigerGraphBackupSchedule
Metadata:
  Creation Timestamp:  2022-12-20T02:40:10Z
  Generation:          1
  Resource Version:  1696649
  UID:               f8c95418-bcb3-495b-b5e4-5083789ce11a
Spec:
  Backup Template:
    Backup Config:
      Compress Process Number:  0
      Tag:                      daily
      Timeout:                  18000
    Cluster Name:               test-cluster
    Destination:
      Local:
        Path:   /home/tigergraph/backup
      Storage:  local
  Schedule:     * * * * *
Status:
  Conditions:
    Last Transition Time:  2022-12-20T02:42:01Z
    Message:               Backup job is active
    Reason:                BackupActive
    Status:                True
    Type:                  Active
  Job Counter:
    Successful Jobs:     1
  Last Schedule Time:    2022-12-20T02:42:00Z
  Last Successful Time:  2022-12-20T02:41:11Z
Events:
  Type    Reason                   Age                From                      Message
  ----    ------                   ----               ----                      -------
  Normal  Backup schedule created  2m1s               TigerGraphBackupSchedule  Create a new backup schedule success.
  Normal  Backup job succeed       60s                TigerGraphBackupSchedule  Last scheduled job succeed
  Normal  Backup job created       10s (x2 over 71s)  TigerGraphBackupSchedule  Schedule a new backup job
```


Indeed, the events associated with backup schedule executions provide valuable insights into the success or failure of the scheduled jobs. By examining these events, you can ascertain whether the backup schedules were executed as intended and if any issues arose during the process.


### Pausing and Resuming a Backup Schedule

You have the ability to temporarily halt a running backup schedule or resume a paused one using the following commands:

To pause a currently active backup schedule:

```bash
kubectl tg backup-schedule pause --name backupsch-local -n tigergraph
```

This action will prevent the scheduling of the next backup job.

To resume a paused backup schedule:

```bash
kubectl tg backup-schedule resume --name backupsch-local -n tigergraph
```


### Backup Strategy Overview

It's important to note that the backup strategy feature is available for cluster versions equal to or greater than 3.9.0. This feature provides enhanced control over backup operations and file retention. Presently, you have three distinct options at your disposal to facilitate a comprehensive backup strategy:

1. **`--max-retry`**: This parameter allows you to specify the maximum number of retry attempts for each backup job. It helps ensure that backup processes have a predefined limit of retries in the event of any unexpected issues.

2. **`--max-backup-file`**: As time progresses, the accumulation of backup files can consume substantial disk space. You can utilize this parameter to determine the maximum number of backup files to retain. For instance, setting `--max-backup-file 10` will retain the latest 10 backup files according to the specified tag.

3. **`--max-reserved-day`**: This parameter governs the maximum number of days that backups are retained. If a backup is created more than the defined number of days ago, it will be automatically deleted, thus optimizing storage management.

For example, consider a backup schedule with the tag `daily`. If you set `--max-backup-file 10`, a cleanup process will run after each scheduled backup, ensuring that only the latest 10 backups with the `daily` tag are retained. Backups with different tags will remain unaffected.

Furthermore, with `--max-reserved-day 7`, backups created more than 7 days ago (and possessing the `daily` tag) will be deleted, aligning with your defined retention strategy.

By leveraging these options, you can meticulously manage your backup jobs and safeguard against excessive disk usage. This proactive approach to backup strategy aids in optimizing storage utilization while preserving the necessary backups for operational needs.

Utilizing `kubectl tg` for Restore 
====
When you possess backups generated through the backup process or backup schedule, you have the capability to restore your cluster to a previous state. You can initiate restore from a backup that was crafted by the same cluster, and this feature extends to both local storage and S3 buckets.

It's important to highlight that we also offer cross-cluster restore, enabling you to restore Cluster B utilizing backups from Cluster A. As of now, this functionality exclusively supports S3 buckets.

A crucial consideration is that the restore process is currently restricted to clusters featuring the same partition configuration as the cluster that originated the backup.

|  Scenarios   | Is Partition changed?  | Is HA changed? | Support or not | Example(x\*y means x partitions and y ha) |
|  ----  | ----  | ---- | ---- | ---- |
| Clone an identical cluster | N | N | Y | Source cluster: 3\*2, Target cluster: 3\*2 |
| Restore in a cluster with different partition | Y | N or Y | N | Source cluster: 3*x, Target cluster: 2\*3 or 2\*2 |
| Restore in a cluster with different HA | N | Y | Y | Source cluster: 3\*3, Target cluster: 3\*1 |



```
USAGE:
  kubectl tg restore [OPTIONS]

Options:
  -h|--help: show this message
  -n|--namespace :              set namespace to deploy TG cluster, default namespace is current namespace
  -c|--cluster-name :           set cluster-name to deploy TG cluster, no default
  --name:                       specify name of restore
  --tag :                       specify the tag of backup files. you can use kubectl tg backup list to get all existing backups
  --metadata :                  specify the metadata file of backup. you should this if you want a cross-cluster restore
  --cluster-template :          configure the cluster you want to create from exported CR
  --staging-path :              specify where to store the temporary files
  --source :                    set the source to get backup files, support local and s3 now
  Followings are about the configuration of different destination:
  If destination is local,you should provide:
    --local-path :              set the local path where to store backup files
  If destination is s3:
    --s3-bucket :               S3 Bucket name
    --aws-secret :              name of secret for aws, the secret should contain  accessKeyID and secretAccessKey
```
### Restore within the Same Cluster

Suppose you have previously created a backup for `test-cluster` using the `kubectl tg backup create` command. To initiate restore within the same cluster, retrieve the tag of all Backups first:
   
   Execute the following command to retrieve the tags associated with all available backups:

   ```bash
   kubectl tg backup list --cluster-name test-cluster -n tigergraph
   ```

   The output will provide a list of backups along with their respective tags, types, versions, sizes, and creation timestamps. Choose the backup you intend to restore from based on your requirements.

   For instance:

   ```
   +------------------------------+------+---------+--------+---------------------+
   |             TAG              | TYPE | VERSION |  SIZE  |     CREATED AT      |
   +------------------------------+------+---------+--------+---------------------+
   | daily-2022-11-02T103601      | FULL | 3.9.0   | 1.7 MB | 2022-11-02 10:36:02 |
   | daily-2022-11-02T104925      | FULL | 3.9.0   | 1.7 MB | 2022-11-02 10:49:25 |
   | daily-2022-11-09T081545      | FULL | 3.9.0   | 1.7 MB | 2022-11-09 08:15:46 |
   | daily-2022-11-09T081546      | FULL | 3.9.0   | 1.7 MB | 2022-11-09 08:15:53 |
   +------------------------------+------+---------+--------+---------------------+
   ```


Using backup in local storage:
To restore your cluster utilizing a backup stored in local storage, execute the following command:
```
kubectl tg restore --name restore-from-local \
  --cluster-name test-cluster -n tigergraph --tag daily-2022-11-02T103601\
  --source local --local-path /home/tigergraph/backup
```
Replace  `/home/tigergraph/backup` with the appropriate path to the backup stored in your local storage. This command will initiate the restore process and bring your cluster back to the state captured by the specified backup.

Use backup in s3 bucket:

First, create a secret in k8s containing access key id and secret key:

```
kubectl create secret generic aws-secret \
    --from-literal=accessKeyID=AWSACCESSKEY \
    --from-literal=secretAccessKey='AWSSECRETKEY' 
```

Select a backup tag from the available backups and execute the following command to initiate restore from an S3 bucket:
```
kubectl tg restore --name restore-from-s3 \
  --namespace tigergraph --cluster-name test-cluster \
  --tag tests3-2022-10-31T031005 \
  --source s3Bucket --s3-bucket tg-backup \
  --aws-secret aws-secret
```

Make sure to replace tests3-2022-10-31T031005 with the desired backup tag and adjust tg-backup to your S3 bucket name. This command will trigger the restore process, bringing your cluster back to the chosen backup's state.


### Cross-Cluster Restore from Backup
> [!NOTE]
> This section pertains to users utilizing TigerGraph cluster version 3.9.2 or higher. If you are operating on an earlier version, please consult the [Restore an Existing Cluster from Backup Created by Another Cluster (Cluster version < 3.9.2)](#restore-an-existing-cluster-from-backup-created-by-another-cluster-cluster-version--392) section for relevant instructions.

Performing a cross-cluster restore, where you restore an existing cluster (target-cluster) using a backup created by another cluster (source-cluster), requires careful steps. Follow the instructions below for a successful cross-cluster restore:

1. **Retrieve the Backup Tag from the Source Cluster:**

   Obtain the backup tag from the source cluster (source-cluster) using the following command:

   ```bash
   kubectl tg backup list --cluster-name source-cluster --namespace tigergraph
   ```

2. **Use the Tag to Restore the Target Cluster:**

   Create an AWS secret for authentication if you haven't done so already:

   ```bash
   kubectl create secret generic aws-secret \
       --from-literal=accessKeyID=AWSACCESSKEY \
       --from-literal=secretAccessKey='AWSSECRETKEY'
   ```

   Then, initiate the cross-cluster restore for the target cluster (target-cluster) using the obtained backup tag:

   ```bash
   kubectl tg restore --name cross-restore \
     --namespace tigergraph --cluster-name target-cluster \
     --tag tests3-2022-10-31T031005 \
     --source s3Bucket --s3-bucket tg-backup \
     --aws-secret aws-secret
   ```

Remember to adjust the cluster names, backup tag, S3 bucket name, and AWS credentials as needed for your specific setup. Cross-cluster restore is a powerful way to recover data and configurations across different clusters, ensuring data resilience and system stability.



### Clone Cluster from Backup
> [!NOTE]
>  This section pertains to users utilizing TigerGraph cluster version 3.9.2 or higher. If you are operating on an earlier version, please consult the [Clone a Cluster (Cluster version \< 3.9.2)](#clone-a-cluster-cluster-version--392)


Creating a new cluster and restoring it from a backup created by another cluster, often referred as "cloning", involves several steps. Follow these instructions to successfully clone a cluster using the `kubectl tg restore` command:

1. **Retrieve the Cluster Configuration of the Source Cluster:**

   Export the custom resource (CR) configuration of the source cluster (source-cluster) and save it to a YAML file, for example:

   ```bash
   kubectl tg export --cluster-name source-cluster -n tigergraph
   ```
   Assume the output file is /home/test-cluster_backup_1668069319.yaml.

2. **Retrieve the Backup Tag:**

   Obtain the backup tag associated with the desired backup from the source cluster:

   ```bash
   kubectl tg backup list --cluster-name source-cluster --namespace tigergraph
   ```

3. **Use the Configuration and Backup Tag to Create a Cluster Clone:**

   Create an AWS secret for authentication if you haven't done so already:

   ```bash
   kubectl create secret generic aws-secret \
       --from-literal=accessKeyID=AWSACCESSKEY \
       --from-literal=secretAccessKey='AWSSECRETKEY'
   ```

   Initiate the cluster cloning process using the cluster configuration template and the backup tag:

   ```bash
   kubectl tg restore --name cross-restore \
     --namespace tigergraph --cluster-name new-cluster \
     --tag tests3-2022-10-31T031005 --cluster-template /home/test-cluster_backup_1668069319.yaml \
     --source s3Bucket --s3-bucket tg-backup  \
     --aws-secret aws-secret
   ```

By following these steps, you can easily perform cross-cluster restore or clone a cluster using backup files created by another cluster. Be sure to replace placeholders such as `source-cluster`, `target-cluster`, `AWSACCESSKEY`, `AWSSECRETKEY`, and file paths with actual values specific to your environment.

Once the process is complete, the new cluster (`new-cluster`) will be initialized and ready for use. The restore ensures that the new cluster matches the state of the source cluster captured by the backup. Cloning a cluster from a backup is a powerful way to quickly replicate environments and configurations for testing, development, or disaster recovery purposes.



### Cross-Cluster Restore and Cluster Clone (Cluster Version < 3.9.2)

Starting from TigerGraph cluster version 3.9.2, the process for cross-cluster restore and cluster cloning has been simplified. You only need the backup tag to specify the backup file that you want to restore. If you are using cluster < 3.9.2, you need to follow the instructions below:

#### Restore an Existing Cluster from Backup Created by Another Cluster (Cluster version < 3.9.2)
1. **Retrieve Backup Metadata for Source Cluster:**

   Obtain the metadata of the backup from the source cluster (source-cluster) and save it to a file named `backup-metadata`. Run the following command:

   ```bash
   kubectl tg backup list --cluster-name source-cluster --namespace tigergraph \
     --tag tests3-2022-10-31T031005 --meta > backup-metadata
   ```

2. **Create AWS Secret for Authentication:**

   If you haven't done so already, create a Kubernetes secret containing your AWS credentials for authentication:

   ```bash
   kubectl create secret generic aws-secret \
     --from-literal=accessKeyID=AWSACCESSKEY \
     --from-literal=secretAccessKey='AWSSECRETKEY'
   ```

   Replace `AWSACCESSKEY` and `AWSSECRETKEY` with your actual AWS access key ID and secret access key.

3. **Initiate Cross-Cluster Restore:**

   Execute the following command to initiate the cross-cluster restore process for the target cluster (target-cluster) using the backup metadata obtained from the source cluster:

   ```bash
   kubectl tg restore --name cross-restore \
     --namespace tigergraph --cluster-name target-cluster \
     --metadata backup-metadata \
     --source s3Bucket --s3-bucket tg-backup \
     --aws-secret aws-secret
   ```

   This command will initiate the cross-cluster restore, ensuring that the target-cluster is brought back to the state captured by the backup from the source-cluster.

Remember to adjust the cluster names, backup tag, S3 bucket name, and AWS credentials as needed for your specific setup. Cross-cluster restores are a powerful way to recover data and configurations across different clusters, ensuring data resilience and system stability.



#### Clone a Cluster (Cluster version < 3.9.2)

Creating a new cluster and restoring it from a backup created by another cluster, often referred to as "cloning," involves several steps. Follow these instructions to successfully clone a cluster using the `kubectl tg restore` command:

1. **Export Configuration of Source Cluster:**

   Obtain the custom resource (CR) configuration of the source cluster (source-cluster) and save it to a YAML file. Run the following command:

   ```bash
   kubectl tg export --cluster-name source-cluster -n tigergraph
   ```
  Assume the output file is /home/test-cluster_backup_1668069319.yaml.
  This file will serve as the template for creating the new cluster.

2. **Retrieve Backup Metadata for Source Cluster:**

   Obtain the metadata of the backup from the source cluster (source-cluster) and save it to a file named `backup-metadata`. Run the following command:

   ```bash
   kubectl tg backup list --cluster-name source-cluster --namespace tigergraph \
     --tag tests3-2022-10-31T031005 --meta > backup-metadata
   ```

3. **Create AWS Secret for Authentication:**

   If you haven't done so already, create a Kubernetes secret containing your AWS credentials for authentication:

   ```bash
   kubectl create secret generic aws-secret \
     --from-literal=accessKeyID=AWSACCESSKEY \
     --from-literal=secretAccessKey='AWSSECRETKEY'
   ```

   Replace `AWSACCESSKEY` and `AWSSECRETKEY` with your actual AWS access key ID and secret access key.

4. **Initiate Cluster Clone and Restore:**

   Execute the following command to create a new cluster (new-cluster) based on the configuration template and restore it from the backup created by the source cluster:

   ```bash
   kubectl tg restore --name cross-restore \
     --namespace tigergraph --cluster-name new-cluster \
     --metadata backup-metadata --cluster-template /home/test-cluster_backup_1668069319.yaml \
     --source s3Bucket --s3-bucket tg-backup  \
     --aws-secret aws-secret
   ```

   This command will create a new cluster named `new-cluster` based on the provided cluster template and restore its state from the specified backup.

Once the process is complete, the new cluster (`new-cluster`) will be initialized and ready for use. The restore ensures that the new cluster matches the state of the source cluster captured by the backup.

Remember to adjust the cluster names, backup tag, S3 bucket name, paths, and AWS credentials as needed for your specific setup. Cloning a cluster from a backup is a powerful way to quickly replicate environments and configurations for testing, development, or disaster recovery purposes.

### Show Status of Restore

To check the status of a restore process, you can use the following command:

```bash
kubectl tg restore status --name restore-from-local --namespace $NAMESPACE
```

This command will provide you with details about the ongoing or completed restore process. You can review the information in the output, including any events or messages related to the restore job. The status will indicate whether the restore was successful or if there were any issues.

### Delete Restore Job

If you want to delete a restore job, you can use the following command:

```bash
kubectl tg restore delete --name restore-from-local --namespace $NAMESPACE
```

This command will delete the specified restore job. Make sure to replace `restore-from-local` with the actual name of the restore job you want to delete, and provide the appropriate namespace using the `$NAMESPACE` variable.


