INSTRUCTIONS:
 clean_deleted_segments.py is used clean up the dropped segments and one can also recover the previous operation of this script 

1) to clean up dropped segments: The script will detect the gstore position and read from gstore/0/part/config.yaml to obtain info about deleted vertex type ids, then it scan all segment folders to find the deleted type id, if that segment is deleted, this segment folder will be removed. If the segment folder is a soft link then the script will only remove the softlink. If the segment folder is not a soft link then the script will move the folder to folder dest = /tmp/deleted_segments and you need make sure 'dest' can hold all the deleted segments. If one want a different folder chang 'dest' at the beginning of the script.
        python clean_deleted_segments.py

2) to recover deleted segments: if one run the command with -r option then the script will recover the previous operation
        python clean_deleted_segments.py -r
