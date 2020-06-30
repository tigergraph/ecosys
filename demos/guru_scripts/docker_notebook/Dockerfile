FROM docker.tigergraph.com/tigergraph-dev:latest

USER tigergraph
WORKDIR /home/tigergraph

RUN wget --quiet --no-check-certificate https://repo.anaconda.com/archive/Anaconda3-2020.02-Linux-x86_64.sh
RUN /bin/bash Anaconda3-2020.02-Linux-x86_64.sh -f -b -p 
RUN rm Anaconda3-2020.02-Linux-x86_64.sh

COPY --chown=tigergraph gsql101.ipynb jupyter_notebook_config.py /home/tigergraph/

USER root

EXPOSE 22

ENTRYPOINT  su - tigergraph bash -c "export PATH='$PATH:/home/tigergraph/.gium/:/home/tigergraph/anaconda3/bin/' && gadmin start && jupyter notebook --NotebookApp.token='' && script -q /dev/null "
