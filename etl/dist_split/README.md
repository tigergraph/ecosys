1. INTRODUCTION
This program runs under TigerGraph env. Assume the TigerGraph has n machine nodes, it will split a large file to n splits, and 
 shuffle each split to each node.
Note:
  - Make sure it's under TigerGraph env. E.g., try command gssh which should lists all the nodes in the cluster
  - Make sure local disk is large enough as it will double the current file
  - Better to run this in background such in a schreen or nohup session
  - Currently only support one file, and it's best to run this script under the directory of that file using relative path
  - After the program finishes, you may need to manually delete the local temp folder, called split_SOURCE_FILE
Usage: 
 - With 2 arguments, we duplicate original header line to each splits. 3 or more arguments, no duplication
  PATH/tg_dist_split filename target_dir
  e.g. generate #(machines) splits to local and remote /data/mysplit dir, each with a header line of abc.csv, 
       /tool_dir/tg_dist_split abc.csv /data/mysplit
 - With 3 arguements, each split doesn't duplcate the header 
  PATH/tg_dist_split filename target_dir 1
  e.g. generate #(machines) splits to local and remote /data/mysplit dir, each without header duplication, 
       /tool_dir/tg_dist_split abc.csv /data/mysplit 1
       
2. EXAMPLE
split file inv-2-plant.csv to /tmp/example/abc in a 3 machine cluster
$>> /tools/dist_split/tg_dist_split inv-2-plant.csv /tmp/example/abc
---------------------------------
-------- Setup ------------------
---------------------------------
Source file  :  inv-2-plant.csv
Target dir   :  /tmp/example/abc
# of machines:  3

---------------------------------
-------- Phase 1: split ---------
---------------------------------
[2018-08-27 20:31:28] Split with header prepended each split
created output folder split_inv-2-plant.csv
We split inv-2-plant.csv into 3 files under split_inv-2-plant.csv/
total 1208
-rw-rw-r--. 1 tigergraph tigergraph 409381 Aug 27 20:31 inv-2-plant.csv00
-rw-rw-r--. 1 tigergraph tigergraph 409601 Aug 27 20:31 inv-2-plant.csv01
-rw-rw-r--. 1 tigergraph tigergraph 409602 Aug 27 20:31 inv-2-plant.csv02
The header line below added to each splits:
Company ID|Company Name|Customer ID|Customer Name|Material ID|Site Code|Site Name|Storage Location|Storage Location Name|Stock Condition|Inventory Status|Currency Code|Inventory Date|Unit of Measure (UOM)|Quantity|Value

---------------------------------
-------- Phase 2 transer ---------
---------------------------------
[2018-08-27 20:31:28]  start transfer

### Connecting to local  server 192.168.55.38 ...
[2018-08-27 20:31:29] Done the transfer: split_inv-2-plant.csv/inv-2-plant.csv00 to m1

### Connecting to remote server 192.168.55.37 ...
[2018-08-27 20:31:29] Done the transfer: split_inv-2-plant.csv/inv-2-plant.csv01 to m2

### Connecting to remote server 192.168.55.34 ...
[2018-08-27 20:31:29] Done the transfer: split_inv-2-plant.csv/inv-2-plant.csv02 to m3


3. VERIFY
use "grun all" to list the target folder, 
$>>  grun all "ls -lh /tmp/example/abc"

### Connecting to local  server 192.168.55.38 ...
total 400K
-rw-rw-r--. 1 tigergraph tigergraph 400K Aug 27 20:36 inv-2-plant.csv00

### Connecting to remote server 192.168.55.37 ...
total 404K
-rw-rw-r--. 1 tigergraph tigergraph 401K Aug 27 20:36 inv-2-plant.csv01

### Connecting to remote server 192.168.55.34 ...
total 404K
-rw-rw-r--. 1 tigergraph tigergraph 401K Aug 27 20:36 inv-2-plant.csv02

4. CLEANUP
The progrom doesn't clean up the temp dir and you need to remove it manually
$>> rm -rf split_inv-2-plant.csv/ 
