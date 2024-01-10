# How to Debug Backup & Restore

## General Guidelines

* It is important to avoid initiating multiple backup and restore jobs simultaneously for the same cluster.

    Attempting to do so may result in the following outcomes:

  * If a backup job is already in progress and you attempt to create another `TigerGraphBackup` to back up the identical cluster, the controller will await the completion of the ongoing job before generating a backup job for the new `TigerGraphBackup`.

  * If a restore job is currently active and you create another `TigerGraphRestore` for the same cluster, the controller will wait for the ongoing job to finish before creating a restore job for the new `TigerGraphRestore`.

  * In case a backup job is running and you create a new `TigerGraphRestore`, or if a restore job is ongoing and you create an additional `TigerGraphBackup`, the subsequently created job will encounter failure.

* If the targeted cluster for backup or restore is not in a ready state (e.g., the cluster is in an uninitialized state, undergoing shrinkage, or undergoing an upgrade),the backup/restore controller will patiently await the cluster's return to a normal state before proceeding to create the backup/restore job.

* Up to three pods responsible for executing backup or restore operations will be maintained for each cluster. These pods can prove useful for debugging purposes.

* Should the backup process extend beyond the configured backup schedule interval, resulting in a duration exceeding the scheduled backup window, the scheduled backup will be missed. For instance, if your backup schedule is set to `0 * * * *`, indicating an hourly backup at the 1st minute of each hour, and if a backup process takes 1.5 hours, a backup job initiated at 00:00 will conclude at 01:30, leading to the scheduled 01:00 backup job being skipped.
  
## Debug backup or restore job

When dealing with backup and restore jobs, it's important to be able to troubleshoot and diagnose any issues that may arise. Here's a guide on how to debug backup and restore operations:

1. **List Pods**: To begin, you can list pods running backup in the specified namespace using the following command:

    ```bash
    kubectl get pods -n NAMESPACE -l tigergraph.com/backup-cluster=test-cluster
    ```

    This will give you an overview of pods running backup for test-cluster in the specified namespace. You can replace "test-cluster" with the name of your cluster.

    ```bash
    NAME                                                      READY   STATUS      RESTARTS        AGE
    test-cluster-backup-local-backup-job-7sbcs                0/1     Completed   0               2d
    test-cluster-backup-local-backup-job-7xd58                0/1     Error       0               5d13h
    ```

2. **Identify Backup and Restore Pods**: Look for pods related to backup and restore operations. These pods are typically named `${BACKUP_NAME}-backup-job-{SUFFIX}` for backup jobs and `${RESTORE_NAME}-restore-job-{SUFFIX}` for restore jobs.

3. **Check Pod Status**: Check the status of the pods. If a pod's status is "Error" or not in the "Running" state, it indicates an issue with the backup or restore process.

4. **View Logs**: To view the logs of a specific pod, you can use the following command:

   ```bash
   kubectl logs $POD_NAME -n NAMESPACE
   ```

   Replace `$POD_NAME` with the name of the pod you want to inspect, and specify the appropriate namespace using the `-n NAMESPACE` flag. The logs may provide valuable information about any errors or issues that occurred during the backup or restore job.

    ```bash
    > kubectl logs test-cluster-backup-job-7xd58  
    Warning: Permanently added '[test-cluster-internal-service.default]:10022' (ED25519) to the list of known hosts.
    Fri Dec 16 13:44:19 UTC 2022
    Start configure backup
    [   Info] Configuration has been changed. Please use 'gadmin config apply' to persist the changes.
    [   Info] Configuration has been changed. Please use 'gadmin config apply' to persist the changes.
    Use Local Storage
    [   Info] Configuration has been changed. Please use 'gadmin config apply' to persist the changes.
    [   Info] Configuration has been changed. Please use 'gadmin config apply' to persist the changes.
    [   Info] Configuration has been changed. Please use 'gadmin config apply' to persist the changes.
    Apply config
    [Warning] No difference from staging config, config apply is skipped.
    [   Info] Successfully applied configuration change. Please restart services to make it effective immediately.
    Create backup
    [  Error] NotReady (check backup dependency service online get error: NotReady (GPE is not available; NotReady (GSE is not available)))
    ```

5. **Troubleshoot Errors**: Examine the logs for any error messages or warnings. These messages can help you identify the root cause of the problem. Common issues could include connectivity problems, resource limitations, or configuration errors. For instance, in above logs we can know that the reason of this error is that GPE is not ready.

6. **Verify Configuration**: Double-check the configuration options provided for the backup or restore job. Ensure that paths, destinations, tags, and other settings are correctly specified.

7. **Permissions and Secrets**: Ensure that any necessary permissions, access keys, or secrets (such as AWS credentials) are correctly configured and accessible to the pods.

8. **Retry or Rerun**: If the issue is transient, you might consider retrying the backup or restore operation. You can also delete failed pods and trigger the job again.

9. **Documentation**: Refer to the official documentation for TigerGraph's backup and restore features for more detailed troubleshooting steps and specific error messages.

By following these steps, you can effectively troubleshoot and resolve issues with backup and restore operations.

## Debug backup schedule job

When debugging backup schedules in TigerGraph, you may encounter issues with the scheduled backup jobs. Here's a step-by-step guide on how to troubleshoot and debug backup schedule problems:

1. **List Pods**: Start by listing pods running backup in the specified namespace to identify the pods related to backup schedule operations:

   ```bash
   kubectl get pods -n NAMESPACE -l tigergraph.com/backup-cluster=test-cluster
   ```

2. **Identify Schedule Pods**: Look for pods with names resembling `${BACKUP_SCHEDULE_NAME}-backup-cronjob-{SUFFIX}`. These pods are associated with the scheduled backup jobs created by the `TigerGraphBackupSchedule`.

3. **Check Pod Status**: Examine the status of the pods. If a pod's status is not "Completed" or if it is in a non-running state, it indicates an issue with the backup schedule.

4. **View All Logs**: To view the logs of a specific pod, including all containers within the pod, use the following command:

   ```bash
   kubectl logs $POD_NAME -n $NAMESPACE --all-containers=true
   ```

   Replace `$POD_NAME` with the name of the pod you want to inspect, and specify the appropriate namespace using the `-n NAMESPACE` flag. The `--all-containers=true` option ensures that logs from all containers within the pod are displayed.

5. **Analyze Logs**: Carefully analyze the logs to identify any error messages, warnings, or anomalies. Look for clues that may point to the cause of the issue, such as connectivity problems, configuration errors, or resource limitations.

By following these steps, you can effectively troubleshoot and resolve issues with backup schedule operations. If you encounter specific error messages or need further assistance, you can refer to the documentation or seek help.
