# Lifecycle of TigerGraph

TigerGraph CR follows a designed lifecycle to manage the TigerGraph cluster. Starting from `InitializeRoll` status, moving to `InitializePost` status. In `InitializePost` status, TigerGraph Operator will create an init-job to initialize TigerGraph system. After the init-job is finished, TigerGraph Operator will move the TigerGraph CR to `Normal` status.

- [Lifecycle of TigerGraph](#lifecycle-of-tigergraph)
  - [Configure Lifecycle Hooks in TigerGraph CR](#configure-lifecycle-hooks-in-tigergraph-cr)
    - [PostInitAction](#postinitaction)
  - [Configure Lifecycle Hooks by kubectl-tg](#configure-lifecycle-hooks-by-kubectl-tg)
    - [--post-init-action](#--post-init-action)

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

## Configure Lifecycle Hooks by kubectl-tg

### --post-init-action

You may want to execute some commands in **TigerGraph container** once the TigerGraph system is initialized. We provide a flag `--post-init-action` in `kubectl tg create` and `kubectl tg update` to support this. You can specify a bash script in this flag, and the script will be put into the init-job and be executed in one of the TigerGraph pods after the TigerGraph system is initialized. Create a script file `post-init-action.sh`:

```bash
echo "This is a post init action" >> /tmp/post-init-action.log
```

Then you can create a cluster with this post init action by:

```bash
kubectl tg create --cluster-name test-cluster --namespace tigergraph \
  --post-init-action post-init-action.sh ${OTHER_OPTIONS}
```

> [!WARNING]
> If your PostInitAction failed, the whole init-job will be considered as failed, and the TigerGraph CR will be moved to `InitializePost,False` status.
> So please make sure your PostInitAction is correct before you configure it, and you should also handle any errors in the PostInitAction script.
> If your PostInitAction failed and the cluster is in `InitializePost,False` status, you can use `kubectl tg update` to correct the PostInitAction,
> then operator will create a new init-job to execute the PostInitAction again.
