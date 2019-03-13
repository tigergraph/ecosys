import sys, csv
from tornado.httputil import url_concat
from datetime import datetime
if sys.version_info < (3, 0):
  from urllib import pathname2url as quote
else:
  from urllib.parse import quote as quote

ENDPOINT_URL_PREFIX = "http://127.0.0.1:9000/query/ldbc_snb/"

def get_messages_from_person(person_id, num):
  params = {"seed": person_id, "numMessages": num}
  return url_concat(ENDPOINT_URL_PREFIX + "get_messages_from_person", params)

def generate_seed_dict(row, query_type, query_num):
  # is queries
  if query_type == "is":
    if query_num in [1,2,3]:
      seed = {"personId":row}
    else:
      seed = {"messageId":row}
  # ic queries
  if query_type == "ic":
    if query_num == 1:
      seed = {"personId":row[0], "firstName":row[1]}
    elif query_num == 2:
      max_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"personId":row[0], "maxDate":max_date}
    elif query_num == 3:
      start_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"personId":row[0], "startDate":start_date, "durationDays":row[2], "countryXName":row[3], "countryYName":row[4]}
    elif query_num == 4:
      start_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"personId":row[0], "startDate":start_date, "durationDays":row[2]}
    elif query_num == 5:
      min_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"personId":row[0], "minDate":min_date}
    elif query_num == 6:
      seed = {"personId":row[0], "tagName":row[1]}
    elif query_num == 7:
      seed = {"personId":row[0]}
    elif query_num == 8:
      seed = {"personId":row[0]}
    elif query_num == 9:
      max_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"personId":row[0], "maxDate":max_date}
    elif query_num == 10:
      month = int(row[1])
      next_month = (month + 1) if month < 12 else 1
      seed = {"personId":row[0], "month":str(month), "nextMonth":str(next_month)}
    elif query_num == 11:
      seed = {"personId":row[0], "countryName":row[1], "workFromYear":row[2]}
    elif query_num == 12:
      seed = {"personId":row[0], "tagClassName":row[1]}
    elif query_num == 13:
      seed = {"person1Id":row[0], "person2Id":row[1]}
    elif query_num == 14:
      seed = {"person1Id":row[0], "person2Id":row[1]}
  # bi queries
  elif query_type == "bi":
    if query_num == 1:
      max_date = datetime.fromtimestamp(int(row[0])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"maxDate":max_date}
    elif query_num == 2:
      start_date = datetime.fromtimestamp(int(row[0])/1000).strftime("%Y-%m-%d %H:%M:%S")
      end_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"startDate":start_date, "endDate":end_date, "country1Name":row[2], "country2Name":row[3]}
    elif query_num == 3:
      seed = {"year1":row[0], "month1":row[1]}
    elif query_num == 4:
      seed = {"tagClassName":row[0], "countryName":row[1]}
    elif query_num == 5:
      seed = {"countryName":row[0]}
    elif query_num == 6:
      seed = {"tagName":row[0]}
    elif query_num == 7:
      seed = {"tagName":row[0]}
    elif query_num == 8:
      seed = {"tagName":row[0]}
    elif query_num == 9:
      seed = {"tagClass1Name":row[0], "tagClass2Name":row[1], "threshold":row[2]}
    elif query_num == 10:
      min_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"tagName":row[0], "minDate":min_date}
    elif query_num == 11:
      seed = {"countryName":row[0], "blacklist":row[1].split(";")}
    elif query_num == 12:
      date = datetime.fromtimestamp(int(row[0])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"minDate":date, "likeThreshold":row[1]}
    elif query_num == 13:
      seed = {"countryName":row[0]}
    elif query_num == 14:
      start_date = datetime.fromtimestamp(int(row[0])/1000).strftime("%Y-%m-%d %H:%M:%S")
      end_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"startDate":start_date, "endDate":end_date}
    elif query_num == 15:
      seed = {"countryName":row[0]}
    elif query_num == 16:
      seed = {"personId":row[0], "countryName":row[1], "tagClassName":row[2], "minPathDistance":row[3], "maxPathDistance":row[4]}
    elif query_num == 17:
      seed = {"countryName":row[0]}
    elif query_num == 18:
      min_date = datetime.fromtimestamp(int(row[0])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"minDate":min_date, "lengthThreshold":row[1], "languages":row[2].split(";")}
    elif query_num == 19:
      min_date = datetime.fromtimestamp(int(row[0])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"minDate":min_date, "tagClass1Name":row[1], "tagClass2Name":row[2]}
    elif query_num == 20:
      seed = {"tagClassNames":row[0]}
    elif query_num == 21:
      end_date = datetime.fromtimestamp(int(row[1])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"countryName":row[0], "endDate":end_date}
    elif query_num == 22:
      seed = {"country1Name":row[0], "country2Name":row[1]}
    elif query_num == 23:
      seed = {"countryName":row[0]}
    elif query_num == 24:
      seed = {"tagClassName":row[0]}
    elif query_num == 25:
      start_date = datetime.fromtimestamp(int(row[2])/1000).strftime("%Y-%m-%d %H:%M:%S")
      end_date = datetime.fromtimestamp(int(row[3])/1000).strftime("%Y-%m-%d %H:%M:%S")
      seed = {"person1Id":row[0], "person2Id":row[1], "startDate":start_date, "endDate":end_date}
  return seed

def get_endpoint_url(seed, query_type, query_num):
  url_prefix = ENDPOINT_URL_PREFIX + "{}_{}?".format(query_type, query_num)
  args = ""
  for key, value in seed.items():
    if not type(value) is list:
      if type(value) is bytes:
        args += "{}={}&".format(key, quote(value))
      else:
        args += "{}={}&".format(key, value)
    else:
      for v in value:
        args += "{}={}&".format(key, quote(v))
  return url_prefix + args[:-1]

def get_endpoints_is(ids, query_type, query_num):
  urls = []
  for id in ids:
    seed = generate_seed_dict(id, query_type, query_num)
    url = get_endpoint_url(seed, query_type, query_num)
    urls.append(url)
  return urls

def get_endpoints(path, num, query_type, query_num):
  urls = []
  f_name = "interactive_{}_param.txt".format(query_num) if query_type == "ic" \
      else "bi_{}_param.txt".format(query_num)

  with open(path + f_name, "r") as f:
    reader = csv.reader(f, delimiter="|")
    next(reader) # skip header
    seeds = []
    count = 0
    for row in reader:
      seed = generate_seed_dict(row, query_type, query_num)
      url = get_endpoint_url(seed, query_type, query_num)
      urls.append(url)
      count += 1
      if count >= num:
        break
  return urls

def get_endpoint_single(params, query_type, query_num):
  row = params.split("|")
  if query_type == "is":
    seed = generate_seed_dict(row, query_type, query_num)
  elif query_type == "ic":
    seed = generate_seed_dict(row, query_type, query_num)
  elif query_type == "bi":
    seed = generate_seed_dict(row, query_type, query_num)
  return get_endpoint_url(seed, query_type, query_num)
