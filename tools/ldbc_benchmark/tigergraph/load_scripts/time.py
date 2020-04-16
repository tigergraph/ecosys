    
import sys
import os
import re
from datetime import datetime, timedelta
import argparse

def getTimeLoadingJob(file):
  with open(file, "r") as f:
    end_time_str = ""
    for line in reversed(list(f)):
      if "System_GCleanUp|Finished" in line:
        end_time_str = line[:15]
        break
      elif "*** Aborted at" in line:
        end_epoch_str = line[15:25]
        break
    if not end_time_str and not end_epoch_str:
      return -1
  begin_time_str = re.match(r".+\.([0-9]+).log", file, re.M).group(1)
  begin_time = datetime.fromtimestamp(int(begin_time_str)/1000.0)
  if end_time_str:
    end_time_str = "{}-{:02d}-{:02d} {}".format(begin_time.year, begin_time.month, begin_time.day, end_time_str)
    end_time = datetime.strptime(end_time_str, "%Y-%m-%d %H:%M:%S.%f")
  else:
    end_time = datetime.fromtimestamp(int(end_epoch_str))
  if end_time < begin_time:
    end_time = end_time + timedelta(days=1)
  return round((end_time - begin_time).total_seconds(), 3)

def getTimeBuildGstore(file):
  with open(file, "r") as f:
    end_time_str = ""
    num_line_to_read = 10
    for line in reversed(list(f)):
      num_line_to_read -= 1
      if "All done!" in line:
        end_time_str = re.match(r".+\(([0-9\.]+) ms\).+", line).group(1)
        break
      if num_line_to_read <= 0:
        break
    if not end_time_str:
      return -1
    else:
      return round(float(end_time_str)/1000, 3)

if __name__ == "__main__":
  ap = argparse.ArgumentParser()
  ap.add_argument("loading_job", help="Full path to the loding job log file")
  ap.add_argument("-d", "--delta", nargs="?",  help="Full path to the gstore build log file, delta.out")

  args = ap.parse_args()
  loading_job_time = getTimeLoadingJob(args.loading_job)
  if loading_job_time > 0:
    print("- Loading job: {} s".format(loading_job_time))
  else:
    print("Loading job is still in progress. Please come back later.")

  try:
    home_dir = os.environ['HOME']
    link = home_dir + "/.gium" 
    tg_home = os.path.abspath(os.path.join(os.readlink(link), os.pardir))
    
    if "-d" not in sys.argv:
      sys.exit()

    if args.delta == None:
      delta_path = tg_home + "/logs/delta.out"
      if not os.path.isfile(delta_path):
        print ("Failed to anto-detect \"delta.out\", Please specify the full path to <tigergraph.root>/logs/delta.out")
        sys.exit()
      else:
        building_gstore_time = getTimeBuildGstore(delta_path)
    else:
      building_gstore_time = getTimeBuildGstore(args.delta)

    if building_gstore_time > 0:
      print("- Building gstore: {} s".format(building_gstore_time))
      print("- Total: {} s".format(loading_job_time + building_gstore_time))
    else:
      print("Build gstore is still in progress. Please come back later.")

  except SystemExit:
    pass
  except NotADirectoryError:
    print("Can't find TigerGraph. Please make sure that you can run TigerGraph with the current user.")
  except FileNotFoundError:
    print("No such file: " + args.delta)
  except:
    print("Something went wrong.")
