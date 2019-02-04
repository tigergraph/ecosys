from datetime import datetime

with open('/home/neo4j/neo4j-community-3.5.1/logs/debug.log', 'r') as log:
  begin = []
  end = []
  for line in log:
    if 'Index population started' in line:
      begin.append(line[:23])
    elif 'Index creation finished' in line:
      end.append(line[:23])
  if len(begin) > 9:
    print("Something went wrong. Please check debug.log")
  elif len(begin) != len(end):
    print("{}/{} Done. Please come back later.".format(len(end), len(begin)))
  else:
    elapsed_time = 0
    for i in range(0,9):
      begin_tmp = datetime.strptime(begin[i], '%Y-%m-%d %H:%M:%S.%f')
      end_tmp = datetime.strptime(end[i],'%Y-%m-%d %H:%M:%S.%f')
      elapsed_time += (end_tmp-begin_tmp).total_seconds()
    
    print("Done in {} s".format(elapsed_time))
