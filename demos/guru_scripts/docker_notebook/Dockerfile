FROM ubuntu:16.04

ENV DEV_VERSION 2.4.1
RUN useradd -ms /bin/bash tigergraph

RUN apt-get -qq update && apt-get install -y --no-install-recommends curl iproute2 net-tools cron ntp locales vim emacs wget git tar unzip jq uuid-runtime openssh-client openssh-server > /dev/null && \
  mkdir /var/run/sshd && \
  echo 'root:root' | chpasswd && \
  echo 'tigergraph:tigergraph' | chpasswd && \
  sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
  sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd && \
  curl -s -L http://dl.tigergraph.com/developer-edition/tigergraph-2.4.1-developer.tar.gz \
    -o /home/tigergraph/tigergraph-dev.tar.gz && \
  cd /home/tigergraph/ && \
  tar xfz tigergraph-dev.tar.gz && \
  rm -f tigergraph-dev.tar.gz && \
  cd /home/tigergraph/tigergraph-* && \
  ./install.sh -n || : && \
  rm -fR /home/tigergraph/tigergraph-* && \
  rm -fR /home/tigergraph/tigergraph/dev/deploy_pkg && \
  rm -f /home/tigergraph/tigergraph/gium_prod.tar.gz && \
  rm -f /home/tigergraph/tigergraph/pkg_pool/tigergraph_*.tar.gz

RUN echo "export VISIBLE=now" >> /etc/profile && \
  echo "export USER=tigergraph" >> /home/tigergraph/.bash_tigergraph && \
  rm -f /home/tigergraph/.gsql_fcgi/RESTPP.socket.1 && \
  touch /home/tigergraph/.gsql_fcgi/RESTPP.socket.1 && \
  chmod 644 /home/tigergraph/.gsql_fcgi/RESTPP.socket.1 && \
  chown -R tigergraph:tigergraph /home/tigergraph && \
  su - tigergraph -c "/home/tigergraph/.gium/gadmin config-apply"

USER tigergraph
WORKDIR /home/tigergraph

RUN wget --quiet --no-check-certificate https://repo.anaconda.com/archive/Anaconda3-2020.02-Linux-x86_64.sh
RUN /bin/bash Anaconda3-2020.02-Linux-x86_64.sh -f -b -p 
RUN rm Anaconda3-2020.02-Linux-x86_64.sh

# Copy local files as late as possible to avoid cache busting
COPY jupyter_notebook_config.py /home/tigergraph

USER root

EXPOSE 22
ENTRYPOINT /usr/sbin/sshd && su - tigergraph bash -c "script -q /dev/null"
