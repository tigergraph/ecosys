from tornado.httputil import url_concat
import csv

ENDPOINT_URL_PREFIX = "http://127.0.0.1:9000/query/ldbc_snb/"

def get_messages_from_person(person_id, max_num_seeds):
  params = {"seed": person_id, "num_messages": max_num_seeds}
  return url_concat(ENDPOINT_URL_PREFIX + "get_messages_from_person", params)

def is_queries(ids, query_num):
  urls = []
  for id in ids:
    params = {"seed": id}
    url = url_concat(ENDPOINT_URL_PREFIX + "is_{}".format(query_num), params)
    urls.append(url)
  return urls

def ic_queries(path_to_seeds, max_num_seeds, query_num):
  with open(path_to_seeds + "interactive_{}_param.txt".format(query_num), "r") as f:
    reader = csv.DictReader(f, delimiter="|")
    seeds = []
    count = 0
    for row in reader:
      if query_num == 1:
        seed = {"personId":row["personId"], "firstName":row["firstName"]}
      elif query_num == 2:
        seed = {"personId":row["personId"], "maxDateEpoch":row["minDate"]}
      elif query_num == 3:
        seed = {"personId":row["personId"], "startDateEpoch":row["startDate"], "durationDays":row["durationDays"], "countryXName":row["countryXName"], "countryYName":row["countryYName"]}
      elif query_num == 4:
        seed = {"personId":row["personId"], "startDate":row["startDate"], "durationDays":row["durationDays"]}
      elif query_num == 5:
        seed = {"personId":row["personId"], "minDate":row["minDate"]}
      elif query_num == 6:
        seed = {"personId":row["personId"], "tagName":row["tagName"]}
      elif query_num == 7:
        seed = {"personId":row["personId"]}
      elif query_num == 8:
        seed = {"personId":row["personId"]}
      elif query_num == 9:
        seed = {"personId":row["personId"], "minDate":row["minDate"]}
      elif query_num == 10:
        seed = {"personId":row["personId"], "month":row["month"]}
      elif query_num == 11:
        seed = {"personId":row["personId"], "countryName":row["countryName"], "workFromYear":row["workFromYear"]}
      elif query_num == 12:
        seed = {"personId":row["personId"], "tagClassName":row["tagClassName"]}
      elif query_num == 13:
        seed = {"person1Id":row["person1Id"], "person2Id":row["person2Id"]}
      elif query_num == 14:
        seed = {"person1Id":row["person1Id"], "person2Id":row["person2Id"]}

      seeds.append(seed)
      count += 1
      if count >= max_num_seeds:
        break
  urls = []
  for seed in seeds:
    url = url_concat(ENDPOINT_URL_PREFIX + "ic_{}".format(query_num), seed)
    urls.append(url)
  return urls

def bi_queries(path_to_seeds, max_num_seeds, query_num):
  with open(path_to_seeds + "bi_{}_param.txt".format(query_num), "r") as f:
    reader = csv.DictReader(f, delimiter="|")
    seeds = []
    count = 0
    for row in reader:
      if query_num == 1:
        seed = {"date":row["date"]}
      elif query_num == 2:
        seed = {"date1":row["date1"], "date2":row["date2"], "country1":row["country1"], "country2":row["country2"]}
      elif query_num == 3:
        seed = {"year":row["year"], "month":row["month"]}
      elif query_num == 4:
        seed = {"tagClass":row["tagClass"], "country":row["country"]}
      elif query_num == 5:
        seed = {"country":row["country"]}
      elif query_num == 6:
        seed = {"tag":row["tag"]}
      elif query_num == 7:
        seed = {"tag":row["tag"]}
      elif query_num == 8:
        seed = {"tag":row["tag"]}
      elif query_num == 9:
        seed = {"tagClass1":row["tagClass1"], "tagClass2":row["tagClass2"], "threshold":row["threshold"]}
      elif query_num == 10:
        seed = {"tag":row["tag"], "date":row["date"]}
      elif query_num == 11:
        seed = {"country":row["country"], "blacklist":row["blacklist"]}
      elif query_num == 12:
        seed = {"date":row["date"], "likeThreshold":row["likeThreshold"]}
      elif query_num == 13:
        seed = {"country":row["country"]}
      elif query_num == 14:
        seed = {"startDate":row["startDate"], "endDate":row["endDate"]}
      elif query_num == 15:
        seed = {"country":row["country"]}
      elif query_num == 16:
        seed = {"person":row["person"], "country":row["country"], "tagClass":row["tagClass"], "minPathDistance":row["minPathDistance"], "maxPathDistance":row["maxPathDistance"]}
      elif query_num == 17:
        seed = {"country":row["country"]}
      elif query_num == 18:
        seed = {"date":row["date"], "lengthThreshold":row["lengthThreshold"], "languages":row["languages"]}
      elif query_num == 19:
        seed = {"date":row["date"], "tagClass1":row["tagClass1"], "tagClass2":row["tagClass2"]}
      elif query_num == 20:
        seed = {"tagClasses":row["tagClasses"]}
      elif query_num == 21:
        seed = {"country":row["country"], "endDate":row["endDate"]}
      elif query_num == 22:
        seed = {"country1":row["country1"], "country2":row["country2"]}
      elif query_num == 23:
        seed = {"country":row["country"]}
      elif query_num == 24:
        seed = {"tagClass":row["tagClass"]}
      elif query_num == 25:
        seed = {"person1Id":row["person1Id"], "person2Id":row["person2Id"], "startDate":row["startDate"], "endDate":row["endDate"]}

      seeds.append(seed)
      count += 1
      if count >= max_num_seeds:
        break
  urls = []
  for seed in seeds:
    url = url_concat(ENDPOINT_URL_PREFIX + "bi_{}".format(query_num), seed)
    urls.append(url)
  return urls