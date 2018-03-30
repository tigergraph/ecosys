sudo mkfs -t ext4 /dev/xvdb
sudo mkdir /data
sudo mount /dev/xvdb /data
sudo cp /etc/fstab /etc/fstab.bak
sudo echo -e "/dev/xvdb\t/data\text4\tdefaults,nofail" >> /etc/fstab
sudo mount -a
sudo chmod -R 777 /data
