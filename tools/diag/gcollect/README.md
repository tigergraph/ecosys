# gcollect: A TigerGraph Debug Tool
gcollect is a TigerGraph debug tool, which could be used to collect all the debug information that is needed for TigerGraph debugging.

## Installation
To compile this tool, please install the latest Golang and clone this repo on your computer.
```
export GOPATH=/path/to/gcollect
go get golang.org/x/crypto/ssh
go get gopkg.in/yaml.v2
go install gcollect
```

## Examples of typical usage

```
# show all requests during the last hour
./bin/gcollect -t 3600 show
# collect debug info for a specific request, either contains "RESTPP_2_1.1559075028795" or "error", from [T - 60s] to [T + 120s] (T is the time when the request was issued)
./bin/gcollect -r RESTPP_2_1.1559075028795 -b 60 -d 120 -p "error" collect
# collect debug info from "5/22/2019,18:00" to "5/22/2019,19:00" for all components, either contains "error" or "FAILED", case insensitive.
./bin/gcollect -i -p "error" -p "FAILED" -s "2019-05-22,18:00:00" -e "2019-05-22,19:00:00" collect
# Search for "unknown" from log files that have been collected before, only search components "admin" and "gpe", case insensitive, print 1 line of trailing context after matching lines, print 2 lines of leading context before matching lines, print to screen.
./bin/gcollect -i -p "unknown" -c admin,gpe -D -A 1 -B 2 grep
```

### Debug information that will be collected

```
* OS version (cat /etc/*release)
* TigerGraph version (gadmin version)
* Services status (gadmin status)
* Services status history (From ts3)
* Free disk space (df -lh)
* Free memory (free -g)
* OOM information (dmesg | grep -i oom)
* gstore yaml files (gstore/0/part/config.yaml, gstore/0/1/ids/graph_config.yaml)
* kafka log files (kafka/kafka.out)
* zk log files (zk/zookeeper.out.*)
* nginx log files (logs/nginx/nginx_*.access.log)
* restpp log files (logs/RESTPP_*_1/log.INFO, logs/restpp/restpp_*_1.out)
* restpp loader log files (logs/RESTPP-LOADER_*_1/log.INFO)
* gpe log files (logs/GPE_*_1/log.INFO, logs/gpe/gpe_*_1.out)
* gse log files (logs/GSE_*_1/log.INFO, logs/gse/gse_*_1.out)
* gsql log (~/tigergraph/dev/gdk/gsql/logs/GSQL_LOG)
* fab logs (~/tigergraph/.gsql/fab_dir/cmd_logs)
* ium config (gadmin --dump-config)
* GSQL *catalog*: (mkdir catalog && gadmin __pullfromdict catalog)
* admin server (logs/admin_server/gadmin_server.out, logs/admin_server/gadmin_server.INFO)
* gdict logs(logs/dict/gdict_client_.INFO, logs/dict/gdict_server.INFO)
* tsar log (/var/log/tsar.data*)
* ts3 DB (~/tigergraph/data/ts3.db)
* restpp loader logs (logs/restpp/restpp_loader_logs)
* GST logs (logs/gui/gui_INFO.log)
* kafka connector (kafka/kafka-connect.out)
* kafka stream (kafka/kafka-stream.out)
```

For distributed mode, all the information from each node will be collected.

## Synopsis
```
gcollect [Options] COMMAND

Options:
-h, --help                        show this help message and exit
-A num, --after-context num       Print num lines of trailing context after each match.
-B num, --before-context num      Print num lines of leading context before each match.
-c, --components gpe,gse,rest     only collect information related to the specified component(s). All by default. Supported components: gpe,gse,gsql,dict,tsar,kafka,zk,rest,nginx,admin,restpp_loader,fab,kafka-stream,kafka-connect
-n, --nodes m1,m2                 only search patterns for specified nodes. (only works in together with command "grep")
-s, --start DateTime              logs older than this DateTime will be ignored. Format: 2006-01-02,15:04:05
-e, --end DateTime                logs newer than this DateTime will be ignored. Format: 2006-01-02,15:04:05
-t, --tminus num                  only search for logs that are generated in the past num seconds.
-r, --request_id id               only collect information related to the specified request id. Lines match "pattern" will also be printed.
-b, --before num                  how long before the query should we start collecting. (in seconds, could ONLY be used with [--reqest_id] option).
-d, --duration num                how long after the query should we stop collecting. (in seconds, could ONLY be used with [--reqest_id] option).
-o, --output_dir dir              specify the output directory, "./output" by default. (ALERT: files in this folder will be DELETED.)
-p, --pattern regex               collect lines from logs which match the regular expression. (Could have more than one regex, lines that match any of the regular expressions will be printed.)
-i, --ignore-case                 Ignore case distinctions in both the PATTERN and the input files.
-D, --display                     Print to screen.

Commands:
grep                              search patterns from logs files that have been collected before.
show                              show all the requests during the specified time window.
collect                           collect all the debugging information which satisfy all the requirements specified by Options.
```

## Output format
All the log files will be printed in the output directory, and each node has a subdirectory. Each component will have one or two log files.
