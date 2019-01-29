############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
# Author: Mingxi Wu mingxi.wu@tigergraph.com
############################################################
import sys
import datetime
import json
import threading
import errno
import csv
import argparse

from timeit import default_timer as timer

from tornado.httpclient import *
from tornado.ioloop import *
from tornado.httputil import *

from query_defs import *

# default value for arguments
DEFAULT_PATH_TO_SEEDS = "/home/ubuntu/ldbc_snb_data_sf1/substitution_parameters/"
DEFAULT_MAX_NUM_SEEDS = 100

# params for interactive short queries
IS_NAME = "Interactive Short"
IS_SIZE = 7

# params for ic queries
IC_NAME = 'Interactive Complex'
IC_SIZE = 14

# params for bi queries
BI_NAME = 'Business Intelligence'
BI_SIZE = 25

requests_sent = 0
response_bad = 0
response_ok = 0
requests_total = 0
response_time = 0

def handle_request(response):
  global requests_sent
  global response_bad
  global response_ok
  global requests_total
  global response_time
  
  requests_sent += 1
  if response.error:
    response_bad += 1
    print("response.error: " + str(response))
    IOLoop.instance().stop()
  else:
    response_ok += 1
    response_time += (response.time_info["starttransfer"] - response.time_info["pretransfer"])
  if requests_sent >= requests_total:
    IOLoop.instance().stop()

# There is no pre-generated seeds for interactive short queries,
# so generate them using a helper query from a seed for interactive complex queries.
# We'll use interactive_7_param.txt to generate seeds for interactive short queries
def generateSeedsForInteractiveShortQueries(path_to_seeds, max_num_seeds, query_num = 0):
  with open(path_to_seeds + "interactive_7_param.txt", "r") as f:
    print("Generating seeds for " + IS_NAME + " queries...", end=" ", flush=True)
    reader = csv.DictReader(f, delimiter='|')
    person_ids = []
    count = 0
    for row in reader:
      person_ids.append(row['personId'])
      count += 1
      if count >= max_num_seeds:
        break
    
    message_ids = []
    if not query_num in [1,2,3]:
      # run the helper query
      http_client = HTTPClient()
      url = get_messages_from_person(person_ids[0], max_num_seeds)
      http_response = http_client.fetch(url, method = "GET")
      print('Done') # Generating seeds for Interactive short queries...

      # parse json to get message_ids
      http_response_json = json.loads(http_response.body.decode('utf-8'))
      message_ids = http_response_json['results'][0]['message_ids']
      http_client.close()
    return person_ids, message_ids

def runIS(path_to_seeds, max_num_seeds, query_num, person_ids, message_ids):
  global requests_sent
  global response_bad
  global response_ok
  global requests_total
  global response_time

  # initialization
  requests_sent = 0
  response_bad = 0
  response_ok = 0
  response_time = 0

  http_client = AsyncHTTPClient()
  if query_num in [1,2,3]:
    urls = is_queries(query_num, person_ids)
  else:
    urls = is_queries(query_num, message_ids)
  for url in urls:
    http_client.fetch(url, method = "GET", callback = handle_request, \
                      connect_timeout = 3600, request_timeout=3600)
  requests_total = len(urls)

  # start ioloop
  print(IS_NAME + " {}:".format(query_num))
  IOLoop.instance().start()
  print("- Average Response Time: {} ms".format((response_time * 1000 / response_ok)))
  print("- # OK Responses: {}".format(response_ok) )
  if response_bad > 0:
    print("- # Bad Responses: {}".format(response_bad))
  http_client.close()
  if response_ok != len(urls):
    return False
  return True

def runISWrapper(path_to_seeds, max_num_seeds, query_num):
  # generate seeds
  person_ids, message_ids = generateSeedsForInteractiveShortQueries(path_to_seeds, max_num_seeds, query_num)
  runIS(path_to_seeds, max_num_seeds, query_num, person_ids, message_ids)

def runAllIS(path_to_seeds, max_num_seeds):
  global requests_sent
  global response_bad
  global response_ok
  global requests_total

  # generate seeds
  person_ids, message_ids = generateSeedsForInteractiveShortQueries(path_to_seeds, max_num_seeds)

  # make reqests
  for i in range(1,IS_SIZE+1):
    if not runIS(path_to_seeds, max_num_seeds, i, person_ids, message_ids):
      return False
  return True

def runICWrapper(path_to_seeds, max_num_seeds, query_num):
  print(IC_NAME + " is not implemented yet")
  return True

def runBIWrapper(path_to_seeds, max_num_seeds, query_num):
  print(BI_NAME + " is not implemented yet")
  return True

def runAllIC(path_to_seeds, max_num_seeds):
  print(IC_NAME + " is not implemented yet")
  return True

def runAllBI(path_to_seeds, max_num_seeds):
  print(BI_NAME + " is not implemented yet")
  return True

def runAllQueries(path_to_seeds, max_num_seeds):
  # Interactive Short
  runAllIS(path_to_seeds, max_num_seeds)

  # Interactive Complex
  runAllIC(path_to_seeds, max_num_seeds)

  # Business Intelligence
  runAllBI(path_to_seeds, max_num_seeds)

if __name__ == "__main__":
  AsyncHTTPClient.configure("tornado.curl_httpclient.CurlAsyncHTTPClient", max_clients=22)
  
  ap = argparse.ArgumentParser()
  ap.add_argument("--path", help="Full path to the seed directory")
  ap.add_argument("--num", help="Number of seeds to run querie(s)")
  ap.add_argument("--query", help="Type and number of single query to run (e.g. is2, ic12, bi22)")
  ap.add_argument("--seed", help="If you just want to have some seed(s) to test, put the number")

  args = ap.parse_args()
  
  if args.path:
    path_to_seeds = args.path
  else:
    path_to_seeds = DEFAULT_PATH_TO_SEEDS

  if args.num:
    max_num_seeds = int(args.num)
  else:    
    max_num_seeds = DEFAULT_MAX_NUM_SEEDS

  if args.seed:
    person_ids, message_ids = generateSeedsForInteractiveShortQueries(path_to_seeds, max_num_seeds)
    print("person.id\tmessage.id")
    for i in range(0,int(args.seed)):
      print("{}\t{}".format(person_ids[i], message_ids[i]))
    sys.exit()

  if not args.query:
    runAllQueries(path_to_seeds, max_num_seeds)
  else:
    try:
      query_type = args.query[:2].upper()
      query_num = int(args.query[2:])
      if query_type == "IS" and query_num in range(1,IS_SIZE+1):
        runISWrapper(path_to_seeds, max_num_seeds, query_num)
      elif query_type == "IC" and query_num in range(1,IC_SIZE+1):
        runICWrapper(path_to_seeds, max_num_seeds, query_num)
      elif query_type == "BI" and query_num in range(1,BI_SIZE+1):
        runBIWrapper(path_to_seeds, max_num_seeds, query_num)
      else:
        print(args.query + " does not exist")
    except ValueError:
      print(args.query[2:] + " is not a valid number")
