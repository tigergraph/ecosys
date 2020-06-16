Steps to get TigerGraph Developer + Jupyter notebook 
=================

1. To build the container, download this folder and in it run (assuming you already installed Docker):

   `docker build --tag tigergraph-notebook:0.1 .`
   
2. To run it:

   `docker run -d -p 9000:9000 -p 14240:14240 -p 8888:8888 --ulimit nofile=1000000:1000000 --name tigergraph-notebook tigergraph-notebook:0.1`
   
3. To log in:

   `docker exec -it --user tigergraph tigergraph-notebook /bin/bash`
   
4. To run the gsql server (inside the container):

   `gadmin start`
   
5. To open the Jupyter notebook:

   `/home/tigergraph/anaconda3/bin/jupyter notebook`
   
6. The Jupyter notebook will give you a link like the one below. Copy-paste that link into your browser window:

   `http://127.0.0.1:8888/?token=...`

7. To test that everything works open `gsql101.ipynb` and click Cell -> Run All from the menu (or shift-enter to run one cell at a time).  
   
