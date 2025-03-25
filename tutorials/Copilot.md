# Introduction

TigerGraph CoPilot is an AI assistant that is meticulously designed to combine the powers of graph databases and generative AI to draw the most value from data and to enhance productivity across various business functions, including analytics, development, and administration tasks. It is one AI assistant with three core component services:
* InquiryAI as a natural language assistant for graph-powered solutions
* SupportAI as a knowledge Q&A assistant for documents and graphs
* QueryAI as a GSQL code generator including query and schema generation, data mapping, and more (Not available in Beta; coming soon)

This document provides instructions on how to use SupportAI.

# Content
This Copilot tutorial contains 
- [Setup Docker Environment](#setup-docker-environment)
- [Download Docker Images](#download-tigergraph-docker-images)
- [Deploy CoPilot Services](#deploy-copilot-with-docker-compose)
    
# Setup Environment 

### Setup Docker Environment

* Follow [Docker setup ](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up your docker Environment.
* Please follow (Overview of installing Docker Compose)[https://docs.docker.com/compose/install/] to install Docker Compose for your platform accordingly.

### Download Docker Images

#### TigerGraph Docker Image

To use TigerGraph Community Edition without a license key, download the corresponding docker image from https://dl.tigergraph.com/ and load to Docker:
> ```
> docker load -i ./tigergraph-4.2.0-alpha-community-docker-image.tar.gz
> docker images
> ```

You should be able to find `tigergraph/community:4.2.0-alpha` in the image list.

#### CoPilot Docker Images

The following images are also needed for TigerGraph CoPilot. It's not necessary to download them in advance as Docker Compose will download the automatically. But if it's desired, please use command `docker pull <image_name>` to download them:

```
tigergraphml/copilot:latest
tigergraphml/ecc:latest
tigergraphml/chat-history:latest
tigergraphml/copilot-ui:latest
nginx:latest
```

### Deploy Copilot with Docker Compose

* Step 1: Get docker-compose file
  - Download the [docker-compose.yml](./copilot/docker-compose.yml) file directly

  The Docker Compose file contains all dependencies for CoPilot including a TigerGraph database. If you want to use a separate TigerGraph instance, you can comment out the `tigergraph` section from the docker compose file and restart all services. However, please follow the instructions below to make sure your standalone TigerGraph server is accessible from other Copilot containers.

* Step 2: Set up configurations

  Next, download the following configuration files and put them in a `configs` subdirectory of the directory contains the Docker Compose file:
  * [configs/db_config.json](https://github.com/tigergraph/ecosys/blob/master/tutorials/copilot/configs/db_config.json)
  * [configs/llm_config.json](https://github.com/tigergraph/ecosys/blob/master/tutorials/copilot/configs/db_config.json)
  * [configs/chat_config.json](https://github.com/tigergraph/ecosys/blob/master/tutorials/copilot/configs/db_config.json)
  * [configs/nginx.config](https://github.com/tigergraph/ecosys/blob/master/tutorials/copilot/configs/nginx.config)

* Step 3: Adjust configurations
  Edit `configs/llm_config.json` and replace `<YOUR_OPENAI_API_KEY>` to your own OPENAI_API_KEY. 
  
  If desired, you can also change the model to be used for the embedding service and completion service to your preferred models to adjust the output from the LLM service.

* Step 4: Start all services

  Now, simply run `docker compose up -d` and wait for all the services to start.

[Go back to top](#top)

### Standalone TigerGraph instance (Optional)

> **_Note:_** Vector feature preview is available in both TigerGraph Community Edition (Alpha) and Enterprise Edition (Preview).

To start a TigerGraph Community Edition instance without a license key:
> ```
> docker run -d -p 14240:14240 --name tigergraph --ulimit nofile=1000000:1000000 --init --network copilot_default -t tigergraph/community:4.2.0-alpha
> ```

Check the service status with the following commands:
> ```
> docker exec -it tigergraph /bin/bash
> gadmin status
> gadmin start all
> ```

After using the database, and you want to shutdown it, use the following shell commmand
>```
>gadmin stop all
>```

[Go back to top](#top)


# Detailed Configurations

### DB configuration
Copy the below into `configs/db_config.json` and edit the `hostname` and `getToken` fields to match your database's configuration. If token authentication is enabled in TigerGraph, set `getToken` to `true`. Set the timeout, memory threshold, and thread limit parameters as desired to control how much of the database's resources are consumed when answering a question.

`embedding_store` selects the vector db to use, currently supports `tigergraph` and `milvus`. Set `reuse_embedding` to `true` will skip re-generating the embedding if it already exists.

`ecc` and `chat_history_api` are the addresses of internal components of CoPilot.If you use the Docker Compose file as is, you don’t need to change them.

```json
{
    "hostname": "http://tigergraph",
    "restppPort": "14240",
    "gsPort": "14240",
    "getToken": false,
    "default_timeout": 300,
    "default_mem_threshold": 5000,
    "default_thread_limit": 8,
    "embedding_store": "tigergraph",
    "reuse_embedding": true,
    "ecc": "http://eventual-consistency-service:8001",
    "chat_history_api": "http://chat-history:80
}
```

### LLM provider configuration
In the `configs/llm_config.json` file, copy JSON config template from below for your LLM provider, and fill out the appropriate fields. Only one provider is needed.

* OpenAI

In addition to the `OPENAI_API_KEY`, `llm_model` and `model_name` can be edited to match your specific configuration details.

```json
{
    "model_name": "GPT-4",
    "embedding_service": {
        "embedding_model_service": "openai",
        "authentication_configuration": {
            "OPENAI_API_KEY": "<YOUR_OPENAI_API_KEY>"
        }
    },
    "completion_service": {
        "llm_service": "openai",
        "llm_model": "gpt-4-0613",
        "authentication_configuration": {
            "OPENAI_API_KEY": "<YOUR_OPENAI_API_KEY>"
        },
        "model_kwargs": {
            "temperature": 0
        },
        "prompt_path": "./common/prompts/openai_gpt4/"
    }
}
```

### Chat configuration
Copy the below code into `configs/chat_config.json`. You shouldn’t need to change anything unless you change the port of the chat history service in the Docker Compose file.
```json
{
    "apiPort":"8002",
    "dbPath": "chats.db",
    "dbLogPath": "db.log",
    "logPath": "requestLogs.jsonl",
    "conversationAccessRoles": ["superuser", "globaldesigner"]
}
```

[Go back to top](#top)
