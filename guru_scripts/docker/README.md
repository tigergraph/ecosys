Installing Docker Desktop
============================
- Intsall Docker for Mac OS laptop, follow this video

    https://www.youtube.com/watch?v=MU8HUVlJTEY

- Install Docker for Linux, follow this video

    https://www.youtube.com/watch?v=KCckWweNSrM

- Intsall Docker for Windows OS laptop, follow this video

    https://www.youtube.com/watch?v=ymlWt1MqURY

- Configure Docker Desktop using at least 2 cores, and 10G memory. Recommend 4 cores, 16G memory

  - Click Dock Desktop icon, click Preferences...>>Advanced menu, drag CPU and Memory slide to the desired configuration,
quit and restart docker desktop

Prepare shared data drive on your host with docker container
=============================================================
1. Open a shell on your host machine 

        cd ~/

1. Create the data folder to share between your host machine and docker container.

        mkdir data
        chmod 777 data

You can put files under ~/data, it will be visible in docker container under /home/tigergraph/tigergraph/loadingData, 
which is defined by **"-v ~/data:/home/tigergraph/tigergraph/loadingData"** in the docker command

Vice versa, anything you put under /home/tigergraph/tigergraph/loadingData within the container, 
will be visible on your host machchine ~/data folder. 

Since our dev edition does not support backup/restore data, you can persist your data (raw file, gsql script etc.) 
on the data volume. After upgrading Dev version, you can start a new container using the same data volume. 

Pull Pre-built TigerGraph Docker Image and Run it as a server
================================================================
One command pull docker image and bind all ports for first time user from TigerGraph  docker registry. 
This image will start as a daemon, so user can ssh to it. 

1. remove old version, only do this step if you upgrade your docker image

        docker rmi -f docker.tigergraph.com/tigergraph-dev:latest > /dev/null 2>&1
        docker pull docker.tigergraph.com/tigergraph-dev:latest
    > Note: replace "latest" with specific version number if a dedicated version of TigerGraph is to be used

1. stop and remove existing container only if an old version is being used

        docker ps -a | grep tigergraph_dev
        docker stop tigergraph_dev
        docker rm tigergraph_dev

1. pull the tigergraph docker image and run it as a daemon, change the ports accordingly if there is a conflict

        docker run -d -p 14022:22 -p 9000:9000 -p 14240:14240 --name tigergraph_dev --ulimit nofile=1000000:1000000 -v ~/data:/home/tigergraph/tigergraph/loadingData -t docker.tigergraph.com/tigergraph-dev:latest
    > Note: if you are using windows, change the above ~/data to something using windows file system convention, e.g. c:\data

1. verify that container is running

        docker ps | grep tigergraph_dev

1. Optional: stop/start container

        docker container stop tigergraph_dev
        docker container start tigergraph_dev

Content of the Docker Image
================================
In the dev image, we include 

- ssh server 
- git
- wget
- curl
- emac, vim etc. 
- jq
- tar
- tutorial: gsql 101, gsql 102 folder, tutorial folder
- latest gsql open source graph algorithm library: gsql-graph-algorithms folder


Start/shutdown Docker Desktop
===============================
- To shut down it, click you docker desktop, click "Quick Docker Desktop" will shutdown it. 
- To start it, find the Docker Desktop icon, double click it. 

Start/shutdown TigerGraph service
==================================
1. After you start Docker Desktop, use the below command to start the container created 

        docker start tigergraph_dev

1. ssh to the container, if localhost is not recognized, remove localhost entry from ~/.ssh/known_hosts

        sed -i.bak '/localhost/d' ~/.ssh/known_hosts
        ssh -p 14022 tigergraph@localhost
    > Linux users can access the container through its ip address directly

        docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' tigergraph_dev
        ssh tigergraph@<container_ip_address>

1. enter password for tigergraph user

        tigergraph

1. start tigergraph service

        gadmin start 

1. start graph studio
open a browser on your laptop and access:

        http://localhost:14240

1. check version

        gsql version
    > Use 2.4 and above

Documents and Forum
=====================
- Tutorial

    https://docs.tigergraph.com/intro/gsql-101

    https://docs.tigergraph.com/intro/gsql-102

- Forum
If you like the tutorial and want to explore more, join the GSQL developer community at 

    https://groups.google.com/a/opengsql.org/forum/?hl=en#!forum/gsql-users

- Videos to learn

    https://www.youtube.com/channel/UCnSOlBNWim68MYzcKbEswCA/videos

- ebook 

    https://info.tigergraph.com/ebook

- webinars 

    https://www.tigergraph.com/webinars-and-events/

