# INSTRUCTIONS

### Purpose
`dump_log_auto_start.sh` is used to monitor services' status, dump log when any service is down and restart the corresponding services.

### Configuration
By default, the script will monitor the status of `gpe` and `gse`, you may add other service (e.g., `restpp`) if needed. Once any service is down, it will dump all the logs from all the machines in the cluster in the past `600` seconds (you may change the time period if needed) to the folder `/tmp/dumplog-[timestamp]` (please change the output folder if needed. For other options, please check out `~/.gium/gcollect -h`). At last, it will start the service.


```
DUMP_LOG_AUTO_START_LOCK="/tmp/.dump_log_auto_start_lock"

if [[ -f ${DUMP_LOG_AUTO_START_LOCK} ]]; then
    exit 0
fi

if ~/.gium/gadmin status -v gpe gse | grep PROC | grep False; then
    touch ${DUMP_LOG_AUTO_START_LOCK}
    # dump 600 seconds logs before gpe/gse is down
    ~/.gium/gcollect -t 600 -o /tmp/dumplog-`date +"%Y%m%d-%T"` collect
    ~/.gium/gadmin start gpe gse
    rm ${DUMP_LOG_AUTO_START_LOCK}
fi
```

### Installation
Edit the script and put it under `~/.gsql`, then run `crontab -e` and add the following line at the end:
```
* * * * * /home/tigergraph/.gsql/dump_log_auto_start.sh >/dev/null 2>&1 </dev/null
```

Save the file and you are all set. The script will run every minute.
