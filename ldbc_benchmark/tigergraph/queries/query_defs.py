from tornado.httputil import url_concat

ENDPOINT_URL_PREFIX = "http://127.0.0.1:9000/query/ldbc_snb/"

def get_messages_from_person(person_id, max_num_seeds):
  params = {"seed": person_id, "num_messages": max_num_seeds}
  return url_concat(ENDPOINT_URL_PREFIX + "get_messages_from_person", params)

def is_queries(i, ids):
  urls = []
  for id in ids:
    params = {"seed": id}
    url = url_concat(ENDPOINT_URL_PREFIX + "is_{}".format(i), params)
    urls.append(url)
  return urls

