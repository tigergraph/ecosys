
# !/usr/bin/sh
key=$1
ip_list_file=$2
for ip in $(cat $ip_list_file)
do 
  scp -i $key -o StrictHostKeyChecking=accept-new setup.sh ec2-user@$ip:~
  ssh -i $key ec2-user@$ip "nohup sh setup.sh > foo.out 2>&1 < /dev/null & "
done