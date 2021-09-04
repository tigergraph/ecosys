# !/usr/bin/sh
export key=~/yuchen_key.pem
export sys=ubuntu
export user=ubuntu
ip_list="172.31.20.237"

for ip in ${ip_list}
do
  echo "setup m${i}"
  scp -i $key setup_${sys}.sh ${user}@${ip}:~
  ssh -i $key ${user}@${ip} "nohup sh setup_${sys}.sh > foo.out 2>&1 < /dev/null & "
done
