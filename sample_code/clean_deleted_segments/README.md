# INSTRUCTIONS:

CAUTION: Before using this script please run "gadmin reset -y" to clean up all buffered kafka messages.

PURPOSE: clean_deleted_segments.py is used to clean up (and purge) the dropped segments and one can also recover the previous operation of this script.

### 1) to clean up dropped segments
The script will detect the gstore position and read from gstore/0/part/config.yaml to obtain info about deleted vertex type ids, then it scans all segment folders to find the deleted type id, if that segment is deleted, this segment folder will be removed. If the segment folder is a soft link then the script will only remove the softlink. If the segment folder is not a soft link then the script will move the folder to folder `dest = /tmp/deleted_segments` and you need make sure 'dest' can hold all the deleted segments. If one want a different folder chang 'dest' at the beginning of the script.

``` python clean_deleted_segments.py ```

Once 1) is done, you should start the system and make sure it's OK. Then you may proceed with 2.2 to purge the deleted segments, or 2.1 to recover from the delete if system has any issue.
### 2.1) to recover deleted segments
one run the command with -r option then the script will recover the previous operation

```python clean_deleted_segments.py -r ```

### 2.2) to purge deleted segments
one run the command with -p:

```python clean_deleted_segments.py -p ```

==============================================================================

### To Run this in distributed TigerGraph system.

First of all, make sure that 'dest' folder has large enough space to hold the unused segments if the segments under gstore is not softlink.

Second, copy this script to each machine, for example, /tmp/ using scp or "gscp" command.

Then one could use the following command to clean up the useless segments on all machines (assuming the script is under /tmp folder), i.e.

```   grun all "python /tmp/clean_deleted_segments.py" ```
