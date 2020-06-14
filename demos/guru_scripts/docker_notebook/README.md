Steps to get TigerGraph Developer + notebook 
=================

1. To build the docker:

   `docker build --tag tigergraph-notebook:0.1 .`
   
2. To run it:

   `docker run -dp 10000:22 --ulimit nofile=1000000:1000000 tigergraph-notebook:0.1`
   
3. To log in (with password tigergraph):

   `ssh -p 10000  -L 8888:localhost:8888 tigergraph@localhost`
   
4. To run the gsql server (inside the docker):

   `gadmin start`
   
5. To open the Jupyter notebook:

   `/home/tigergraph/anaconda3/bin/jupyter notebook`
   
6. Then open a new notebook and test it by pasting into a cell:

   `!gsql tutorial/gsql101/gsql101.gsql`
   
