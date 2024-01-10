# Backup and Restore Overview

This document describes how to perform backup and restore on TigerGraph cluster on Kubernetes.
To backup and restore your data, you can use the `kubectl-tg` plugin or the  YAML file  that corresponds to the  TigerGraphBackup/TigerGraphRestore Custom Resource(CR).

## Difference Between Managing Backup/Restore by YAML File and Using kubectl-tg Plugin

**Using YAML Files:**

1. **Manual Configuration:** With YAML files, you manually craft the configuration settings for backup or restore operations.

2. **Customization:** You can store multiple backup/restore configurations in separate YAML files, enabling customized setups for different scenarios.

**Using kubectl-tg Plugin:**

1. **Simplified Commands:** The `kubectl tg` plugin streamlines the process by providing pre-defined command options that directly create CRs with specified configurations.

2. **Efficiency:** You avoid the need to create YAML files and write configurations manually, accelerating the setup of backup and restore operations.

3. **CR Management:** The `kubectl tg` plugin operates directly on CRs, enabling you to manage and modify them conveniently through commands.

Ultimately, both approaches achieve the same outcome, but the `kubectl tg` plugin simplifies the process by eliminating manual configuration steps and providing a more streamlined and efficient method for managing backup and restore operations.

* See [Backup & Restore cluster by kubectl-tg plugin](./backup-restore-by-kubectl-tg.md) to know how to use `kubectl tg` for backup & restore.
* See [Backup & Restore cluster by CR](./backup-restore-by-cr.md) to get the example YAML files for backup & restore.

## Usage scenarios

### Back up data

You can create backups of your TigerGraph clusters and store the backup files to Local storage or S3 bucket. Refer to:

* [Backup to Local Storage](./backup-restore-by-kubectl-tg.md#backup-to-local-storage)
* [Backup to S3 Bucket](./backup-restore-by-kubectl-tg.md#backup-to-an-s3-bucket)

You can create a backup schedule to backup cluster periodically. Refer to:

* [Creating and Managing Backup Schedules](./backup-restore-by-kubectl-tg.md#creating-and-managing-backup-schedules)
* [TigerGraphBackupSchedule CR](./backup-restore-by-cr.md#tigergraphbackupschedule)

About managing backup files and backup CR, refer to:

* [Listing Backup Custom Resources](./backup-restore-by-kubectl-tg.md#listing-backup-custom-resources)
* [Displaying Backup Process Status](./backup-restore-by-kubectl-tg.md#displaying-backup-process-status)
* [Delete Backup Custom Resource (CR)](./backup-restore-by-kubectl-tg.md#delete-backup-custom-resource-cr)
* [Listing Backups](./backup-restore-by-kubectl-tg.md#listing-backups)
* [Removing Backups](./backup-restore-by-kubectl-tg.md#removing-backups)

### Restore data

If you have created backups of your cluster to Local storage or S3 Bucket, you can restore the cluster using a specific backup. Refer to:

* [Restoring within the Same Cluster](./backup-restore-by-kubectl-tg.md#restoring-within-the-same-cluster)

If you have created backups of your cluster to S3 Bucket, you can restore in another cluster, which we call cross-cluster restore. Refer to:

* [Cross-Cluster Restore from Backup](./backup-restore-by-kubectl-tg.md#cross-cluster-restore-from-backup)

If you want to clone your cluster, you can use cross-cluster to achieve this goal. Refer to:

* [Clone Cluster from Backup](./backup-restore-by-kubectl-tg.md#clone-cluster-from-backup)

## Troubleshoot

If you encounter any Error with backup & restore process, please refer to [How to debug Backup & Restore](./troubleshoot.md) for troubleshooting guidance.
