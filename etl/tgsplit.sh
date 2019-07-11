#################################################
#provided as it is. Use at your own risk.
################################################

#!/bin/bash

if [ $# -eq 0 ]; then
  echo "Usage: "
  echo " "
  echo "With 2 arguments, we duplicate original header line to each splits. 3 or more arguments, no duplication"
  echo " "
  echo "./tgsplit filename split_count" 
  echo "e.g. to generate 3 splits, each with a header line of abc.csv,  ./tgsplit abc.csv 3" 
  echo "OR"
  echo "./tgsplit filename split_count 1"
  echo "e.g. to generate 3 splits, each split 1/3 size of abc.csv without header duplication, ./tgsplit abc.csv 3 1" 
  exit 0
fi


#check file existence
if [ ! -f $1 ]; then
  echo "$1 not found."
  exit 2
fi

#split file, each split has suffix index.
splitfolder="split_${1}"
#echo "$splitfolder"

#clear existing one, and create a new output folder
rm -fr $splitfolder; mkdir $splitfolder
echo "created output folder $splitfolder"

#split file $2 is the number of splits, $1 is the source file name.
split -dn $2 $1 $splitfolder/$1

#add header line
first_split_suffix='00'

#loop through all files prefixed $1: original file + generated splits
for i in `find $splitfolder/$1*`
do
  #skip first splits, which has header already
  if [ "$i" = "$splitfolder/$1$first_split_suffix" ]; then
    :
  elif [ $# -eq 2 ]; then  #add header to the rest of splits
    head -n 1 $1 > $splitfolder/tmp_file
    cat $i >> $splitfolder/tmp_file
    mv -f $splitfolder/tmp_file $i
  else # more than three arugments, do not add header to splits
    :
  fi;
done

echo "We split $1 into $2 files under $splitfolder/"

ls -lrt  $splitfolder 

header=`head -n 1 $1`

if [ $# -eq 2 ]; then 
  echo "The header line below added to each splits:"
  echo "$header" 
else 
  echo "NO header line added to each splits"
fi


