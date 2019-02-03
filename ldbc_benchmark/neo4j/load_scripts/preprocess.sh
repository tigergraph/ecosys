#!/bin/bash

# remove the first line of each csv file
files=$(find ${LDBC_SNB_DATA_DIR} -name '*.csv')
for file in $files
do
  printf "Removing header in %s... " ${file}
  tail -n +2 $file > $file.tmp && mv -f $file.tmp $file
  # sed '1d' $file > $file.tmp && mv -f $file.tmp $file
  echo "Done"
done

# add header files
printf "Creating headers... "
while read line; do
  IFS=' ' read -r -a array <<< $line
  filename=${array[0]}
  header=${array[1]}
  echo "${header}" > ${LDBC_SNB_DATA_DIR}/${filename}_header.csv
done < headers.txt
echo "Done"

# replace labels with one starting with an uppercase letter
printf "Replacing labels... "
sed -i "s/|city$/|City/" "${LDBC_SNB_DATA_DIR}/place${LDBC_SNB_DATA_POSTFIX}"
sed -i "s/|country$/|Country/" "${LDBC_SNB_DATA_DIR}/place${LDBC_SNB_DATA_POSTFIX}"
sed -i "s/|continent$/|Continent/" "${LDBC_SNB_DATA_DIR}/place${LDBC_SNB_DATA_POSTFIX}"
sed -i "s/|company|/|Company|/" "${LDBC_SNB_DATA_DIR}/organisation${LDBC_SNB_DATA_POSTFIX}"
sed -i "s/|university|/|University|/" "${LDBC_SNB_DATA_DIR}/organisation${LDBC_SNB_DATA_POSTFIX}"
echo "Done"

# convert each date of format yyyy-mm-dd to a number of format yyyymmddd
# find ${LDBC_SNB_DATA_DIR} -name 'person_[0-9]*.csv' -exec sed -i "s#|\([0-9][0-9][0-9][0-9]\)-\([0-9][0-9]\)-\([0-9][0-9]\)|#|\1\2\3|#g" {} \;

# convert each datetime of format yyyy-mm-ddThh:mm:ss.mmm+0000
# to a number of format yyyymmddhhmmssmmm
# find ${LDBC_SNB_DATA_DIR} -name *.csv -exec sed -i "s#|\([0-9][0-9][0-9][0-9]\)-\([0-9][0-9]\)-\([0-9][0-9]\)T\([0-9][0-9]\):\([0-9][0-9]\):\([0-9][0-9]\)\.\([0-9][0-9][0-9]\)+0000#|\1\2\3\4\5\6\7#g" \;
