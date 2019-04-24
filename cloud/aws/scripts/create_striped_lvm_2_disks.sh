sudo yum install -y lvm2* 

sudo lsblk
sudo pvdisplay

sudo pvcreate /dev/nvme[12]n1
sudo pvdisplay

sudo vgcreate ev-data-tg_vg /dev/nvme[12]n1
sudo vgdisplay

sudo lvcreate --yes --extents 100%FREE --stripes 2 --stripesize 256 -n ev-data ev-data-tg_vg
sudo lvdisplay
sudo lvs --segments
sudo lvdisplay -vm

sudo fdisk -l /dev/mapper/ev--data--tg_vg-ev--data 


sudo mkfs.ext4 /dev/ev-data-tg_vg/ev-data 


sudo mkdir /data
sudo mount /dev/mapper/ev--data--tg_vg-ev--data /data/
