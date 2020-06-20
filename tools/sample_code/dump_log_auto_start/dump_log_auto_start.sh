#!/bin/bash

if ~/.gium/gadmin status -v gpe gse | grep PROC | grep False; then
    # dump 600 seconds logs before gpe/gse is down
    ~/.gium/gcollect -t 600 -o /tmp/dumplog-`date +"%Y%m%d-%T"` collect
    ~/.gium/gadmin start gpe gse
fi
