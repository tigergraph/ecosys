nohup python kn.py $1/twitter_rv.net-seed  300 tigergraph 1 notes latency
nohup python kn.py $1/twitter_rv.net-seed  300 tigergraph 2 notes latency
nohup python kn.py $1/twitter_rv.net-seed  10 tigergraph 3 notes latency
nohup python kn.py $1/twitter_rv.net-seed  10 tigergraph 6 notes latency
nohup python wcc.py twitter-rv tigergraph 3
nohup python pg.py twitter-rv tigergraph 10 3
