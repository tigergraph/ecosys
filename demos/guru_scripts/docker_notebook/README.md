Steps to get TigerGraph Developer + Jupyter notebook 
=================

1. To build the container, download this folder and in it run (assuming you already installed Docker):

   `docker build --tag tigergraph-notebook:0.1 .`
   
2. To run it:

   `docker run -dp 10000:22 --ulimit nofile=1000000:1000000 tigergraph-notebook:0.1`
   
3. To log in (with password tigergraph):

   `ssh -p 10000  -L 8888:localhost:8888 tigergraph@localhost`
   
4. To run the gsql server (inside the container):

   `gadmin start`
   
5. To open the Jupyter notebook:

   `/home/tigergraph/anaconda3/bin/jupyter notebook`
   
6. The Jupyter notebook will give you a link like the one below. Copy-paste that link into your browser window:

   `http://127.0.0.1:8888/?token=...`

7. To test that everything works you can upload `gsql101.ipynb` from this folder and click Cell -> Run All from the menu (or shift-enter to run one cell at a time).  
   
