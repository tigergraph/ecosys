# !/usr/bin/sh
# run `gcloud init` to log in before using this script
export sys=centOS
export nvm=40
gcloud compute config-ssh
cp setup_${sys}.sh setup_tmp.sh
echo "sudo pip3 install google-cloud-storage" >> setup_tmp.sh
  
for i in $(seq 1 $nvm)
do
  echo "setup m${i}"
  gcloud beta compute scp setup_tmp.sh m${i}:~ 
  gcloud beta compute ssh m${i} --command="nohup sh setup_tmp.sh > foo.out 2>&1 < /dev/null & "
done




