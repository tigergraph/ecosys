# !/usr/bin/sh
# prepare the disk
if [ ! -d "/home/tigergraph" ]
then
  sudo mkfs -t xfs /dev/xvdb 
  sudo mkdir -p /home/tigergraph 
  sudo mount /dev/xvdb /home/tigergraph/
  sudo chown -R tigergraph:tigergraph /home/tigergraph/
fi

sudo useradd -ms /bin/bash tigergraph
echo 'tigergraph:tigergraph' | sudo chpasswd
sudo bash -c 'echo "tigergraph    ALL=(ALL)       NOPASSWD: ALL" >> /etc/sudoers'
sudo bash -c 'echo "export VISIBLE=now" >> /etc/profile'
sudo bash -c 'echo "export USER=tigergraph" >> /home/tigergraph/.bash_tigergraph'

sudo sed -i 's/\#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config
sudo sed -i 's/\#PubkeyAuthentication yes/PubkeyAuthentication yes/' /etc/ssh/sshd_config
sudo sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config
sudo service sshd reload

sudo apt-get update
sudo apt-get -y install python3-pip net-tools sshpass parallel awscli 
sudo pip3 install --upgrade awscli
sudo pip3 install boto3