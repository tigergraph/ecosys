Steps to get TigerGraph Developer + Jupyter notebook 
=================

1. To build the Docker container download this folder, open a terminal, and in this folder run (assuming you already installed Docker):

   `docker build --tag tigergraph-notebook:0.2 .`

2. To run the Docker container for the first time:

   `docker run -it -p 9000:9000 -p 14240:14240 -p 8888:8888 --ulimit nofile=1000000:1000000 --name tigergraph-notebook tigergraph-notebook:0.2`
      
3. To open the Jupyter notebook, navigate in your browser to [http://localhost:8888](http://localhost:8888).

4. To test that everything works, open `gsql101.ipynb` and click Cell -> Run All from the menu (or shift-enter to run one cell at a time). Don't worry if it looks like it's stuck. Some of the cells can take up to a minute or two to compute. 
5. To open TigerGraph Studio navigate to [http://localhost:14240](http://localhost:14240).

6. To close the Jupyter notebook server press `Ctrl-c` in the terminal. To stop the Docker container run `exit`.

7. To restart the server:

   `docker start tigergraph-notebook && docker attach tigergraph-notebook`

8. To erase the current container with its modified files (but keep the image produced at step 1):

   `docker rm -f tigergraph-notebook`