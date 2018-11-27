nohup python kn.py $1/graph500-22-seed  300 tigergraph 1 notes latency
nohup python kn.py $1/graph500-22-seed  300 tigergraph 2 notes latency
nohup python kn.py $1/graph500-22-seed  10 tigergraph 3 notes latency
nohup python kn.py $1/graph500-22-seed  10 tigergraph 6 notes latency
nohup python wcc.py graph500-22 tigergraph 3
nohup python pg.py graph500-22 tigergraph 10 3 
