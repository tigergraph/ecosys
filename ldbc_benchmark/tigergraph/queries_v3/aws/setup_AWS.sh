# !/usr/bin/sh
export key=~/yuchen.pem
export sys=ubuntu
export user=ubuntu
export ip_list="172.31.25.8 172.31.25.82"

for ip in ${ip_list}
do
  echo "setup ${ip}"
  scp -i $key setup_${sys}.sh ${user}@${ip}:~
  ssh -i $key ${user}@${ip} "nohup sh setup_${sys}.sh > foo.out 2>&1 < /dev/null & "
done
