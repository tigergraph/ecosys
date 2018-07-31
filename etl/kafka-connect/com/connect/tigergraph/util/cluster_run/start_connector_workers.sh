#!/bin/bash
binaryPath=$1
if [ -z "$1" ] 
then
  echo "Please provide absolute path for binary of connect-distributed.sh \n   e.g. /kafkafolder/bin/connect-distributed.sh"
  exit 0
fi
if [ ! -f "$binaryPath" ] 
then
  echo "Cannot find connect-distributed.sh from given path \'$binaryPath\'!" 
  exit 0
fi
filename=$2
if [ -z "$2" ] 
then
  echo "Please provide the basic config file for the connect worker."
  exit 0
fi
if [ ! -f "$filename" ] 
then
  echo "Cannot find the input config file \'$filename\'!" 
  exit 0
fi
limit=$3
if [ -z "$3" ]
then
  echo "The number of connect worker is not specified, default value 4 is used."
  limit=4
fi
PIDFilename="pid.log"
for (( c=0; c<$limit; c++))
do
   echo "Start the connect-worker $c"
   awk -v var="$c" 'BEGIN{FS="="}{if(NF==2 && $1=="rest.port"){a=$2 + var; print $1"="a}else{print}}' $filename > "tigerConfig"$c
   logfile="connect-worker$c.log"
   nohup $binaryPath "tigerConfig"$c > $logfile 2>&1 & echo $! >> $PIDFilename
   echo "nohup job for connct-worker $c started, please check $logfile for details."
done 
sleep 1
echo -e "\nnohup jobs are here:";jobs -l
