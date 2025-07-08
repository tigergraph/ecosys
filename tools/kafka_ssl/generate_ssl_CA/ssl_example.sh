#! /bin/bash

cd $(dirname $0)
BASE_DIR=$(pwd)

broker_hostname=${1:-kafka-0.tigergraph.com}
client_hostname=${2:-tigergraph}
output_path=./SSL_OUTPUT

cleanup() {
if [ ! -z "${output_path}" -a -d ${output_path} ]; then
   rm -fr ${output_path}
fi
}

# cleanup
cleanup

## step1: Generate a Certificate Authority (CA) private_key/certificate, keystore and truststore
bash ssl_generate.sh

## step2: generate and sign Kafka broker private_key/certificate
bash ssl_generate.sh -gen_subCA -cer ${output_path}/ca-root.crt -cerKey ${output_path}/ca-root.key -c ${broker_hostname}

## step3: import CA key/certificate pairs to keystore
bash ssl_import.sh -import_to_keystore -keystore ${output_path}/server.keystore -cer ${output_path}/ca-root.crt -cerKey ${output_path}/ca-root.key

## step4: import Kafka broker private_key/certificate in keystore
bash ssl_import.sh -import_to_keystore -keystore ${output_path}/server.keystore -cer ${output_path}/${broker_hostname}.crt -cerKey ${output_path}/${broker_hostname}.key

## step5: generate and sign client private_key/certificate
bash ssl_generate.sh -gen_subCA -cer ${output_path}/ca-root.crt -cerKey ${output_path}/ca-root.key -c ${client_hostname}

## step6: import CA certificate in trustStore
bash ssl_import.sh -import_to_truststore -truststore ${output_path}/server.truststore -cer ${output_path}/ca-root.crt