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
import errno

from timeit import default_timer as timer

from query_runner import *
from config import *


###########################################################
# Weakly Connected Component benchmark 
###########################################################

def RunWCC(filename, db_name, num_tests, notes = ""):
    """
    #test,time
    ...
    summary,average_time
    """
    #create result folder
    if not os.path.exists(os.path.dirname("./result/")):
     try:
        os.makedirs(os.path.dirname("./result/"))
     except OSError as exc: # Guard against race condition
       if exc.errno != errno.EEXIST:
         raise


    ofile = open("result/WCC-latency-" + db_name + "-" + filename, 'a')
    if db_name == "neo4j":
        runner = Neo4jQueryRunner()
    elif db_name == "tigergraph":
        runner = TigerGraphQueryRunner()
    else:
        print("invalid db " + db_name)
    total_time = 0.0
    report = "\n---------- " + str(datetime.datetime.now()) + "  " + notes + "  ----------\n"
    for i in range(0, num_tests):
        start = timer()
        runner.WCC()
        end = timer()
        exe_time = end - start
        total_time += exe_time
        line = str(i) + "," + str(exe_time)
        report += line + "\n"
    report += "summary, avg time " + str(total_time/num_tests) + " seconds"
    ofile.write(report)
    print (report)


if __name__ == "__main__":
    # kn.py file_name db_name num_test
    if len(sys.argv) < 4:
        print("Usage: python wcc.py file_name db_name num_tests")
        print("e.g. python wcc.py /data/graph500/graph500-22 neo4j 3")
        print("e.g. python wcc.py /data/graph500/graph500-22 tigergraph 3")
        
        sys.exit()

    RunWCC(os.path.basename(sys.argv[1]), sys.argv[2], int(sys.argv[3]), sys.argv[4] if len(sys.argv) == 5 else "")
