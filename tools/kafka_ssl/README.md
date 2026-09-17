# SSL - certificates/keystore/truststore generate
# SSL - import key/certificate pairs into an existing keystore
# SSL - import certificate into an existing truststore

# help commands
1. bash ssl_generate.sh -h
2. bash ssl_import.sh -h

# help options
1. bash ssl_generate.sh -gen_keystore -h
2. bash ssl_import.sh -import_to_keystore -h

# Example
1. Only generate a Certificate Authority (CA) key and certificate
  - `bash ssl_generate.sh -gen_CARoot -c kafka-0.tigergraph.com -p 123456`
    - 'kafka-0.tigergraph.com' is the CN of Certificate Authority (CA)
    - '123456' is the passphrase of CA private_key
    

2. Only generate a keystore
  - `bash ssl_generate.sh -gen_keystore -c kafka-0.tigergraph.com -storepass 123456`
    - 'kafka-0.tigergraph.com' is the Subject CN of keystore
    - '123456' is the storepass of keystore


3. Only generate an empty truststore
  - `bash ssl_generate.sh -gen_truststore -storepass 123456`
    - '123456' is the storepass of truststore
    

4. At the same time, generate CARoot/CARoot private_key, keystore, and an empty truststore
  - `bash ssl_generate.sh -c kafka-0.tigergraph.com`
    - 'kafka-0.tigergraph.com' is the CN
    - The default passphrase of CA private_key is 'tiger123'
    - The default storepass of keystore and truststore is 'tiger123'

    
5. Sign sub-certificates with an existing certificate (CARoot or other Superior certificate)
  - `bash ssl_generate.sh -gen_subCA -cer ./SSL_OUTPUT/ca-root.crt -cerKey ./SSL_OUTPUT/ca-root.key -p 123456` -c tigergraph
    - './SSL_OUTPUT/ca-root.crt' is the path of higher-level CA
    - './SSL_OUTPUT/ca-root.key' is the path of higher-level CA private_key
    - '123456' is the passphrase of higher-level CA private_key
    - 'tigergraph' is the CN of your sub-certificate
    
    
6. Import key/certificate pairs into an existing keystore
  - `bash ssl_import.sh -import_to_keystore -keystore ./SSL_OUTPUT/server.keystore -cer ./SSL_OUTPUT/ca-root.crt -cerKey ./SSL_OUTPUT/ca-root.key -storepass 123456 -p tiger123`
    - './SSL_OUTPUT/server.keystore' is the path of your keystore
    - './SSL_OUTPUT/ca-root.crt' is the certificate path to be imported
    - './SSL_OUTPUT/ca-root.key' is the certificate private_key path to be imported
    - '123456' is the storepass of keystore
    - 'tiger123' is the passphrase of the certificate private_key
    
  
7. Import certificate into an existing truststore
  - `bash ssl_import.sh -import_to_truststore -truststore ./SSL_OUTPUT/server.truststore -cer ./SSL_OUTPUT/ca-root.crt -storepass 123456`
    - './SSL_OUTPUT/server.truststore' is the path of your truststore
    - './SSL_OUTPUT/ca-root.crt' is the certificate path to be imported
    - '123456' is the storepass of the truststore