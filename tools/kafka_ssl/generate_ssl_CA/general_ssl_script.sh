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
source_file utils/pretty_print "No miss tools found, utils/pretty_print NOT exist, exit" true
source_file utils/env_utils "No miss tools found, utils/env_utils NOT exist, exit" true
source_file utils/ssl_utils "No miss tools found, utils/ssl_utils NOT exist, exit" true

OSG=$(get_os)
OS=$(echo "$OSG" | cut -d' ' -f1)
version=$(echo "$OSG" | cut -d' ' -f2)
OSV="$OS$(echo "$version" | cut -d'.' -f1)"

generate_root=${BASE_DIR}/SSL_files
CN=kafka-0.tigergraph.com
storetype=jks
pass=tiger123
CARoot=""
CA_key=""
keystore=""
truststore=""
importCA=""

CARoot_flag=""
subCA_flag=""
genKeystore_flag=""
genTruststore_flag=""
importToKeystore_flag=""
importToTruststore_flag=""
help_flag=""

opt_string="hip:c:s:"
opt_long_string="help,gen_CARoot,gen_subCA,gen_keystore,gen_truststore,passphrase:,import:,import_to_keystore,import_to_truststore,storetype:,keystore:,truststore:,cer:,cerKey:,CN:"
ARGS=`getopt -a -o $opt_string --long $opt_long_string -- "$@"`

if [ $? != 0 ] ; then exit 1 ; fi
eval set -- "${ARGS}"
while :
do
    case $1 in
        -h|--help)
            help_flag=true
            ;;
        --gen_CARoot)
            CARoot_flag=true
            ;;
        --gen_subCA)
            subCA_flag=true
            ;;
        --gen_keystore)
            genKeystore_flag=true
            ;;
        --gen_truststore)
            genTruststore_flag=true
            ;;
        --import_to_keystore)
            importToKeystore_flag=true
            ;;
        --import_to_truststore)
            importToTruststore_flag=true
            ;;
        --cer)
            CARoot=`path_conver $2`
            shift
            ;;
        --cerKey)
            CA_key=`path_conver $2`
            shift
            ;;
        --keystore)
            keystore=`path_conver $2`
            shift
            ;;
        --truststore)
            truststore=`path_conver $2`
            shift
            ;;
        --import)
            importCA=`path_conver $2`
            shift
            ;;
        -d|--directory)
            generate_root=$2
            if [ ! -d ${generate_root} ]; then
              error "The path '$generate_root' does not exist"
              exit 1
            else
              generate_root=`path_conver $generate_root`/SSL_files
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
        -c|--CN)
            CN=$2
            shift
            ;;
        -s|--storetype)
            storetype=$2
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
            general_help
            ;;
    esac
shift
done

if [[ ! -z $help_flag ]]; then
  if [[ ! -z $CARoot_flag ]]; then
    general_usage gen_CARoot
  elif [[ ! -z $subCA_flag ]]; then
    general_usage gen_subCA
  elif [[ ! -z $genKeystore_flag ]]; then
    general_usage gen_keystore
  elif [[ ! -z $genTruststore_flag ]]; then
    general_usage gen_truststore
  elif [[ ! -z $importToKeystore_flag ]]; then
    general_usage import_to_keystore
  elif [[ ! -z $importToTruststore_flag ]]; then
    general_usage import_to_truststore
  else
    general_help
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

  if [[ ! -z $CARoot_flag ]]; then
    prog "root-CA generate directory: $generate_root"
    prog "root-CA subject CN: $CN"
    CARoot=${generate_root}/ca-root.crt
    CA_key=${generate_root}/ca-root.key

    check_file ${CARoot} 0
    check_file ${CA_key} 0
    generate_CARoot ${generate_root} $CN
  fi

  if [[ ! -z $genKeystore_flag ]]; then
    prog "keystore generate directory: $generate_root"
    prog "keystore -Dname CN: $CN"

    generate_keystore ${generate_root} ${pass} ${CN} ${storetype} "server"
    keystore=`ls -rt $(find ${generate_root} -type f -name "server.keystore*") | head -1`
    prog "generate keystore: $keystore"
  fi

  if [[ ! -z $subCA_flag ]]; then
    prog "subordinate-CA generate directory: $generate_root"
    if [ -z "${CARoot}" -o -z "${CA_key}" ]; then
       error "Missing options: '-cer' or '-cerKey', exiting..."
       general_usage gen_CARoot
       exit 1
    else
       check_CARoot ${CARoot} ${CA_key}
    fi

    if [ -z "${keystore}" ]; then
      generate_keystore ${generate_root} ${pass} ${CN} ${storetype} "server"
      keystore=`find ${generate_root} -type f -name "server.keystore*" | head -1`
    fi

    # generate sub-certificate
    generate_subCA ${generate_root} ${keystore} ${CARoot} ${CA_key} ${CN} ${pass}
    subCA=${CN}.crt

    prog "generate subordinate-CA: $subCA successfully"
  fi

  if [[ ! -z $genTruststore_flag ]]; then
    prog "truststore generate directory: $generate_root"
    generate_truststore ${generate_root} "server" ${pass} ${storetype}
    truststore=`ls -rt $(find ${generate_root} -type f -name "server.truststore*") | head -1`
    prog "generate truststore: $truststore"
  fi

  if [[ ! -z $importToKeystore_flag ]]; then
    if [ -z "${keystore}" -o -z "${importCA}" ]; then
      error "'-keystore' and '-import' are required options"
      general_usage import_to_keystore
      exit 1
    fi
    alias=${importCA##*/}
    alias=${alias%.*}
    prog "import alias is ${alias}"
    check_file ${keystore} 1
    check_file ${importCA} 1
    import_to_keystore ${keystore} ${importCA} ${alias} ${pass}
  fi

  if [[ ! -z $importToTruststore_flag ]]; then
    if [ -z "${truststore}" -o -z "${importCA}" ]; then
      error "'-truststore' and '-import' are required options"
      general_usage import_to_truststore
      exit 1
    fi
    alias=${importCA##*/}
    alias=${alias%.*}
    prog "import alias is ${alias}"
    check_file ${truststore} 1
    check_file ${importCA} 1
    import_to_truststore ${truststore} ${importCA} ${alias} ${pass}
  fi

  total_flag=($CARoot_flag $genKeystore_flag $subCA_flag $genTruststore_flag $importToKeystore_flag $importToTruststore_flag)
  if [[ -z $(IFS=,; echo "${total_flag[*]}") ]]; then
    error "Please enter at least one Command"
    general_help
    exit 1
  fi
fi
