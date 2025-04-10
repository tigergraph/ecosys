# Kubectl tg plugin Troubleshoot

This document provides solutions to common issues that may arise when using the `kubectl tg` plugin to deploy or manage TigerGraph clusters in Kubernetes.

- [Kubectl tg plugin Troubleshoot](#kubectl-tg-plugin-troubleshoot)
  - [Issues encountered when using the `kubectl tg` plugin on MacOS](#issues-encountered-when-using-the-kubectl-tg-plugin-on-macos)
  - [Use `kubectl tg` plugin on Windows](#use-kubectl-tg-plugin-on-windows)


## Issues encountered when using the `kubectl tg` plugin on MacOS

We only ensure that the `kubectl tg` command can run on GNU/Linux because it relies on some GNU Core Utilities such as `sed` and `grep`.
The builtin commands in the MacOS terminal are not GNU commands, and they often have different options compared to GNU commands, which can cause `kubectl tg` to not function properly. Here is an error message that you might encounter when running `kubectl tg` on MacOS:

```bash
> kubectl tg create --cluster-name test-cluster --private-key-secret ssh-key-secret \
  --version 4.1.0 --storage-class standard --storage-size 10G -n tigergraph \
  --tigergraph-config "System.Backup.TimeoutSec=900,Controller.BasicConfig.LogConfig.LogFileMaxSizeMB=40"

grep: invalid option -- P
usage: grep [-abcdDEFGHhIiJLlMmnOopqRSsUVvwXxZz] [-A num] [-B num] [-C[num]]
    [-e pattern] [-f file] [--binary-files=value] [--color=when]
    [--context[=num]] [--directories=action] [--label] [--line-buffered]
    [--null] [pattern] [file ...]
Unable to parse the input labels or annotations: System.Backup.TimeoutSec=900,Controller.BasicConfig.LogConfig.LogFileMaxSizeMB=40. A typical example is: --pod-annotations k1=v1,k2="v2",k3='v 3',k4=\"abc,123\"
```

`kubectl tg` relies on GNU `grep` to parse the value of `--tigergraph-config`, and the MacOS terminal does not have GNU `grep` installed by default. That's why you see the error message `grep: invalid option -- P`.

We highly recommend that you run the `kubectl tg` command on a GNU/Linux system. If you are using MacOS, you can install GNU core utilities using Homebrew.

## Use `kubectl tg` plugin on Windows

`kubectl tg` plugin is not supported on Windows. If you are using Windows, you can use Windows Subsystem for Linux (WSL) to run the `kubectl tg` plugin.
Please refer to [Windows Subsystem for Linux Documentation](https://learn.microsoft.com/en-us/windows/wsl/) to install WSL on your Windows machine.
