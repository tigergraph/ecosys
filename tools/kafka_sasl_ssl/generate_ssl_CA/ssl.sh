#!/bin/bash
# ssl_one_way ca files generation
# ssl_two_way ca files generation

cd $(dirname $0)
BASE_DIR=$(pwd)

source_file() {
  file=$1
  msg="$2"
  src_flag=$3
  if [ -f "$file" ]; then
    if [ "$src_flag" != "false" ]; then
      source $file
    fi
  else
    echo $(tput setaf 1) "[ERROR   ]: $msg" $(tput sgr0)
    exit 1
  fi
}

# source all functions
source_file utils/pretty_print "No miss tools found, utils/pretty_print NOT exist, exit" true
source_file utils/env_utils "No miss tools found, utils/env_utils NOT exist, exit" true

OSG=$(get_os)
OS=$(echo "$OSG" | cut -d' ' -f1)
version=$(echo "$OSG" | cut -d' ' -f2)
OSV="$OS$(echo "$version" | cut -d'.' -f1)"

# this script only support rhel/centos
prog "Checking operation system (OS) version ..."
check_os $OS $version

prog "Checking root/sudo ..."
check_root

server_hostname=kafka-0.tigergraph.com
generate_root=~/SSL_files
pass=tiger123
ssl_type=ssl_one_way
client_hostname=

opt_string=":ht:s:d:p:ic:"
incompatible_opt=""
while getopts $opt_string opt; do
  case $opt in
    h)
      help
      ;;
    s)
      server_hostname=$OPTARG
      ;;
    t)
      if [[ "$OPTARG" == "ssl_two_way" ]]; then
        ssl_type=$OPTARG
        two_way=true
      elif [[ "$OPTARG" == "ssl_one_way" ]]; then
        ssl_type=$OPTARG
        two_way=false
      else
        error "\"-t\" only supports \"ssl_one_way\" or \"ssl_two_way\""
        exit 1
      fi
      ;;
    d)
      generate_root=$OPTARG
      ;;
    c)
      client_hostname=$OPTARG
      incompatible_opt+=" -c"
      ;;
    p)
      if [ ${#OPTARG} -lt 6 ];then
        error "Password is too short - must be at least 6 characters."
        exit 1
      else
      	pass=$OPTARG
      fi
      ;;
    i)
      SETUP_JDK=true
      SETUP_OPENSSL=true
      ;;
    *)
      error "${bldred}Invalid option, the correct usage is described below: $txtrst"
      help
      ;;
  esac
done

if [[ "$two_way" == "true" ]] && [[ -z $incompatible_opt ]]; then
   echo "${bldred}Option '-t ssl_two_way' needs to be used together with option '-c', the correct usage is described below: $txtrst"
   help
fi

# Using option '-i' will install openjdk-1.8.0 and openssl, otherwise openjdk-1.8.0 and openssl will not be installed
# install openJDK
install_openJDK
# install openssl
install_openssl
# ca generation
if [[ "$ssl_type" == "ssl_one_way" ]]; then
  generate_root=$generate_root/ssl_one_way
  rm -rf $generate_root
  ssl_oneway_ca_generate
else
  generate_root=$generate_root/ssl_two_way
  rm -rf $generate_root
  ssl_twoway_ca_generate
fi