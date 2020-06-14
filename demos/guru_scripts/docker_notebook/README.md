Steps to get TigerGraph Developer + notebook 
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
   
6. At this point the Jupyter notebook will give you a link of the form:

   `http://127.0.0.1:8888/?token=e0bea4e3210e728c8c0f824309feeab8d0eff97f35dc1fad`
   
Copy paste that link into your browser window

7. Then open a new notebook and test it by pasting the follwing into a cell and running it:

   `!gsql tutorial/gsql101/gsql101.gsql`
   
