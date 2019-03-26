#!/bin/bash

gsql_cmd="gsql -g ldbc_snb"
tmp_gsql=".tmp.gsql"

function get_install_str_body {
  is_first=1
  body=""
  for f in $1/*.gsql
  do
    cat $f >> $tmp_gsql
    echo "" >> $tmp_gsql

    fn=$(basename $f)
    if [ $is_first -eq "1" ]; then
      is_first=0
    else
      body="$body, "
    fi
    body="$body ${fn%.gsql}"
  done
  echo $body
}

# add bigint_to_string function and back up original ExprFunctions.hpp
echo "[STEP 1 ] Add an user-defined function."
gdk_path=$( sed -n "s/export[[:space:]]gdk=*//p" ~/.bash_tigergraph )
udf_file="$gdk_path/gsql/src/QueryUdf/ExprFunctions.hpp"
if [ ! -f $udf_file ]; then 
  echo "[ERROR  ] Can't add the user-defined function. Please make sure that you can run TigerGraph with the current user."
  exit 1
fi
cmp $udf_file "helper/ExprFunctions.hpp" >/dev/null 2>&1
if [ $? -ne 0 ]; then
  need_bak=1
  udf_bak=$( ls -1 $udf_file.* 2>/dev/null | sort -r | head -1 )
  if [ ! -z $udf_bak ] && [ -f $udf_bak ]; then
    cmp $udf_file $udf_bak >/dev/null 2>&1
    if [ $? -eq 0 ]; then
      need_bak=0
    fi
  fi
  if [ $need_bak -eq "1" ]; then
    echo "[INFO   ] Backing up existing ExprFunctions.hpp"
    cp $udf_file "$udf_file.$( date "+%Y-%m-%d_%H-%M-%S" )"
  fi
  cp helper/ExprFunctions.hpp $udf_file
  gadmin restart gsql -y
fi

# drop all queries
echo "[STEP 2 ] Drop all existing queries."
$gsql_cmd "DROP QUERY *"

# install helper queries
echo "[STEP 3 ] Install helper queries."
touch $tmp_gsql
> $tmp_gsql
install_str="INSTALL QUERY"
body_help=$( get_install_str_body "$PWD/helper" )
install_str="$install_str $body_help"
echo $install_str >> $tmp_gsql

$gsql_cmd $tmp_gsql

# install benchmark queries
echo "[STEP 4 ] Install LDBC SNB queries."
> $tmp_gsql
install_str="INSTALL QUERY"
body_is=$( get_install_str_body "$PWD/interactive_short" )
body_ic=$( get_install_str_body "$PWD/interactive_complex" )
body_bi=$( get_install_str_body "$PWD/business_intelligence" )
install_str="$install_str $body_is, $body_ic, $body_bi"
echo $install_str >> $tmp_gsql

$gsql_cmd $tmp_gsql

# remove .tmp.gsql
rm $tmp_gsql