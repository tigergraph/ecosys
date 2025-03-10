# Lifecycle of TigerGraph

TigerGraph CR follows a designed lifecycle to manage the TigerGraph cluster. Starting from `InitializeRoll` status, moving to `InitializePost` status. In `InitializePost` status, TigerGraph Operator will create an init-job to initialize TigerGraph system. After the init-job is finished, TigerGraph Operator will move the TigerGraph CR to `Normal` status.

You may want to perform certain actions at specific stages of the TigerGraph CR lifecycle. For example, changing the username and password after cluster initialization, backing up certain files before pausing the cluster, etc. You can achieve these requirements by configuring the lifecycle. The currently supported lifecycle hooks include `postInitAction`, `prePauseAction`, and `preDeleteAction`.

| Lifecycle Hook | When to execute | Supported Operator Version |
| -------------- | --------------- | ----------------- |
| PostInitAction | After the TigerGraph is initialized | 1.0.0 |
| PrePauseAction | Before pausing the TigerGraph cluster | 1.2.0 |
| PreDeleteAction | Before deleting the TigerGraph cluster | 1.2.0 |

- [Lifecycle of TigerGraph](#lifecycle-of-tigergraph)
  - [Configure Lifecycle Hooks in TigerGraph CR](#configure-lifecycle-hooks-in-tigergraph-cr)
    - [PostInitAction](#postinitaction)
    - [PrePauseAction](#prepauseaction)
    - [PreDeleteAction](#predeleteaction)
  - [Configure Lifecycle Hooks by kubectl-tg](#configure-lifecycle-hooks-by-kubectl-tg)
    - [--post-init-action](#--post-init-action)
    - [--pre-pause-action](#--pre-pause-action)
    - [--pre-delete-action](#--pre-delete-action)

## Configure Lifecycle Hooks in TigerGraph CR

### PostInitAction

You may want to execute some commands in **TigerGraph container** once the TigerGraph system is initialized. We provide a field `.spec.lifecycle.postInitAction` in TigerGraph CR to support this. You can specify a bash script in this field, and the script will be put into the init-job and be executed in the first TigerGraph pod(whose suffix is `-0`) after the TigerGraph system is initialized. For example:

```yaml
spec:
  lifecycle:
    postInitAction: |
      echo "This is a post init action" >> /tmp/post-init-action.log
```

If you configure the above post init action, the command will be executed in the first TigerGraph pods(whose suffix is `-0`) after the TigerGraph system is initialized. You can check the log in the pod:

```bash
kubectl exec -ti test-cluster-0 -n tigergraph -- cat /tmp/post-init-action.log
```

The output should be:

```bash
This is a post init action
```

> [!WARNING]
> If your PostInitAction failed, the whole init-job will be considered as failed, and the TigerGraph CR will be moved to `InitializePost,False` status.
> So please make sure your PostInitAction is correct before you configure it, and you should also handle any errors in the PostInitAction script.
> If your PostInitAction failed and the cluster is in `InitializePost,False` status, you can modify the TigerGraph CR to correct the PostInitAction,
> then operator will create a new init-job to execute the PostInitAction again.

### PrePauseAction

You may want to execute some commands in **TigerGraph container** before pausing the TigerGraph cluster. We provide a field `.spec.lifecycle.prePauseAction` in TigerGraph CR to support this. You can specify a bash script in this field, and the script will be put into the pause-pre-job and be executed in the first TigerGraph pod(whose suffix is `-0`) before pausing the TigerGraph cluster. For example:

```yaml
spec:
  lifecycle:
    prePauseAction: |
      echo "This is a pre pause action" >> /tmp/pre-pause-action.log
```

If you configure the above `PrePauseAction`, the command will be executed in the first TigerGraph pods(whose suffix is `-0`) before pausing the TigerGraph cluster. And the status of the cluster will become `PausePre` once the pause-pre-job starts. After the pause-pre-job is finished, the status of the cluster will become `PauseRoll`, operator will clean all computing resources and move the cluster to `Paused` status. (See more details about pausing cluster in [Pause and Resume TigerGraph cluster](../04-manage/pause-and-resume.md))

> [!WARNING]
> If your PrePauseAction failed, the pause-pre-job will be considered as failed, and the TigerGraph CR will be moved to `PausePre,False` status. The computing resources will not be cleaned, and the cluster will not be paused because the operator has to make sure the PrePauseAction is executed successfully before pausing the cluster.
> So please make sure that your PrePauseAction is correct before you configure it, and you should also handle any errors in the PrePauseAction script.
> If your PrePauseAction failed and the cluster is in `PausePre,False` status, you can modify the TigerGraph CR to correct the PrePauseAction,
> then operator will create a new pause-pre-job to execute the PrePauseAction again.
> If you don't want to execute the PrePauseAction, you can remove the `prePauseAction` field from the TigerGraph CR. Then the cluster will transfer from `PausePre,False` status to `PauseRoll` status directly.

### PreDeleteAction

You may want to execute some commands in **TigerGraph container** before deleting the TigerGraph cluster. We provide a field `.spec.lifecycle.preDeleteAction` in TigerGraph CR to support this. You can specify a bash script in this field, and the script will be put into the delete-pre-job and be executed in the first TigerGraph pod(whose suffix is `-0`) before deleting the TigerGraph cluster. For example:

```yaml
spec:
  lifecycle:
    preDeleteAction: |
      echo "This is a pre delete action" >> /tmp/pre-delete-action.log
```

If you configure the above `PreDeleteAction`, the command will be executed in the first TigerGraph pods(whose suffix is `-0`) before deleting the TigerGraph cluster. And the status of the cluster will become `DeletePre` once the delete-pre-job starts. After the delete-pre-job is finished, the status of the cluster will become `DeleteRoll`, operator will clean all resources of the cluster and remove the finalizer of TigerGraph CR. Then the TigerGraph CR will be cleaned by K8s. (See more details about finalizers in [Finalizers used by TG](../07-reference/finalizers-used-by-tg.md))

> [!IMPORTANT]
> In following scenarios, the PreDeleteAction will not be executed:
>
> 1. The cluster is in `Paused` status when you delete the cluster. Since the cluster is paused, the TigerGraph pods are not running, and the PreDeleteAction cannot be executed.
>
> 2. The cluster is in `InitializeRoll` status, and for some reason the pods are not running. Since the pods are not running, the PreDeleteAction cannot be executed.
>

> [!WARNING]
> If your PreDeleteAction failed, the delete-pre-job will be considered as failed, and the TigerGraph CR will be moved to `DeletePre,False` status. The resources of the cluster will not be cleaned, and the finalizer of the TigerGraph CR will not be removed because the operator has to make sure the PreDeleteAction is executed successfully before deleting the cluster.
> So please make sure that your PreDeleteAction is correct before you configure it, and you should also handle any errors in the PreDeleteAction script.
> If your PreDeleteAction failed and the cluster is in `DeletePre,False` status, you can modify the TigerGraph CR to correct the PreDeleteAction ,
> then operator will create a new delete-pre-job to execute the PreDeleteAction again.
> If you don't want to execute the PreDeleteAction, you can remove the `preDeleteAction` field from the TigerGraph CR. Then the cluster will transfer from `DeletePre,False` status to `DeleteRoll` status directly.

## Configure Lifecycle Hooks by kubectl-tg

### --post-init-action

You may want to execute some commands in **TigerGraph container** once the TigerGraph system is initialized. We provide a flag `--post-init-action` in `kubectl tg create` and `kubectl tg update` to support this. You can specify a bash script in this flag, and the script will be put into the init-job and be executed in one of the TigerGraph pods after the TigerGraph system is initialized. Create a script file `post-init-action.sh`:

```bash
echo "This is a post init action" >> /tmp/post-init-action.log
```

Then you can create a cluster with this `PostInitAction` by:

```bash
kubectl tg create --cluster-name test-cluster --namespace tigergraph \
  --post-init-action post-init-action.sh ${OTHER_OPTIONS}
```

> [!WARNING]
> If your PostInitAction failed, the whole init-job will be considered as failed, and the TigerGraph CR will be moved to `InitializePost,False` status.
> So please make sure your PostInitAction is correct before you configure it, and you should also handle any errors in the PostInitAction script.
> If your PostInitAction failed and the cluster is in `InitializePost,False` status, you can use `kubectl tg update` to correct the PostInitAction,
> then operator will create a new init-job to execute the PostInitAction again.

### --pre-pause-action

You may want to execute some commands in **TigerGraph container** before pausing the TigerGraph cluster. We provide a flag `--pre-pause-action` in `kubectl tg pause` to support this. You can specify a bash script in this flag, and the script will be put into the pause-pre-job and be executed in one of the TigerGraph pods before pausing the TigerGraph cluster. Create a script file `pre-pause-action.sh`:

```bash
echo "This is a pre pause action" >> /tmp/pre-pause-action.log
```

Then you can create a cluster with this `PrePauseAction` by:

```bash
kubectl tg create --cluster-name test-cluster --namespace tigergraph \
  --pre-pause-action pre-pause-action.sh ${OTHER_OPTIONS}
```

If you forget to configure the `PrePauseAction` when creating the cluster, you can also update the cluster to add the `PrePauseAction` by:

```bash
kubectl tg update --cluster-name test-cluster --namespace tigergraph \
  --pre-pause-action pre-pause-action.sh
```

Or you can configure `PrePauseAction` when you pause the cluster by:

```bash
kubectl tg pause --cluster-name test-cluster --namespace tigergraph \
  --pre-pause-action pre-pause-action.sh
```

> [!WARNING]
> If your PrePauseAction failed, the pause-pre-job will be considered as failed, and the TigerGraph CR will be moved to `PausePre,False` status. The computing resources will not be cleaned, and the cluster will not be paused because the operator has to make sure the PrePauseAction is executed successfully before pausing the cluster.
> So please make sure that your PrePauseAction is correct before you configure it, and you should also handle any errors in the PrePauseAction script.
> If your PrePauseAction failed and the cluster is in `PausePre,False` status, you can use `kubectl tg update` to correct the PrePauseAction,
> then operator will create a new pause-pre-job to execute the PrePauseAction again.
> If you don't want to execute the PrePauseAction, you can create an empty file by `touch empty.sh` and use `kubectl tg update --pre-pause-action empty.sh` to remove `PrePauseAction`.
> Then the cluster will transfer from `PausePre,False` status to `PauseRoll` status directly.

### --pre-delete-action

You may want to execute some commands in **TigerGraph container** before deleting the TigerGraph cluster. We provide a flag `--pre-delete-action` in `kubectl tg delete` to support this. You can specify a bash script in this flag, and the script will be put into the delete-pre-job and be executed in one of the TigerGraph pods before deleting the TigerGraph cluster. Create a script file `pre-delete-action.sh`:

```bash
echo "This is a pre delete action" >> /tmp/pre-delete-action.log
```

Then you can create a cluster with this `PreDeleteAction` by:

```bash
kubectl tg create --cluster-name test-cluster --namespace tigergraph \
  --pre-delete-action pre-delete-action.sh ${OTHER_OPTIONS}
```

If you forget to configure the `PreDeleteAction` when creating the cluster, you can also update the cluster to add the `PreDeleteAction` by:

```bash
kubectl tg update --cluster-name test-cluster --namespace tigergraph \
  --pre-delete-action pre-delete-action.sh
```

> [!WARNING]
> If your PreDeleteAction failed, the delete-pre-job will be considered as failed, and the TigerGraph CR will be moved to `DeletePre,False` status.
> The resources of the cluster will not be cleaned, and the finalizer of the TigerGraph CR will not be removed because the operator has to make sure the PreDeleteAction is executed successfully before deleting the cluster.
> So please make sure that your PreDeleteAction is correct before you configure it, and you should also handle any errors in the PreDeleteAction script.
> If your PreDeleteAction failed and the cluster is in `DeletePre,False` status, you can use `kubectl tg update` to correct the PreDeleteAction,
> then operator will create a new delete-pre-job to execute the PreDeleteAction again.
> If you don't want to execute the PreDeleteAction, you can create an empty file by `touch empty.sh` and use `kubectl tg update --pre-delete-action empty.sh` to remove `PreDeleteAction`.
> Then the cluster will transfer from `DeletePre,False` status to `DeleteRoll` status directly.
