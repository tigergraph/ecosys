#!/bin/bash

ssh_pem=~/keys/gsql_zz.pem
ssh_usr=ubuntu
echo $1
IFS=$'\n' read -d '' -r -a lines <  pip.txt
for i in "${lines[@]}"
do
  echo "---- "$i
  if [ ! -f "$1" ]
  then
    ssh -i $ssh_pem $ssh_usr@$i "sudo bash -c '$1'"
  else
    ssh -i $ssh_pem $ssh_usr@$i 'sudo bash -s' < $1 
  fi
done
