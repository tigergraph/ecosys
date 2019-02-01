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
DEFAULT_DEBUG_MODE = 0

# params for interactive short queries
IS_NAME = "Interactive Short"
IS_SIZE = 7

# params for ic queries
IC_NAME = "Interactive Complex"
IC_SIZE = 14

# params for bi queries
BI_NAME = "Business Intelligence"
BI_SIZE = 25

request_sent = 0
response_recv = 0
response_code = 0
response_msg = ""
response_time = 0
print_response = False

def handle_response(response):
  global request_sent
  global response_recv
  global response_code
  global response_msg
  global response_time
  global print_response
  
  response_recv += 1
  if print_response:
    print("--- Request: " + response.request.url)
  if response.error:
    response_code = response.code
    IOLoop.instance().stop()
  else:
    http_response_json = json.loads(response.body.decode("utf-8"))
    if http_response_json["error"]:
      response_code = http_response_json["code"] 
      response_msg = http_response_json["message"]
      IOLoop.instance().stop()
    else:
      response_time += (response.time_info["starttransfer"] - response.time_info["pretransfer"])
      if print_response:
        print("--- Response:")
        print(http_response_json["results"])
        print("--- {}/{}: {} ms\n".format(response_recv, request_sent, (response.time_info["starttransfer"] - response.time_info["pretransfer"]) * 1000))
  if response_recv >= request_sent:
    IOLoop.instance().stop()

# There is no pre-generated seeds for interactive short queries,
# so generate them using a helper query from a seed for interactive complex queries.
# We'll use interactive_7_param.txt to generate seeds for interactive short queries
def generateSeedsForIS(path_to_seeds, max_num_seeds, query_num = 0):
  with open(path_to_seeds + "interactive_7_param.txt", "r") as f:
    reader = csv.DictReader(f, delimiter="|")
    person_ids = []
    count = 0
    for row in reader:
      person_ids.append(row["personId"])
      count += 1
      if count >= max_num_seeds:
        break
    
    message_ids = []
    if not query_num in [1,2,3]:
      # run the helper query
      http_client = HTTPClient()
      url = get_messages_from_person(person_ids[0], max_num_seeds)
      http_response = http_client.fetch(url, method = "GET")

      # parse json to get message_ids
      http_response_json = json.loads(http_response.body.decode("utf-8"))
      message_ids = http_response_json["results"][0]["message_ids"]
      http_client.close()
    return person_ids, message_ids

def runQuery(async_client, path_to_seeds, max_num_seeds, query_type, query_num, \
             person_ids = [], message_ids = [], debug_mode = 0):
  global request_sent
  global response_recv
  global response_code
  global response_msg
  global response_time
  global print_response

  response_recv = 0
  response_time = 0
  print_response = True if debug_mode > 0 else False

  print((IS_NAME if query_type == "IS" \
    else IC_NAME if query_type == "IC" \
    else BI_NAME) + " {}:".format(query_num))

  if query_type == "IS":
    if query_num in [1,2,3]:
      urls = is_queries(person_ids, query_num)
    else:
      urls = is_queries(message_ids, query_num)
  elif query_type == "IC":
    urls = ic_queries(path_to_seeds, max_num_seeds, query_num)
  elif query_type == "BI":
    urls = bi_queries(path_to_seeds, max_num_seeds, query_num)
  request_sent = len(urls)

  for url in urls:
    async_client.fetch(url, method = "GET", callback = handle_response, \
                       connect_timeout = 3600, request_timeout=3600)

  # start ioloop
  IOLoop.instance().start()
  if not response_code:
    print("- Average Response Time: {} ms".format((response_time * 1000 / response_recv)))
    print("- # Seeds: {}".format(request_sent))
  else:
    if not response_msg:
      print("- Bad Response: HTTP {}".format(response_code))
    else:
      print("- Error {}: {}".format(response_code, response_msg))

def runISWrapper(async_client, path_to_seeds, max_num_seeds, query_num, debug_mode = 0):
  # generate seeds
  person_ids, message_ids = generateSeedsForIS(path_to_seeds, max_num_seeds, query_num)
  # run query
  runQuery(async_client, path_to_seeds, max_num_seeds, "IS", query_num, person_ids, message_ids, debug_mode)

def runAllIS(async_client, path_to_seeds, max_num_seeds):
  # generate seeds
  person_ids, message_ids = generateSeedsForIS(path_to_seeds, max_num_seeds)

  # make reqests
  for i in range(1,IS_SIZE+1):
    runQuery(async_client, path_to_seeds, max_num_seeds, "IS", i, person_ids, message_ids)

def runAllIC(async_client, path_to_seeds, max_num_seeds):
  for i in range(1,IC_SIZE+1):
    runQuery(async_client, path_to_seeds, max_num_seeds, "IC", i)

def runAllBI(async_client, path_to_seeds, max_num_seeds):
  for i in range(1,BI_SIZE+1):
    runQuery(async_client, path_to_seeds, max_num_seeds, "BI", i)

def runAllQueries(path_to_seeds, max_num_seeds):
  async_client = AsyncHTTPClient()

  runAllIS(async_client, path_to_seeds, max_num_seeds)
  runAllIC(async_client, path_to_seeds, max_num_seeds)
  runAllBI(async_client, path_to_seeds, max_num_seeds)

  async_client.close()

if __name__ == "__main__":
  # when max_clients > 1, the response time actually increases since response.time_info includes wait time in the queue.
  # it'd be great if I can find out the queue wait time so that I can exclude it to retrieve the actual response time.
  # for more info: https://www.tornadoweb.org/en/stable/httpclient.html#tornado.httpclient.HTTPResponse
  AsyncHTTPClient.configure("tornado.curl_httpclient.CurlAsyncHTTPClient", max_clients=1)
  
  ap = argparse.ArgumentParser()
  ap.add_argument("--path", help="Full path to the seed directory")
  ap.add_argument("--num", help="Number of seeds to run queries")
  ap.add_argument("--query", help="Type and number of single query to run (e.g. is2, ic12, bi22)")
  ap.add_argument("--seed", help="If you just want to have some seed(s) to test, put the number")
  ap.add_argument("--debug", help="If you want to debug the output, put 1 to print out the reponse")

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
    person_ids, message_ids = generateSeedsForIS(path_to_seeds, max_num_seeds)
    print("person.id\tmessage.id")
    for i in range(0,int(args.seed)):
      print("{}\t{}".format(person_ids[i], message_ids[i]))
    sys.exit()

  if args.debug:
    debug_mode = int(args.debug)
  else:
    debug_mode = DEFAULT_DEBUG_MODE

  if not args.query:
    runAllQueries(path_to_seeds, max_num_seeds)
  else:
    try:
      query_type = args.query[:2].upper()
      query_num = int(args.query[2:])
    except:
      print(args.query + " does not exist")
      sys.exit()

    async_client = AsyncHTTPClient()

    if query_type == "IS" and query_num in range(1,IS_SIZE+1):
      runISWrapper(async_client, path_to_seeds, max_num_seeds, query_num, debug_mode)
    elif query_type == "IC" and query_num in range(1,IC_SIZE+1):
      runQuery(async_client, path_to_seeds, max_num_seeds, "IC", query_num, [], [], debug_mode)
    elif query_type == "BI" and query_num in range(1,BI_SIZE+1):
      runQuery(async_client, path_to_seeds, max_num_seeds, "BI", query_num, [], [], debug_mode)
    else:
      print(args.query + " does not exist")
    
    async_client.close()
