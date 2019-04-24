sudo umount /data/
sudo lvremove -y /dev/ev-data-tg_vg/ev-data
sudo vgdisplay
sudo vgremove -y ev-data-tg_vg
sudo pvdisplay 
sudo pvremove -y /dev/sdf /dev/sdg
