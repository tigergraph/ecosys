############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
# Author: Mingxi Wu mingxi.wu@tigergraph.com
############################################################
import random
import sys
import os
import datetime
import json
import threading
import errno

from timeit import default_timer as timer

from tornado.httpclient import *
from tornado.ioloop import *
from tornado.httputil import url_concat

from query_runner import *
from config import *

#####################################################################
# K-hop-path-neighbour-count benchmark workload.
# (1) read prepared random nodes from a seed file under seed folder.
# (2) run 1-step and 2-step path count query
#######################################################################

#########################################################################
# return random number of vertices, if the random seed file does not exists, 
# generate randome seed file and put under ./seed/ file, and return it.
#
# filepath: source vertex file
# count: random veretex number
#########################################################################
def GetRandomNodes(filepath, count):
    nodes = []
    # check if random nodes are already generated
    seed_file_path = "./seed/" + os.path.basename(filepath)  

    #create seed folder
    if not os.path.exists(os.path.dirname(seed_file_path)):
     try:
        os.makedirs(os.path.dirname(seed_file_path))
     except OSError as exc: # Guard against race condition
       if exc.errno != errno.EEXIST:
         raise

    #re-use existing seed if it has been generated before.
    if os.path.isfile(seed_file_path):
        pre_nodes = open(seed_file_path, 'r').read().split()
        if len(pre_nodes) >= count:
            print("Found pre-generated randome nodes from " + seed_file_path)
            return pre_nodes[0:count]
    # If the seeds are not generated, generate the random seed file.
    # Take the first count lines from source file,
    # then, randomly replace them one by one, until reach end of file.
    dedup = set()
    fd = open(filepath, 'r')
    line_no = 0
    for line in fd:
        curr_id = line.split()[0]
        if len(nodes) < count:
            if curr_id not in dedup:
                nodes.append(curr_id)
                dedup.add(curr_id)
        else:
            p = random.randint(0, line_no)
            if p < len(nodes) and curr_id not in dedup:
                dedup.remove(nodes[p])
                nodes[p] = curr_id
                dedup.add(curr_id)
        line_no += 1
    with open(seed_file_path, 'w') as ofile:
        for node in nodes:
            ofile.write(node + " ")
    return nodes

###############################################################
# function: check the total latency for k-hop-path neighbor count 
# query for a  given set of seeds. 
#
# filename: vertex file name
# roots: seed set
# db_name: tigergraph or neo4j
# depth: hop number
# notes: comments
################################################################
def RunKNLatency(filename, roots, db_name, depth, notes = ""):
    """
    neo4j
    root,knsize,time
    ...
    summary,average_knsize,average_time
    """
    #create result folder
    if not os.path.exists(os.path.dirname("./result/")):
     try:
        os.makedirs(os.path.dirname("./result/"))
     except OSError as exc: # Guard against race condition
       if exc.errno != errno.EEXIST:
         raise

    ofile = open("./result/KN-latency-" + db_name + "-" + filename + "-k" + str(depth), 'a')
    if db_name == "neo4j":
        runner = Neo4jQueryRunner()
    elif db_name == "tigergraph":
        runner = TigerGraphQueryRunner()
    else:
        print("invalid db " + db_name)
    total_time = 0.0
    total_knsize = 0
    report = "\n---------- " + str(datetime.datetime.now()) + "  " + notes + "  ----------\n"
    timeout_query_cnt = 0
    finish_query_cnt = 0
    for root in roots:
        start = timer()
        # the k-hop distinct neighbor size.
        knsize = runner.KN(root, depth)
        # for timeout query, we return -1
        if knsize == -1:
            timeout_query_cnt += 1
        else: 
            total_knsize += knsize
            finish_query_cnt += 1
            end = timer()
            exe_time = end - start
            total_time += exe_time
            line = root + "," + str(knsize) + "," + str(exe_time)
            print(line)
            report += line + "\n"
   # report += "summary, avg knsize=" + str(total_knsize/len(roots)) + ", avg query time=" + str(total_time/len(roots))
    avgKn = str(total_knsize/finish_query_cnt) if finish_query_cnt > 0 else "NA"
    avgQtime = str(total_time/finish_query_cnt) if finish_query_cnt > 0 else "NA" 
    report += "summary, avg knsize=" + avgKn + ", avg query time=" + avgQtime\
               + "s, timeout query cnt=" + str(timeout_query_cnt)
    ofile.write(report)
    print report


bad_requests = 0
error_requests = 0
correct_requests = 0
completed_requests = 0
total_requests = 0
kn_size = 0;
def handle_neo4j_request(response):
    global bad_requests
    global error_requests
    global correct_requests
    global completed_requests
    global total_requests
    global kn_size
    completed_requests += 1
    if response.error:
        bad_requests += 1
        #print("got bad response: " + str(response))
    else:
        try:
            r = json.loads(response.body)
            kn_size += r["results"][0]["data"][0]["row"][0]
        except:
            bad_requests += 1
            #print("got bad response: " + str(response))
        correct_requests += 1

    if completed_requests >= total_requests:
        IOLoop.instance().stop()

def handle_tigergraph_request(response):
    global bad_requests
    global error_requests
    global correct_requests
    global completed_requests
    global total_requests
    global kn_size
    completed_requests += 1
    if response.error:
        bad_requests += 1
        #print("got bad response: " + str(response))
    else:
        try:
            r = json.loads(response.body)
            kn_size += r["results"][0]["@@subgraph_size"]
        except:
            bad_requests += 1
            #print("got bad response: " + str(response))
        correct_requests += 1

    if completed_requests >= total_requests:
        IOLoop.instance().stop()

def RunKNThroughput(filename, roots, db_name, depth, notes = ""):
    global bad_requests
    global error_requests
    global correct_requests
    global completed_requests
    global total_requests
    global kn_size

    # initialization
    bad_requests = 0
    error_requests = 0
    correct_requests = 0
    completed_requests = 0
    total_requests = len(roots)

    # make reqests
    http_client = AsyncHTTPClient()
    i = 0
    neo4j_payload = {}
    neo4j_payload["statements"] = [
      {
        "statement":"match (n1:MyNode)-[:MyEdge*.." + str(depth) + "]->(n2:MyNode) where n1.id={root} return count(distinct n2)",
         "parameters":{
           "root":""
         }
      }
    ]
    for root in roots:
        if db_name == "neo4j":
          neo4j_payload["statements"][0]["parameters"]["root"] = root
          http_client.fetch("http://127.0.0.1:7474/db/data/transaction/commit", method = "POST", \
                body = json.dumps(neo4j_payload), \
                headers = {"Content-Type" : "application/json"}, \
                callback = handle_neo4j_request, \
                connect_timeout = 3600, request_timeout=3600)
        else:
          params = {"depth": depth, "start_node": root}
          url = url_concat("http://127.0.0.1:9000/query/ksubgraph", params)
          http_client.fetch(url, method = "GET", \
                callback = handle_tigergraph_request, \
                connect_timeout = 3600, request_timeout=3600)

    # start ioloop
    print("RunKNThroughput|fetch issued")

    start_time = time.time()
    IOLoop.instance().start()
    elapsed_seconds = time.time() - start_time
    print("elapsed time: " + str(elapsed_seconds) + " s")
    print("total_requests: " + str(total_requests))
    print("QPS: " + str(total_requests/elapsed_seconds))
    print("bad_requests: " + str(bad_requests))
    print("kn_size: " + str(kn_size))
    print("avg kn_size: " + str(kn_size/total_requests))
    if correct_requests != total_requests:
        return False
    else:
        return True


if __name__ == "__main__":
    AsyncHTTPClient.configure("tornado.curl_httpclient.CurlAsyncHTTPClient", max_clients=20)
    if len(sys.argv) < 7:
        print("Usage: python kn.py raw_file_name num_root db_name depth notes mode")
        sys.exit()
    if sys.argv[6] == "latency":
        print(sys.argv[0] + " " + sys.argv[1] + " " +  sys.argv[2] + " " + sys.argv[3] + " " + sys.argv[4] + " " + sys.argv[5] + " " + sys.argv[6]);
        RunKNLatency(os.path.basename(sys.argv[1]), GetRandomNodes(sys.argv[1], int(sys.argv[2])), sys.argv[3], int(sys.argv[4]), sys.argv[5])
    else:
        RunKNThroughput(os.path.basename(sys.argv[1]), GetRandomNodes(sys.argv[1], int(sys.argv[2])), sys.argv[3], int(sys.argv[4]), sys.argv[5])
