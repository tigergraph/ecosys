# Hybrid Search Demo

This repository demonstrates Hybrid Search, combining graph-based and vector-based search, for recommendation and QA systems.

---

## 1. Set Up the Environment 

If you're using your own machine (Windows, macOS, or Linux), the easiest way to run TigerGraph is via Docker.

Follow these two steps in the [Docker setup instructions](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up the environment:

- Install Docker Desktop  
- Prepare a shared folder between the host OS and the Docker container  
  - In this demo, we assume the shared folder `data` is located at `$HOME/Documents/`. If it does not exist, please create it using `mkdir -p $HOME/Documents/data`.

Next, download the Docker image from [TigerGraph Downloads](https://dl.tigergraph.com/) and load it:

```bash
docker load -i ./tigergraph-4.2.0-alpha-community-docker-image.tar.gz
```

Once the image is loaded, verify its presence using:

```bash
docker images
```

Then, start a container:

```bash
docker run -d \
  --platform linux/amd64 \
  -p 14240:14240 \
  -v $HOME/Documents/data:/home/tigergraph/data \
  --name tigergraph_community \
  tigergraph/community:4.2.0-alpha
```

### Notes:
- Detached mode (`-d`): Runs the container in the background. Use `docker ps` to check running containers and `docker ps -a` to list all containers.
- Apple Silicon compatibility: On macOS with Apple Silicon (M1/M2/M3), `--platform linux/amd64` is required. Remove it for Intel-based Macs, Linux, or Windows.
- Port mapping (`-p 14240:14240`): Maps GraphStudio (port 14240) to localhost:14240 for web access.
- Volume mapping (`-v $HOME/Documents/data:/home/tigergraph/data`): Ensures data persistence by sharing a folder between the host and the container.
- Container management:
  ```bash
  docker stop tigergraph_community
  docker start tigergraph_community
  ```

---

## 2. Set Up the Python Environment

### 2.1 Clone the Repository
Clone the repository to your local machine:

```bash
git clone https://github.com/tigergraph/ecosys.git
mkdir -p "$HOME/Documents/data/hybrid-search-demo"
cp -r ecosys/demos/hybrid_search "$HOME/Documents/data/hybrid-search-demo"
rm -rf ecosys  # Optional: remove the cloned repo if no longer needed
```

### 2.2 Install Poetry and Python

Follow the [Poetry installation guide](https://python-poetry.org/docs/#installing-with-pipx) to install Poetry, which will manage the Python virtual environment for this demo.

Ensure you have **Python 3.10–3.12** installed, as it is required for the demo. You can download and install it from the [official Python website](https://www.python.org/downloads/).

### 2.3 Install Dependencies

Run the following commands to create a virtual environment and install all required dependencies:  

```bash
cd "$HOME/Documents/data/hybrid-search-demo"
poetry env use python3.12  # Replace with your Python version (3.10–3.12)
poetry install --no-root
```

### 2.4 Activate the Virtual Environment
Activate the environment using:

```bash
eval $(poetry env activate)
```
## 3. Dataset Preparation
Generating the similarity graph and embeddings can be time-consuming. To save time, you can skip this step and download the pre-generated data directly from [this link](https://tigergraph-tutorial-data.s3.us-west-1.amazonaws.com/vector/song_embeddings.csv).

To download the file to `$HOME/Documents/data/hybrid-search-demo/demo/data`, run the following command:  

```bash
curl -o $HOME/Documents/data/hybrid-search-demo/demo/data/song_embeddings.csv https://tigergraph-tutorial-data.s3.us-west-1.amazonaws.com/vector/song_embeddings.csv
```

This ensures that the dataset is readily available for use without the need to run the next two substeps.

### 3.1 Generate the Similarity Graph (Optional)
Use MinHash (LSH) Approximation to generate a similarity graph:
```bash
cd $HOME/Documents/data/hybrid-search-demo/demo
python3 generator/similarity.py
```
Expected Output:
```
Loading dataset...
Computing MinHash signatures...
Generating MinHash: 100%|███████████████████████████| 8640/8640 [00:07<00:00, 1206.81it/s]
Finding similar songs...
Finding similar songs: 100%|███████████████████████| 8640/8640 [00:35<00:00, 244.80it/s]
Saving results...
Saved 552094 similar song pairs to data/similar_songs.csv
```

### 3.2 Generate Embeddings with OpenAI (Optional)
Use OpenAI’s embedding model to generate song embeddings.

#### 🔹 Step 1: Set up OpenAI API Key
Replace `<YOUR_OPENAI_KEY>` with your actual OpenAI API key:
```bash
export OPENAI_API_KEY="<YOUR_OPENAI_KEY>"
```

#### 🔹 Step 2: Run Embedding Generation
```bash
python3 generator/embedding_generator.py
```

---

## 4. Start the Server

Open a new terminal and run the following command to enter the Docker container:

```bash
docker exec -it tigergraph_community /bin/bash
```

Once inside the container, start the TigerGraph server:

```bash
gadmin start all
```

Check the status of all components:  

```bash
gadmin status
```

If **GPE** and **GSE** show **Warmup** while other components are **Online**, congratulations—your server is up and running! Once a graph is inserted, all components, including **GPE** and **GSE**, will switch to **Online** status.

---

## 5. Create Schema, Load Data, and Install Queries  

Switch to the `gsql` folder:

```bash
cd $HOME/data/hybrid-search-demo/demo/gsql/
```

Run the following GSQL scripts in order:

```bash
gsql 1_create_schema.gsql           # Create the schema
gsql 2_add_vector_attributes.gsql   # Add vector attributes
gsql 3_load_data.gsql               # Load the dataset
gsql 4_install_queries.gsql         # Install graph queries
```

---

## 6. Run the Recommendation and QA Systems

Return to your local machine’s terminal (outside the Docker container).

Since we will be using OpenAI, please follow Step 1 in [3.2 Generate Embeddings with OpenAI](#32-generate-embeddings-with-openai) to set up OpenAI API key if you haven’t done so already.

Then launch Jupyter Notebook:  

```bash
jupyter notebook
```

Open `hybrid_search_demo.ipynb` and run the following tasks:  

✅ Graph-Based Similarity Search  
✅ Vector-Based Similarity Search  
✅ Hybrid Search  
✅ Vector-Based Search for QA  
✅ Hybrid Search for QA  
