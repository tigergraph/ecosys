Overview 
=================
This document provides step-by-step instructions on how to pull the latest TigerGraph Developer Edition docker image to your host machine. You can follow the sections in sequence to setup the TigerGraph docker enviroment. 

The latest TigerGraph docker image includes the following content.  

- ssh server 
- git
- wget
- curl
- emac, vim etc. 
- jq
- tar
- tutorial: gsql 101, gsql 102 sub folders.
- latest gsql open source graph algorithm library: gsql-graph-algorithms folder

This video shows the whole setup process. https://www.youtube.com/watch?v=V5VvgJyjLxA

If you want to customize your own docker image, the last section of this README has instructions on how to accomplish it. 

Install Docker Desktop
=========================
1. Install Docker on your OS (choose one)
   - To install Docker for Mac OS, follow this video
     https://www.youtube.com/watch?v=MU8HUVlJTEY

   - To install Docker for Linux, follow this instructions.
     - Centos https://docs.docker.com/install/linux/docker-ce/centos/
     - Ubuntu https://docs.docker.com/install/linux/docker-ce/ubuntu/

   - To install Docker for Windows OS, follow this video
     https://www.youtube.com/watch?v=ymlWt1MqURY

2. Configure Docker Desktop with sufficient resources:
    Recommended: 4 cores and 16GB memory
    Minimum: 2 cores and 10GB memory (performance will be degraded)

    Click the Docker Desktop icon, click Preferences...>>Advanced menu, drag the CPU and Memory sliders
    to the desired configuration, save and restart Docker Desktop

3. To understand the Docker *Container* and *Image* concepts, watch this video:
  https://www.youtube.com/watch?v=Rv3DAJbDrS0

Prepare a Shared Folder on Host OS shared with Docker Container
===============================================================
Open a shell on your host machine and create or select a directory for sharing data between your host machine and docker container. Grant read/write/execute permission to the folder. For example, to create a folder called data in Linux:

        mkdir data
        chmod 777 data

You can mount(map) the data folder to a folder under the docker container (will show -v of the mount command later). 
Then, you can share files between your host OS and Docker OS. 

Suppose we mount the host OS ~/data folder to a docker folder /home/tigergraph/mydata, then anything we put on ~/data will be visible in docker container under /home/tigergraph/mydata, and vice versa.  

Since our dev edition does not support backup/restore data, you can persist your data (raw file, gsql script etc.) 
on the data volume. After upgrading Dev version, you can start a new container using the same data volume. 

Pull Pre-built TigerGraph Docker Image And Run It As A Server
================================================================
One command pull docker image and bind all ports for first time user from the TigerGraph docker registry. 
This image will start as a daemon, so user can ssh to it. 

1. pull the latesst version, only do this step in shell if you upgrade your docker image

        docker pull tigergraph/tigergraph:latest
        
    > Note: replace "latest" with specific version number if a dedicated version of TigerGraph is to be used. E.g. If you want to get 3.6.0 version, the following would be the URL. 
     
     docker pull tigergraph/tigergraph:3.6.0
     
    > Note: 
    > * to use the legacy versions, use docker.tigergraph.com/tigergraph:<version> 
    > * to use the legacy developer edition images, use docker.tigergraph.com/tigergraph-dev:latest or docker.tigergraph.com/tigergraph-dev:<version> instead

1. stop and remove existing container in shell only if an old version is being used

        docker ps -a | grep tigergraph
        docker stop tigergraph
        docker rm tigergraph

1. run the tigergraph docker image as a daemon, change the ports accordingly if there is a conflict. 
   - the command is very long, user need to drag the horizontal scroll bar to the right to see the full command. 
   - The command does the following
     - "-d" make the container run in the background. 
     - "-p" map docker 22 port to host OS 14022 port, 9000 port to host OS 9000 port, 14240 port to host OS 14240 port.
     - "--name" name the container  tigergraph. 
     - "--ulimit" set the ulimit (the number of open file descriptors per process) to 1 million.
     - "-v" mount the host OS ~/data folder to the docker /home/tigergraph/mydata folder using the -v option. Note that if you are using windows, change the above ~/data to something using windows file system convention, e.g. c:\data
     - download the "latest" docker image from the TigerGraph docker registry url tigergraph/tigergraph. 
```bash
   docker run -d -p 14022:22 -p 9000:9000 -p 14240:14240 --name tigergraph --ulimit nofile=1000000:1000000 -v ~/data:/home/tigergraph/mydata -t tigergraph/tigergraph:latest
```      
Note that if you use Windows, and have disk drive permission issue with the above command, please try the following
```bash
   docker run -d -p 14022:22 -p 9000:9000 -p 14240:14240 --name tigergraph --ulimit nofile=1000000:1000000 -t tigergraph/tigergraph:latest
```   

After pulling the image and launch the container in the background, you can try the following to verify it's running. 
1. verify that container is running, you should see a row to describe the running container.

        docker ps | grep tigergraph
        
1. open a shell on your host OS to ssh to the container. At prompt, enter "tigergraph" without quotes as password. Note that we have mapped the host 14022 port to the container's 22 port (the ssh default port). So, on host we ssh to 14022. 
         
         ssh -p 14022 tigergraph@localhost
         Enter password tigergraph when prompted.
1. after ssh to the container, start the TigerGraph service under bash shell (may take up to 1 minute). 

         gadmin start all

1. start gsql shell under bash shell, and you are ready to follow https://docs.tigergraph.com/start/gsql-101

         gsql 
1. start GraphStudio, TigerGraph's visual IDE,  by opening a browser on your laptop (host OS) and access:

        http://localhost:14240
        
Operation Commands Cheat Sheet
================================

- After you start Docker Desktop, use the below command to start/stop the container created 
    
        docker container stop tigergraph
        docker container start tigergraph
        
- start/stop tigergraph service within container

        gadmin start all
        gadmin stop  all
        
- Start/Stop Docker Desktop
  - To shut down it, click you docker desktop, click "Quick Docker Desktop" will shutdown it. Before you stop Docker Desktop, be sure to execute the above two steps in reverse order. (1) stop tigergraph service within container. (2) stop tigergraph container.
  - To start it, find the Docker Desktop icon, double click it. 

- ssh to the container, if localhost is not recognized, remove localhost entry from ~/.ssh/known_hosts

        sed -i.bak '/localhost/d' ~/.ssh/known_hosts
        ssh -p 14022 tigergraph@localhost
    > Linux users can access the container through its ip address directly

        docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' tigergraph
        ssh tigergraph@<container_ip_address>

- enter password for tigergraph user

        tigergraph

- After "gadmin start", you can start graph studio of TigerGraph. Open a browser on your laptop (host OS) and access:

        http://localhost:14240

-  Check GSQL version within container shell. 

        gsql version

Documents and Forum
=====================
- Tutorial

    https://docs.tigergraph.com/start/gsql-101

    https://docs.tigergraph.com/start/gsql-102

- Community
If you like the tutorial and want to explore more, join the GSQL developer community at 

  https://community.tigergraph.com/

- Videos to learn

    https://www.youtube.com/channel/UCnSOlBNWim68MYzcKbEswCA/videos

- ebook 

    https://info.tigergraph.com/ebook

- webinars 

    https://www.tigergraph.com/webinars-and-events/
    
    
Build your own Docker Image
================================
To customize the Tigergraph Docker image, e.g., integrate another docker images
1. Create a tigergraph folder
1. Download the [dockerfile](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/dockerfile) shared in this repo to the folder
1. Revise the docker file to add more tools, or integrate with another dockerfile
1. Run the following command:

```
        cd tigergraph
        docker build -t tigergraph .
```

- Please note that you may need to change the URL of the TigerGraph developer package to reflect the version you need.

