# Status of Backup and Restore

## Status of TigerGraphBackup

You can check the status of all TigerGraphBackups in a specific namespace by running the following command:

```bash
# if you have kubectl tg installed
kubectl tg backup status -n $NAMESPACE

# alternatively, use kubectl
kubectl get tgbackup -n $NAMESPACE
```

The output will be similar to the following:

```bash
NAME                  CLUSTER        TAG    STORAGE   INCREMENTAL   STARTTIME   COMPLETIONTIME   STATUS    AGE
backup-test-cluster   test-cluster   test   local                   88s         75s              Succeed   88s
```

The field `STATUS` shows the status of the backup.

You can also check the status of a specific TigerGraphBackup by running the following command:

```bash
# if you have kubectl tg installed
kubectl tg backup status -n $NAMESPACE --name $BACKUP_NAME

# alternatively, use kubectl
kubectl describe tgbackup $BACKUP_NAME -n $NAMESPACE
```

You can check the Status field in the output to see the status of the backup:

```bash
Status:
  Completion Time:  2024-01-03T07:54:26Z
  Conditions:
    Last Transition Time:  2024-01-03T07:54:26Z
    Message:               Backup succeed
    Reason:                BackupSucceed
    Status:                True
    Type:                  Succeed
  Start Time:              2024-01-03T07:54:13Z
  Target Ready:            true
```

The `Status.Conditions[0].Type` is identical to the `STATUS` field in the output of `kubectl tg backup status`.

The following table lists the possible values of the `Status.Conditions[0].Type` field:

| Status.Conditions[0].Type | Description |
| --- | --- |
| Succeed | The backup is completed. |
| Failed | The backup failed. You should check the logs of backup-job. |
| Active | The backup is in progress. |
| Retrying | The backup job failed at least once, and is retrying. |
| Waiting | The backup job is waiting for the target cluster to be ready. |
| Forbidden | The backup is forbidden because some configurations are not supported for target cluster(the version of target cluster is too old). |
| BackOff | The backup job failed too many times, and is backoff. |

## Status of TigerGraphRestore

You can check the status of all TigerGraphRestores in a specific namespace by running the following command:

```bash
# if you have kubectl tg installed
kubectl tg restore status -n $NAMESPACE

# alternatively, use kubectl
kubectl get tgrestore -n $NAMESPACE
```

The output will be similar to the following:

```bash
NAME         STARTTIME   COMPLETIONTIME   CLUSTER        TAG                      STATUS   AGE
restore-tg   8s                           test-cluster   test-2024-01-03T075416   Active   8s
```

The field `STATUS` shows the status of the restore.

You can also check the status of a specific TigerGraphRestore by running the following command:

```bash
# if you have kubectl tg installed
kubectl tg restore status -n $NAMESPACE --name $RESTORE_NAME

# alternatively, use kubectl
kubectl describe tgrestore $RESTORE_NAME -n $NAMESPACE
```

You can check the Status field in the output to see the status of the restore:

```bash
Status:
  Conditions:
    Last Transition Time:  2024-01-03T08:15:55Z
    Message:               Restore job is running
    Reason:                RestoreActive
    Status:                True
    Type:                  Active
  Start Time:              2024-01-03T08:15:55Z
  Target Ready:            true
```

The `Status.Conditions[0].Type` is identical to the `STATUS` field in the output of `kubectl tg restore status`.

The following table lists the possible values of the `Status.Conditions[0].Type` field:

| Status.Conditions[0].Type | Description |
| --- | --- |
| Succeed | The restore is completed. |
| Failed | The restore failed. You should check the logs of restore-job. |
| Active | The restore is in progress. |
| Retrying | The restore job failed at least once, and is retrying. |
| Waiting | The restore job is waiting for the target cluster to be ready. |
| Forbidden | The restore is forbidden because some configurations are not supported for target cluster(the version of target cluster is too old). |
| BackOff | The restore job failed too many times, and is backoff. |
| ClusterCreating | (Only for cloning cluster) The target cluster is being created. |

## Status of TigerGraphBackupSchedule

You can check the status of all TigerGraphBackupSchedules in a specific namespace by running the following command:

```bash
# if you have kubectl tg installed
kubectl tg backup-schedule list -n $NAMESPACE

# alternatively, use kubectl
kubectl get tgbackupsch -n $NAMESPACE
```

The output will be similar to the following:

```bash
NAME        CLUSTER        TAG   LASTSCHEDULE   LASTSUCCESSFUL   SUCCESS   FAILURE   STATUS    AGE
backupsch   test-cluster   sch   50s            37s              1                   Succeed   2m20s
```

The field `STATUS` shows the status of the backup schedule.

You can also check the status of a specific TigerGraphBackupSchedule by running the following command:

```bash
# if you have kubectl tg installed
kubectl tg backup-schedule status -n $NAMESPACE --name $BACKUP_SCHEDULE_NAME

# alternatively, use kubectl
kubectl describe tgbackupsch $BACKUP_SCHEDULE_NAME -n $NAMESPACE
```

You can check the Status field in the output to see the status of the backup schedule:

```bash
Status:
  Conditions:
    Last Transition Time:  2024-01-03T08:25:13Z
    Message:               Backup job succeed.
    Reason:                BackupSucceed
    Status:                True
    Type:                  Succeed
  Job Counter:
    Successful Jobs:     1
  Last Schedule Time:    2024-01-03T08:25:00Z
  Last Successful Time:  2024-01-03T08:25:13Z
```

The `Status.Conditions[0].Type` is identical to the `STATUS` field in the output of `kubectl tg backup-schedule status`.

The following table lists the possible values of the `Status.Conditions[0].Type` field:

| Status.Conditions[0].Type | Description |
| --- | --- |
| Succeed | The last scheduled backup job succeeded. |
| Failed | The last scheduled backup job failed. |
| Active | The last scheduled backup job is in progress. |
| Forbidden | The backup is forbidden because some configurations are not supported for target cluster(the version of target cluster is too old). |
