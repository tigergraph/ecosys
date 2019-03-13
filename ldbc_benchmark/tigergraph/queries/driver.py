############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
############################################################

import sys, logging, time
from datetime import timedelta
from json import loads
from argparse import ArgumentParser

from tornado.httpclient import AsyncHTTPClient, HTTPClient, HTTPClientError

from query_defs import *

# default value for arguments
DEFAULT_PATH_TO_SEEDS = "/home/tigergraph/ldbc_snb_data/substitution_parameters/"
DEFAULT_MAX_NUM_SEEDS = 100
DEFAULT_DEBUG_MODE = 2

# params for interactive short queries
IS_NAME = "Interactive Short"
IS_SIZE = 7

# params for ic queries
IC_NAME = "Interactive Complex"
IC_SIZE = 14

# params for bi queries
BI_NAME = "Business Intelligence"
BI_SIZE = 25

INFO_LVL_1 = 11
INFO_LVL_2 = 12

def info1(self, message, *args, **kws):
  if self.isEnabledFor(INFO_LVL_1):
    self._log(INFO_LVL_1, message, args, **kws) 

def info2(self, message, *args, **kws):
  if self.isEnabledFor(INFO_LVL_2):
    self._log(INFO_LVL_2, message, args, **kws) 

def handle_response(response):
  response_time = 0;
  has_error = False
  logging.Logger.info1(logging.root, "\n[Request] {}".format(response.request.url))

  if response.error:
    has_error = True # this should be catched by the caller first
  else:
    response_json = loads(response.body.decode("utf-8"))
    if response_json["error"]:
      has_error = True
      if "code" in response_json:
        print("-- Error {}: {}".format(response_json["code"] , response_json["message"]))
      else:
        print("-- {}".format(response_json["message"]))
    else:
      response_time = response.time_info["starttransfer"] - response.time_info["pretransfer"]
      logging.Logger.info1(logging.root, "[Response] {}".format(response_json["results"]))
      logging.Logger.info2(logging.root, "[Running Time] {} sec".format(round(response_time, 3)))

  return response_time, has_error

# There is no pre-generated seeds for interactive short queries,
# so generate them using a helper query from a seed for interactive complex queries.
# We'll use interactive_7_param.txt to generate seeds for interactive short queries
def generate_is_seeds(http_client, path, num, query_num = 0):
  with open(path + "interactive_7_param.txt", "r") as f:
    reader = csv.reader(f, delimiter="|")
    next(reader) # skip header
    person_ids = []
    count = 0
    for row in reader:
      person_ids.append(row[0])
      count += 1
      if count >= num:
        break
    
    message_ids = []
    if not query_num in [1,2,3]:
      # run the helper query
      url = get_messages_from_person(person_ids[0], num)
      http_response = http_client.fetch(url, method = "GET")

      # parse json to get message_ids
      response_json = loads(http_response.body.decode("utf-8"))
      message_ids = response_json["results"][0]["message_ids"]
    return person_ids, message_ids

def run_query(http_client, path, num, seed, query_type, query_num, person_ids=[], message_ids=[]):
  response_recv = 0
  response_time = 0
  has_error = None

  print("- {} {}:".format(
      IS_NAME if query_type == "is" else IC_NAME if query_type == "ic" else BI_NAME, query_num))

  urls = []
  if not seed:
    if query_type == "is":
      if query_num in [1,2,3]:
        urls = get_endpoints_is(person_ids, query_type, query_num)
      else:
        urls = get_endpoints_is(message_ids, query_type, query_num)
    else:
      urls = get_endpoints(path, num, query_type, query_num)
  else:
    url = get_endpoint_single(seed, query_type, query_num)
    for i in range(0, num):
      urls.append(url)
  request_sent = len(urls)

  try:
    for url in urls:
      response = http_client.fetch(url, method="GET", connect_timeout=3600, request_timeout=3600)
      t, has_error = handle_response(response)
      if not has_error:
        response_time += t
        response_recv += 1
      else:
        break

    if not has_error:
      print("\n-- # {}: {}".format("Seeds" if not seed else "Iterations", request_sent))
      if response_recv > 0:
        print("-- Average Response Time: {} sec\n".format(round((response_time/response_recv),3)))
  except HTTPClientError as e:
    print("-- Bad Response: HTTP {} {}".format(e.response.code, e.response.reason))
  except Exception as e:
    print("-- Unexpected Error:\n{}".format(repr(e)))

def run_is_wrapper(http_client, path, num, seed, query_num):
  person_ids = []
  message_ids = []

  if not seed:
    # generate seeds
    person_ids, message_ids = generate_is_seeds(http_client, path, num, query_num)
  # run query
  run_query(http_client, path, num, seed, "is", query_num, person_ids, message_ids)

def run_all_is(http_client, path, num):
  # generate seeds
  person_ids, message_ids = generate_is_seeds(http_client, path, num)

  # make reqests
  for i in range(1,IS_SIZE+1):
    run_query(http_client, path, num, None, "is", i, person_ids, message_ids)

def run_all_ic(http_client, path, num):
  for i in range(1,IC_SIZE+1):
    run_query(http_client, path, num, None, "ic", i)

def run_all_bi(http_client, path, num):
  for i in range(1,BI_SIZE+1):
    run_query(http_client, path, num, None, "bi", i)

def run_all(http_client, path, num):
  run_all_is(http_client, path, num)
  run_all_ic(http_client, path, num)
  run_all_bi(http_client, path, num)

if __name__ == "__main__":
  # this is required to retrieve response.time_info even if we use the blocking HTTPClient
  AsyncHTTPClient.configure("tornado.curl_httpclient.CurlAsyncHTTPClient")

  ap = ArgumentParser()
  ap.add_argument("-p", "--path", default=DEFAULT_PATH_TO_SEEDS, 
      help="Full path to the seed directory.")
  ap.add_argument("-n", "--num", type=int, default=DEFAULT_MAX_NUM_SEEDS, 
      help="Number of seeds to run queries.")
  ap.add_argument("-q", "--query", 
      help="Type and/or number of query(ies) to run, \
          e.g. IS_2, Ic_12, bi_22 to run a specific query, \
          and is, iC, BI to run all queries in the given workload")
  ap.add_argument("-d", "--debug", type=int, help="Show HTTP request/response.")
  ap.add_argument("-s", "--seed", 
      help="A pipe-separated string of a parameter to run a query QUERY for NUM number of times.")

  args = ap.parse_args()

  logging.addLevelName(INFO_LVL_1, "INFO1")
  logging.Logger.info1 = info1
  logging.addLevelName(INFO_LVL_2, "INFO2")
  logging.Logger.info2 = info2

  if args.debug:
    logging.basicConfig(stream=sys.stdout, level=(INFO_LVL_1 if args.debug == 1 else INFO_LVL_2), 
        format="%(message)s")

  # here we use blocking HTTP client since response time in AsyncHTTPClient is unstable
  # as the number of clients increases.
  http_client = HTTPClient()

  if not args.query:
    if args.seed:
      print("Note: SEED is ignored (can be used only for running a single query)")
    run_all(http_client, args.path, args.num)
  else:
    query_info = args.query.split("_")
    query_type = query_info[0].lower()
    if len(query_info) == 2:
      query_num = int(query_info[1])
    else:
      query_num = 0
      if args.seed:
        print("Note: SEED is ignored (can be used only for running a single query)")

    if query_type == "is":
      if query_num in range(1,IS_SIZE+1):
        run_is_wrapper(http_client, args.path, args.num, args.seed, query_num)
      else:
        run_all_is(http_client, args.path, args.num)
    elif query_type == "ic":
      if query_num in range(1,IC_SIZE+1):
        run_query(http_client, args.path, args.num, args.seed, "ic", query_num, [], [])
      else:
        run_all_ic(http_client, args.path, args.num)
    elif query_type == "bi":
      if query_num in range(1,BI_SIZE+1):
        run_query(http_client, args.path, args.num, args.seed, "bi", query_num, [], [])
      else:
        run_all_bi(http_client, args.path, args.num)
    else:
      print(args.query + " does not exist")
    
  http_client.close()
