############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
# Author: Mingxi Wu mingxi.wu@tigergraph.com
############################################################

from datetime import datetime, timedelta
from json import loads
from argparse import ArgumentParser

from tornado.httpclient import AsyncHTTPClient
from tornado.ioloop import IOLoop

from query_defs import *

# default value for arguments
DEFAULT_PATH_TO_SEEDS = "/home/tigergraph/ldbc_snb_data/substitution_parameters/"
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
response_time = 0
response_code = 0
response_msg = ""
print_response = False

def handle_response(response):
  global request_sent
  global response_recv
  global response_time
  global response_code
  global response_msg
  global print_response
  
  response_recv += 1
  if print_response:
    print("--- Request: " + response.request.url)

  if response.error:
    response_code = response.code
    IOLoop.instance().stop()
  else:
    http_response_json = loads(response.body.decode("utf-8"))
    if http_response_json["error"]:
      if "code" in http_response_json:
        response_code = http_response_json["code"] 
      else:
        response_code = "RUNTIME"
      response_msg = http_response_json["message"]
      IOLoop.instance().stop()
    else:
      response_time += (response.time_info["starttransfer"] - response.time_info["pretransfer"])
      if print_response:
        print("--- Response:")
        print(http_response_json["results"])
        print()
        
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
  global response_time
  global response_code
  global response_msg
  global print_response

  response_recv = 0
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
  # time_begin = time.time()
  IOLoop.instance().start()
  # time_elapsed = time.time() - time_begin
  if not response_code:
    print("- # Requests Sent: {}".format(request_sent))
    # print("- Elapsed Time: {} sec".format(time_elapsed))
    # print("- QPS: " + str(response_recv/time_elapsed))
    print("- Average Response Time: {}".format(str(timedelta(seconds=(response_time / response_recv)))))
  else:
    if response_code == "RUNTIME":
      print("- Runtime Error: {}".format(response_msg))
    elif not response_msg:
      print("- Bad Response: HTTP {}".format(response_code))
    else:
      print("- Error {}: {}".format(response_code , response_msg))

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
  # average response time somehow increases when max_clients > 1, could be a bug in async request
  # detail: https://www.tornadoweb.org/en/stable/httpclient.html#tornado.httpclient.HTTPResponse
  AsyncHTTPClient.configure("tornado.curl_httpclient.CurlAsyncHTTPClient", max_clients=1)
  
  ap = ArgumentParser()
  ap.add_argument("--path", help="Full path to the seed directory")
  ap.add_argument("--num", help="Number of seeds to run queries")
  ap.add_argument("--query", help="Type and number of single query to run (e.g. is2, ic12, bi22)")
  ap.add_argument("--seed", help="If you just want to have some seed(s) to test, put the number")
  ap.add_argument("--debug", help="If you want to debug the output, put 1 to print out the reponse")

  args = ap.parse_args()
  
  path_to_seeds = args.path if args.path else DEFAULT_PATH_TO_SEEDS
  max_num_seeds = int(args.num) if args.num else DEFAULT_MAX_NUM_SEEDS
  debug_mode = int(args.debug) if args.debug else DEFAULT_DEBUG_MODE

  if args.seed:
    person_ids, message_ids = generateSeedsForIS(path_to_seeds, max_num_seeds)
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
