#!/bin/bash
# The example: bash create_keytab.sh ~/kafka/private/keytab tiger123 kafka-0.tigergraph.com TIGERGRAPH.COM
# generate_root: keyTab files generation path
# pass: KDC database master key
# hostname: Kafka server hostname
# realm: everyone agrees to name realms in uppercase, such as "EXAMPLE.COM"

if [ $# -eq 4 ]; then
  generate_root=$1
  pass=$2
  hostname=$3
  realm=$4
else
  echo "shell script input check failed."
  exit 1
fi

# install Kerberos server in centos os
if [ -d "/etc/yum.repos.d" ]; then
  if ! rpm -qa | grep krb5-server > /dev/null 2>&1;then
    sudo yum install krb5-libs krb5-workstation -y > /dev/null 2>&1
    sudo yum install krb5-server -y > /dev/null 2>&1
  else
    echo "Kerberos has been installed."
  fi
else
  echo "unsupported os"
fi

config_file=/etc/krb5.conf
realm_lower=`echo $realm | awk '{print tolower($0)}'`
if [ ! -f ${config_file} ]; then
  echo "${config_file} not found."
  exit 1
else
  # From the default file, replace the realm EXAMPLE.COM with your realm.
  # Replace also the domain in lowercase with your domain in the [domain_realm] section.
  sudo su -c "echo \"includedir /etc/krb5.conf.d/

[logging]
 default = FILE:/var/log/krb5libs.log
 kdc = FILE:/var/log/krb5kdc.log
 admin_server = FILE:/var/log/kadmind.log

[libdefaults]
 dns_lookup_realm = false
 ticket_lifetime = 24h
 # renew_lifetime = 7d
 forwardable = true
 rdns = false
 pkinit_anchors = FILE:/etc/pki/tls/certs/ca-bundle.crt
 default_realm = ${realm}
 default_ccache_name = KEYRING:persistent:%{uid}

[realms]
 ${realm} = {
  kdc = ${hostname}
  admin_server = ${hostname}
 }

[domain_realm]
 .${realm_lower} = ${realm}
 ${realm_lower} = ${realm}
\" > ${config_file}"

  # delete old Kerberos database password
  cd /var/kerberos/krb5kdc
  sudo rm -rf principal*

  # Create the Kerberos database
  echo -e "${pass}\n${pass}" | sudo kdb5_util create -s -r ${realm}

  # generate new keytab files
  rm -rf ${generate_root}
  mkdir -p ${generate_root}

  # Start and enable Kerberos services as system services
  sudo systemctl start krb5kdc kadmin
  sudo systemctl enable krb5kdc kadmin

  cd $generate_root
  # Create principals and keytab files
  # create principals for Kafka clients
  sudo kadmin.local -q "addprinc -randkey kafka-client@${realm}"
  # create principals for Kafka broker.
  # "kafka" will be the service name.
  sudo kadmin.local -q "addprinc -randkey kafka/${hostname}@${realm}"
  # create kafka client keytab files.
  sudo kadmin.local -q "ktadd -k ${generate_root}/kafka-client.keytab kafka-client@${realm}"
  # create kafka server keytab files.
  sudo kadmin.local -q "ktadd -k ${generate_root}/kafka-servers.keytab kafka/${hostname}@${realm}"

  #check the keytab file
  keytab_num=$(sudo klist -e -k -t ${generate_root}/kafka-client.keytab  | grep -c "kafka-client@${realm}")
  if [ ${keytab_num} -gt 0 ]; then
    echo "keytab file create successfully."
  else
    echo "keytab file create failed."
    exit 1
  fi
fi