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
from timeit import default_timer as timer

from query_runner import *
from config import *


###########################################################
# PageRank benchmark
###########################################################

def RunPG(filename, db_name, num_iteration, num_tests, notes = ""):
    """
    #test,num_iteration,time
    ...
    summary,num_iteration,average_time
    """
    #create result folder
    if not os.path.exists(os.path.dirname("./result/")):
     try:
        os.makedirs(os.path.dirname("./result/"))
     except OSError as exc: # Guard against race condition
       if exc.errno != errno.EEXIST:
         raise

    ofile = open("result/PG-latency-" + db_name + "-" + filename, 'a')
    if db_name == "neo4j":
        runner = Neo4jQueryRunner()
    elif db_name == "tigergraph":
        runner = TigerGraphQueryRunner()
    else:
        print("invalid db " + db_name)
    total_time = 0.0
    total_knsize = 0
    report = "\n----------page rank " + str(datetime.datetime.now()) + "  " + notes + "  ----------\n"
    for i in range(0, num_tests):
        start = timer()
        runner.PG(num_iteration)
        end = timer()
        exe_time = end - start
        total_time += exe_time
        line = str(i) + "," + str(num_iteration) + "," + str(exe_time) + " seconds"
        print(line)
        report += line + "\n"
    report += "summary," + str(num_iteration) + "," + str(total_time/num_tests) + " seconds \n"
    ofile.write(report)
    print report


if __name__ == "__main__":
    # kn.py file_name db_name num_iteration
    if len(sys.argv) < 5:
        print("Usage: python pg.py raw_file_name db_name num_iteration num_tests")
        sys.exit()
    # python pg.py graph500-22 neo4j 1 2
    RunPG(os.path.basename(sys.argv[1]), sys.argv[2], int(sys.argv[3]), int(sys.argv[4]), sys.argv[5] if len(sys.argv) == 6 else "")
