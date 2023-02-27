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

generate_root=${BASE_DIR}/SSL_OUTPUT
CN=kafka-0.tigergraph.com
storetype=jks
pass=tiger123
storepass=tiger123
storeName=""
CA=""
CAkey=""

CARoot_flag=""
subCA_flag=""
genKeystore_flag=""
genTruststore_flag=""
help_flag=""

opt_string="hip:c:s:o:n:"
opt_long_string="help,gen_CARoot,gen_subCA,gen_keystore,gen_truststore,passphrase:,output:,storepass:,storetype:,cer:,cerKey:,CN:,name:"
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
        --cer)
            CA=`path_conver $2`
            shift
            ;;
        --cerKey)
            CAkey=`path_conver $2`
            shift
            ;;
        -o|--output)
            generate_root=$2
            if [ ! -d ${generate_root} ]; then
              warn "The path '$generate_root' does not exist"
              prog "start creating output directory..."
              mkdir -p $generate_root
            fi
            generate_root=`path_conver $generate_root`
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
        -c|--CN)
            CN=$2
            shift
            ;;
        -s|--storetype)
            storetype=$2
            shift
            ;;
        -n|--name)
            storeName=$2
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
            generate_help
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
  else
    generate_help
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

  # generate root CA
  if [[ ! -z $CARoot_flag ]]; then
    prog "root-CA output directory: $generate_root"
    prog "root-CA CN: $CN"
    CA=${generate_root}/ca-root.crt
    CAkey=${generate_root}/ca-root.key

    check_file ${CA} 0
    check_file ${CAkey} 0
    generate_CARoot $generate_root $CN $pass
  fi

  # generate keystore
  if [[ ! -z $genKeystore_flag ]]; then
    if [[ -z $storeName ]]; then
      storeName=server.keystore
    fi
    prog "keystore output directory: $generate_root"
    prog "Keystore -Dname CN: $CN"
    prog "keystore name: $storeName"
    generate_keystore ${generate_root} ${pass} ${CN} ${storetype} ${storeName}
    keystore=${generate_root}/${storeName}
    prog "Generate keystore: $keystore"
    note "View keystore: keytool -list -v -keystore $keystore -storepass $pass"
  fi

  # generate a sub-certificate using the keytool
  if [[ ! -z $subCA_flag ]]; then
    prog "Subordinate-CA output directory: $generate_root"
    if [[ -z "$CA" || -z "$CAkey" ]]; then
      error "Missing options: '-cer' or '-cerKey', exiting..."
      general_usage gen_subCA
      exit 1
    fi

    check_cert $CA $CAkey $pass
    generate_sub_cert $generate_root $CA $CAkey $pass $CN
    prog "Generate subordinate-CA: ${CN}.crt successfully"
  fi

  # generate truststore
  if [[ ! -z ${genTruststore_flag:-} ]]; then
    if [[ -z $storeName ]]; then
      storeName=server.truststore
    fi
    truststore="${generate_root}/${storeName}"
    if [ ! -f "${truststore}" ]; then
      prog "Generate truststore: ${truststore}"
      generate_truststore "${generate_root}" "${storeName}" "${storepass}" "${storetype}"
    else
      warn "${truststore} already exists, skipping generation!"
    fi
    note "View truststore: keytool -list -v -keystore ${truststore} -storepass ${storepass}"
  fi

  # enter at least one command
  total_flag=($CARoot_flag $genKeystore_flag $subCA_flag $genTruststore_flag)
  if [[ -z $(IFS=,; echo "${total_flag[*]}") ]]; then
    error "Please enter at least one Command"
    generate_help
    exit 1
  fi
fi