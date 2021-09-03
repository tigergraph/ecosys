# !/usr/bin/sh
export key=~/yuchen_key.pem
export sys=ubuntu
export user=ubuntu
ip_list="172.31.20.237"
cp setup_${sys}.sh setup_tmp.sh
echo "sudo pip3 install boto3" >> setup_tmp.sh

for ip in ${ip_list}
do
  echo "setup m${i}"
  scp -i $key setup_tmp.sh ${user}@${ip}:~
  ssh -i $key ${user}@${ip} 'nohup sh setup_tmp.sh > foo.out 2>&1 < /dev/null & '
done
