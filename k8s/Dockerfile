FROM ubuntu:18.04

ENV DEBIAN_FRONTEND=noninteractive
ARG APP_VERSION=3.2.0
ARG APP_URL=https://dl.tigergraph.com/enterprise-edition/tigergraph-${APP_VERSION}-offline.tar.gz

RUN apt-get -qq update && apt-get install -y --no-install-recommends \
    sudo curl iproute2 net-tools iptables iptables-persistent \
    sshpass cron ntp locales vim tar jq uuid-runtime openssh-client openssh-server dnsutils iputils-ping > /dev/null && \
    apt-get autoremove && apt-get clean && \
    # Set up default account
    useradd -ms /bin/bash tigergraph && \
    mkdir /var/run/sshd && \
    echo 'tigergraph:tigergraph' | chpasswd && \
    sed -i 's/\#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd && \
    echo "tigergraph    ALL=(ALL)       NOPASSWD: ALL" >> /etc/sudoers && \
    /usr/sbin/sshd && \
    # Download installation packages
    curl -s -k -L ${APP_URL} -o /home/tigergraph/tigergraph-${APP_VERSION}-offline.tar.gz && \
    cd /home/tigergraph/ && \
    tar xfz tigergraph-${APP_VERSION}-offline.tar.gz && \
    rm -f tigergraph-${APP_VERSION}-offline.tar.gz && \
    # Install TigerGraph
    cd /home/tigergraph/tigergraph-* && \
    ./install.sh -n && \
    # Stop TigerGraph
    su - tigergraph -c "/home/tigergraph/tigergraph/app/${APP_VERSION}/cmd/gadmin stop all -y" && \
    # Clean Up unused packages
    rm -rf /home/tigergraph/tigergraph-* && \
    # Setup Enviroments setting
    echo "export USER=tigergraph" >> /home/tigergraph/.bash_tigergraph && \
    chown -R tigergraph:tigergraph /home/tigergraph

WORKDIR /home/tigergraph
USER tigergraph
EXPOSE 22 9000 14240
ENTRYPOINT sudo /usr/sbin/sshd && bash -c "tail -f /dev/null"
