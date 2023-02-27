#!/bin/bash

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
source_file utils/pretty_print "File utils/pretty_print NOT found, exit" true
source_file utils/env_utils "File utils/env_utils NOT found, exit" true
source_file utils/ssl_utils "File utils/ssl_utils NOT found, exit" true

OSG=$(get_os)
OS=$(echo "$OSG" | cut -d' ' -f1)
version=$(echo "$OSG" | cut -d' ' -f2)
OSV="$OS$(echo "$version" | cut -d'.' -f1)"

storepass=tiger123
CA=""
CAkey=""
pass=tiger123
keystore=""
truststore=""

importToKeystore_flag=""
importToTruststore_flag=""
help_flag=""

opt_string="hip:"
opt_long_string="help,passphrase:,import_to_keystore,storepass:,import_to_truststore,keystore:,truststore:,cer:,cerKey:"
ARGS=`getopt -a -o $opt_string --long $opt_long_string -- "$@"`

if [ $? != 0 ] ; then exit 1 ; fi
eval set -- "${ARGS}"
while :
do
    case $1 in
        -h|--help)
            help_flag=true
            ;;
        --import_to_keystore)
            importToKeystore_flag=true
            ;;
        --import_to_truststore)
            importToTruststore_flag=true
            ;;
        --cer)
            CA=`path_conver $2`
            if [ $? -ne 0 ]; then
              error "$CA"
              exit 1
            fi
            shift
            ;;
        --cerKey)
            CAkey=`path_conver $2`
            if [ $? -ne 0 ]; then
              error "$CAkey"
              exit 1
            fi
            shift
            ;;
        --keystore)
            keystore=`path_conver $2`
            if [ $? -ne 0 ]; then
              error "$keystore"
              exit 1
            fi
            shift
            ;;
        --truststore)
            truststore=`path_conver $2`
            if [ $? -ne 0 ]; then
              error "$truststore"
              exit 1
            fi
            shift
            ;;
        -p|--passphrase)
            pass=$2
            if [ ${#pass} -lt 6 ];then
              error "Password is too short - must be at least 6 characters."
              exit 1
            fi
            shift
            ;;
        --storepass)
            storepass=$2
            shift
            ;;
        -i|--install)
            SETUP_JDK=true
            SETUP_OPENSSL=true
            ;;
        --)
            shift
            break
            ;;
        *)
            error "${bldred}Invalid option, the correct usage is described below: $txtrst"
            import_help
            ;;
    esac
shift
done

if [[ ! -z $help_flag ]]; then
  if [[ ! -z $importToKeystore_flag ]]; then
    general_usage import_to_keystore
  elif [[ ! -z $importToTruststore_flag ]]; then
    general_usage import_to_truststore
  else
    import_help
  fi
  exit 0
else
  # this script only support rhel/centos
  prog "Checking operation system (OS) version ..."
  check_os $OS $version

  prog "Checking root/sudo ..."
  check_root

  # Using option '-i/--install' will install openjdk-1.8.0 and openssl,
  # otherwise openjdk-1.8.0 and openssl will not be installed
  # install openJDK
  install_openJDK
  # install openssl
  install_openssl

  # import key-cert pair to keystore
  if [[ ! -z $importToKeystore_flag ]]; then
      [[ -z "$CA" || -z "$CAkey" || -z "$keystore" ]] \
          && { error "'-keystore', '-cer' and '-cerKey' are required options"; general_usage import_to_keystore; exit 1; }
      alias=${CA##*/}
      alias=${alias%.*}
      prog "Import alias is ${alias}"
      check_file ${keystore} 1
      check_file ${CA} 1
      check_file ${CAkey} 1
      import_to_keystore ${keystore} ${alias} ${CAkey} ${CA} ${storepass} ${pass}
  fi

  # import certificate to truststore
  if [[ ! -z $importToTruststore_flag ]]; then
      [[ -z "$CA" || -z "$truststore" ]] \
          && { error "'-truststore' and '-cer' are required options"; general_usage import_to_truststore; exit 1; }
      alias=${CA##*/}
      alias=${alias%.*}
      prog "Import alias is ${alias}"
      check_file ${truststore} 1
      check_file ${CA} 1
      import_to_truststore ${truststore} ${CA} ${alias} ${storepass}
  fi

  # enter at least one command
  total_flag=($importToKeystore_flag $importToTruststore_flag)
  if [[ -z $(IFS=,; echo "${total_flag[*]}") ]]; then
    error "Please enter at least one Command"
    import_help
    exit 1
  fi
fi