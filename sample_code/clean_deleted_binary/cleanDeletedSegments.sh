#!/bin/bash
echo -en "This script needs to \e[31mstop gpe\e[0m, do you want continue [yes|NO]: "
read action
if [ ! "$action" = "yes" ]; then
  echo "'"$action"' entered, exit"
  exit
fi
~/.gium/gadmin stop gpe -y
ts=$(date "+%s")

# setup the running mode
recover=false
if [ $# -gt 0 ];
then
  if [ ! $# -eq 2 ]; then
  echo "cleanDeleteSegments only takes two arguments as ' -r \$timestamp ' to recover a previous run, but "$#" arguments are provided."
  exit
  elif [ ! $1 = "-r" ]; then
  echo "cleanDeleteSegments only takes ' -r \$timestamp ' option to recover a previous run"
  exit
  else
    recover=true
    ts=$2
    Mode="Running in RECOVER mode with ts = "$ts
  fi
else
  Mode="Running in DELETE mode with ts = "$ts
fi
echo $Mode
#create the script file
file=./cleanScript.sh

echo "
#!/bin/bash
CleanUp() {
  mv vertex.bin vertex.bin.bak.$ts
  head -c 1 < /dev/zero > vertex.bin
  mv vertexsize.bin vertexsize.bin.bak.$ts
  head -c \$NumOfDeletedVertices < /dev/zero > vertexsize.bin
}
Recover() {
  mv vertex.bin.bak.$ts vertex.bin 
  mv vertexsize.bin.bak.$ts vertexsize.bin
}" > $file

echo '
gstore=$(cat ~/.gsql/gsql.cfg|grep tigergraph.storage|cut -d" " -f2)"/0/part"
num=0
for i in $(ls -d $gstore/*/);
do 
  NumOfDeletedVertices=$(grep -E "NumOfVertices|NumOfDeletedVertices" "$i"segmentconfig.yaml | cut -d" " -f2 |uniq)
  if [ $(echo $NumOfDeletedVertices|awk "{print NF}") -eq 1 ];
  then
    num=$((num + 1))
    echo "  Found deleted segment: "$i" with "$NumOfDeletedVertices
    #grep -E "VertexTypeId|NumOfVertices|NumOfDeletedVertices" "$i"segmentconfig.yaml
    cd $i
' >> $file

if [ "$recover" = false ]; then
      
   echo '    CleanUp' >> $file
else
   echo '    Recover' >> $file
fi

echo '
    cd - > /dev/null
  fi
done
if [ "$num" -eq 0 ]; then
  echo -e "\e[32mNo deleted segment has been found, do nothing!\e[0m"
fi' >> $file

gscp all $file "/tmp/"
grun all "bash /tmp/$file"
grun all "rm /tmp/$file" > /dev/null
echo $Mode
#rm $file
~/.gium/gadmin start gpe

