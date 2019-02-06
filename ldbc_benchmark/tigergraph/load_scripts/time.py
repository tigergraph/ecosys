import sys
import os
import re
from datetime import datetime

def getTime(file):
  with open(file, 'r') as f:
    end_time_str = ""
    for line in reversed(list(f)):
      if 'System_GCleanUp|Finished' in line:
        end_time_str = line[:15]
        break
      elif '*** Aborted at' in line:
        end_epoch_str = line[15:25]
        break
    if not end_time_str and not end_epoch_str:
      return False
  begin_time_str = re.match(r'.+\.([0-9]+).log', file, re.M).group(1)
  begin_time = datetime.fromtimestamp(int(begin_time_str)/1000.0)
  if end_time_str:
    end_time_str = "{}-{:02d}-{:02d} {}".format(begin_time.year, begin_time.month, begin_time.day, end_time_str)
    end_time = datetime.strptime(end_time_str, "%Y-%m-%d %H:%M:%S.%f")
  else:
    end_time = datetime.fromtimestamp(int(end_epoch_str))
  if end_time < begin_time:
    end_time = end_time + datetime.timedelta(days=1)
  print("Finished in {} seconds.".format(round((end_time - begin_time).total_seconds(), 3)))
  return True

if __name__ == "__main__":
  if len(sys.argv) != 2:
    print("usage: python3 time.py [FULL_PATH_TO_LOG]")
  else:
    if not getTime(sys.argv[1]):
      print(sys.argv[1] + " is still in progress. Please come back later.")