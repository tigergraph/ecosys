#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

usage(){
  echo "Usage: `basename $0` [-g <graph_name> -j <loading_jobs_list> | <loading_config_file>]"
  echo "  graph_name:          Graph name to be used"
  echo "  loading_jobs_list:   List of the loading jobs (separated by ',') to run"
  echo "  loading_config_file: Path to the config file, see the example below"
  echo ""
  echo "Example:"
  echo "  `basename $0` -g tpc_graph -j load_test"
  echo "  `basename $0` ~/loading_config"
  echo ""
  echo "[Sample loading config]
# the name of the graph for the initial loading
GRAPH_NAME=\"tpc_graph\"

# the name of loading jobs separated by white space
LOADING_JOBS=(\"load_test\")
"
  exit 1
}

cwd=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)

while [ -n "$1" ];
do
  case "$1" in
    -g)
      shift
      GRAPH_NAME=$1
      ;;
    -j)
      shift
      LOADING_JOBS=($(echo $1 | sed 's/,/ /g'))
      ;;
    -h)
      usage
      exit 0
      ;;
    *)
      [ -z "$LOADING_CONFIG" ] && LOADING_CONFIG=$1
      ;;
  esac
  shift
done

if [ -n "$LOADING_CONFIG" ]; then
  if [ -f "$LOADING_CONFIG" ]; then
    source $LOADING_CONFIG
  else
    echo "Loading config $LOADING_CONFIG is not found."
    exit 1
  fi
fi

if [ -z "$GRAPH_NAME" ] || [ -z "$LOADING_JOBS" ]; then
  echo "Graph name or loading job is not provided, exiting..."
  exit 2
fi

if ! which gadmin > /dev/null 2>&1; then
  echo "gadmin is not found, please check PATH setting."
  exit 3
fi

printf "${YELLOW}Run loading job(s) \"${LOADING_JOBS[*]}\" for graph \"${GRAPH_NAME}\". ${RED}[WARNING: all data will be lost!] (y/N):${NC}"
read user_opt
if [ "${user_opt}" != "y" -a "${user_opt}" != "Y" -a "${user_opt}" != "yes" ]; then
  exit 0
fi

MYPROMPT(){
  typeset cmnd="$*"
  echo "$(date +"[%Y-%m-%d %T] ") $cmnd"
}
############################################################################################
# before run, make sure
# 1: loading job was created
# 2: gadmin config set Kafka.MessageMaxSizeMB 90  [optional]
############################################################################################
NUM_PB=80

#0 get config and echo them
LOG_DIR=$(gadmin config get System.LogRoot)
LOADING_LOG_DIR=$LOG_DIR"/restpp/restpp_loader_logs/"$GRAPH_NAME
ROOTDIR=$(gadmin config get System.AppRoot)
GST_DIR=$(gadmin config get System.DataRoot)/gstore
DEV_DIR=$(gadmin config get System.AppRoot)/dev
DLT_DIR=${GST_DIR}/0/batchdelta
BIN_DIR=${ROOTDIR}/bin
DLT_BIN=batchdelta
GPE_QUIT_KW_1="Destory"
GPE_QUIT_KW_2="DeltaRecords"
NUM_GPE=$(gadmin status -v | grep GPE | wc -l)

echo "LOG_DIR :" ${LOG_DIR}
echo "LOADING_LOG_DIR :" ${LOADING_LOG_DIR}
echo "GSTORE  :" ${GST_DIR}
echo "DELTA_D :" ${DLT_DIR}
echo "DELTA_E :" ${DLT_BIN}
echo "BIN_DIR :" ${BIN_DIR}
echo "NUM_GPE :" ${NUM_GPE}
echo "KEY_QUIT:" ${GPE_QUIT_KW_1} ${GPE_QUIT_KW_2}
CHECK_LOADING(){
  TEMP_LOADING_FAIL_DIR="/tmp/tmp_loading_fail/"
  LOADING_FAIL_DIR="/tmp/loading_fail/"
  rm -rf "$LOADING_FAIL_DIR"
  mkdir -p "$LOADING_FAIL_DIR"
  #fetch all loading log files
  LOADING_FAILS=false
  gfetch all $TEMP_LOADING_FAIL_DIR $LOADING_FAIL_DIR
  for file in $(ls $LOADING_FAIL_DIR*/*); do
    echo "[LOADING FAIL] $file"
    LOADING_FAILS=true
  done
  if [ "$LOADING_FAILS" = true ]; then
    return 1
  fi
}

MARK_LOADING(){
  MACHINE=$(ghostname | cut -d' ' -f1)
  #clear out the tmp folder
  TEMP_LOADING_FAIL_DIR="/tmp/tmp_loading_fail/"
  #remove any existing loading tmp loading log
  rm -rf "$TEMP_LOADING_FAIL_DIR"
  mkdir -p "$TEMP_LOADING_FAIL_DIR"
  LOG_DIR=$(gadmin config get System.LogRoot)
  LOADING_LOG_DIR=$LOG_DIR"/restpp/restpp_loader_logs/"$GRAPH_NAME
  for f in $(ls $LOADING_LOG_DIR/*.progress); do
    summary=$f".summary"
    #if there is no corresponding ".summary" file, then loading fails
    if [ ! -f $summary ]; then
      touch "$TEMP_LOADING_FAIL_DIR$summary"".missing"
    fi
    #check whether the summary has aborted loading info
    err=$(cat $summary | sed 's/.*"error"\s*:\s*\(true\|false\)\s*[,}].*/\1/g')
    #if the loading fails the error field will be true
    if [ "$err" == "true" ]; then
      echo "[LOADING_ABORTED] $MACHINE:$f"
      #show loading summary from aborted loading
      cp -f $summary $TEMP_LOADING_FAIL_DIR$(basename $summary)".fail"
    else
      echo "[LOADING_SUCCESS] $MACHINE:$f"
    fi
  done
}

#1 Make sure it is an empty stage with no gstore and Kafka data
gadmin restart -y
sleep 5
MYPROMPT STEP_1: RESET
gsql 'clear graph store -HARD'
MYPROMPT STEP_1: DONE

#2 SWITCH MODE to DELTA LOADING MODE: enable this by adding "GDeltaLoading=1" in "GPE" section
MYPROMPT STEP_2: START SYSTEM, LOADING MODE
#current_runtime_config=$(echo $(gadmin config get GPE.BasicConfig.Env) | sed 's/\$/\\\$/g')
current_runtime_config=$(echo $(gadmin config get GPE.BasicConfig.Env))
gadmin config set GPE.BasicConfig.Env "${current_runtime_config}; GDeltaLoading=1"
gadmin config apply -y
gadmin restart -y
sleep 5
MYPROMPT STEP_2: DONE

#3 Start the loading
MYPROMPT STEP_3: START LOADING
#clear out all loading logs
grun all "rm -rf \"$LOADING_LOG_DIR\""
#start the loading job one by one, running in the background
for job in "${LOADING_JOBS[@]}"
do
  gsql -g $GRAPH_NAME "RUN LOADING JOB $job" &
  echo "[LOADING_SUBMITTED] Loading job '$job' has been submitted."
done
#wait for all loading job to be finished
wait
#use grun to check the loading result first, if there is a loading fail, create a loading failed file
grun all "GRAPH_NAME=$GRAPH_NAME; $( typeset -f MARK_LOADING); MARK_LOADING"
#check whether there is any loading failed file
CHECK_LOADING
MYPROMPT STEP_3: DONE

#4 Wait loading job is done
#4.1 signal
MYPROMPT STEP_4: FINALIZE LOADING
grun gpe 'gpe_id=$(pgrep gpe); kill ${gpe_id}'
MYPROMPT STEP_4: SINGAL. START WAITING
#4.2 wait
while : ; do
    sleep 30
    RUNNING=$(grun gpe 'pgrep gpe' | grep -o ^'[0-9]*'  | wc -l)
    [ ${RUNNING} -ne 0 ] || break;
    MYPROMPT "STEP_4: WAITING on " ${RUNNING}
done
MYPROMPT STEP_4: DONE WAITING
#4.3 verification
QUIT_CNT=$(grun gpe 'grep '${GPE_QUIT_KW_1}' '${LOG_DIR}'/gpe/log.INFO' |grep ${GPE_QUIT_KW_2} | wc -l)
if [ ${QUIT_CNT} -ne ${NUM_GPE} ] ; then
    MYPROMPT STEP_4: Failed in verification \"${GPE_QUIT_KW_1} ${GPE_QUIT_KW_2}\"
    exit -1
fi
MYPROMPT STEP_4: DONE

#5 Stop all services first then run batchdelta
MYPROMPT STEP_5: BUILD GSTORE: Mode=${NUM_PB}
gadmin stop -y
grun_p gpe 'export ProcessMode='${NUM_PB}';'${BIN_DIR}/${DLT_BIN}' ~/.tg.cfg > '${LOG_DIR}'/delta.out 2>&1'

#6 Wait delta build is done
MYPROMPT STEP_6: WAIT BUILD GSTORE
while : ; do
    sleep 10
    RUNNING=$(grun gpe 'pgrep '${DLT_BIN}  | grep -o ^'[0-9]*'  | wc -l)
    [ ${RUNNING} -ne 0 ] || break;
done
MYPROMPT STEP_6: DONE BUILD GSTORE

#7 SWITCH MODE to ONLINE: remove "GDeltaLoading=1" in "GPE" section.
MYPROMPT STEP_7: SWITCH MODE to ONLINE
grun_p all "rm -rf ${GST_DIR}/0/batchdelta/*"
gadmin config set GPE.BasicConfig.Env "${current_runtime_config}"
gadmin config apply -y

#8 Reset and Restart
MYPROMPT STEP_8: RESET and START
gadmin reset -y
gadmin start
MYPROMPT STEP_8: DONE
