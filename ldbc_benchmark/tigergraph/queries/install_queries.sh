#!/bin/bash

gsql_cmd='gsql -g ldbc_snb'

function get_install_str_body {
  is_first=1
  body=""
  for f in $1/*.gsql
  do
    cat $f >> tmp.gsql
    echo "" >> tmp.gsql

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


# install helper queries first
touch tmp.gsql
echo 'DROP QUERY ALL' > tmp.gsql
install_str='INSTALL QUERY'
body_help=$( get_install_str_body "$PWD/helper" )
install_str="$install_str $body_help"
echo $install_str >> tmp.gsql

$gsql_cmd tmp.gsql

# install benchmark queries
> tmp.gsql
install_str='INSTALL QUERY'
body_is=$( get_install_str_body "$PWD/interactive_short" )
body_ic=$( get_install_str_body "$PWD/interactive_complex" )
body_bi=$( get_install_str_body "$PWD/business_intelligence" )
install_str="$install_str $body_is, $body_ic, $body_bi"
echo $install_str >> tmp.gsql

$gsql_cmd tmp.gsql

# remove tmp.gsql
rm tmp.gsql