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

opt_string=":ho:s:t:p:ic:"
incompatible_opt=""
while getopts $opt_string opt; do
  case $opt in
    h)
      help
      ;;
    s)
      server_hostname=$OPTARG
      ;;
    o)
      if [[ "$OPTARG" == "ssl_two_way" ]]; then
        ssl_type=$OPTARG
        two_way=true
      elif [[ "$OPTARG" == "ssl_one_way" ]]; then
        ssl_type=$OPTARG
        two_way=false
      else
        error "\"-o\" only supports \"ssl_one_way\" or \"ssl_two_way\""
        exit 1
      fi
      ;;
    t)
      generate_root=$OPTARG
      ;;
    c)
      client_hostname=$OPTARG
      incompatible_opt+=" -c"
      ;;
    p)
      pass=$OPTARG
      ;;
    i)
      SETUP_JDK=true
      ;;
    *)
      error "${bldred}Invalid option, the correct usage is described below: $txtrst"
      help
      ;;
  esac
done

if [[ "$two_way" == "true" ]] && [[ -z $incompatible_opt ]]; then
   echo "${bldred}Option '-o ssl_two_way' needs to be used together with option '-c', the correct usage is described below: $txtrst"
   help
fi

# install openJDK
# Using option '-i' will install openjdk-1.8.0, otherwise openjdk-1.8.0 will not be installed
install_openJDK
# install openssl
install_openssl
# ca generation
rm -rf $generate_root
if [[ "$ssl_type" == "ssl_one_way" ]]; then
  ssl_oneway_ca_generate
else
  ssl_twoway_ca_generate
fi