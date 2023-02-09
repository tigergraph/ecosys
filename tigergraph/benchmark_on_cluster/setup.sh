# !/usr/bin/sh
sudo useradd -ms /bin/bash tigergraph
echo 'tigergraph:100tbenchmark390170243trumantigergraph' | sudo chpasswd # The second tigergraph is the default password, please change it  
mkdir -p /home/tigergraph
sudo bash -c 'echo "tigergraph    ALL=(ALL)       NOPASSWD: ALL" >> /etc/sudoers'
sudo bash -c 'echo "export VISIBLE=now" >> /etc/profile'
sudo bash -c 'echo "export USER=tigergraph" >> /home/tigergraph/.bash_tigergraph'

sudo sed -i 's/\#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config
sudo sed -i 's/\#PubkeyAuthentication yes/PubkeyAuthentication yes/' /etc/ssh/sshd_config
sudo sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/' /etc/ssh/sshd_config
sudo service sshd reload

if command -v apt >/dev/null; then
  installer=apt
elif command -v yum >/dev/null; then
  installer=yum
else
  echo "Require apt or yum"
  exit 0
fi
sudo $installer -y update
sudo $installer -y install wget git
sudo $installer -y install net-tools sshpass parallel git gzip python3-pip
# if sshpass package is not available.
if command -v sshpass >/dev/null; then
echo "sshpass installed"
elif command -v rpm >/dev/null; then
# install sshpass on rpm-based system
wget https://archives.fedoraproject.org/pub/archive/epel/6/x86_64/epel-release-6-8.noarch.rpm
sudo rpm -ivh epel-release-6-8.noarch.rpm
sudo yum --enablerepo=epel -y install sshpass
else
# install sshpass on deb-based system
sudo apt install sshpass
fi
# if parallel package is not available
if ! command -v parallel > /dev/null; then
  sudo $installer -y install bzip2 
  wget http://ftp.gnu.org/gnu/parallel/parallel-latest.tar.bz2
  tar xjf parallel-latest.tar.bz2
  cd parallel-20*
  ./configure && make
  sudo make install
fi

sudo python3 -m pip install --upgrade pip
sudo pip3 install google-cloud-storage
sudo pip3 install paramiko scp
echo 'done setup'
