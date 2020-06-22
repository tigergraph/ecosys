#!/bin/bash

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
