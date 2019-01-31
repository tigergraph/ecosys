#!/bin/bash

# replace headers
while read line; do
  IFS=' ' read -r -a array <<< $line
  filename=${array[0]}
  header=${array[1]}
  printf "Replacing header in %s... " ${filename}
  sed "1s/.*/$header/" ${LDBC_SNB_DATA_DIR}/${filename}${LDBC_SNB_DATA_POSTFIX} > tmp.csv && mv tmp.csv ${LDBC_SNB_DATA_DIR}/${filename}${LDBC_SNB_DATA_POSTFIX}
  echo "Done"
done < headers.txt

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
