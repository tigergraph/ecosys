import yaml
import re
import sys
import datetime
import os
import shutil

# For any segment, if it has "vertexstatus.bin", and its "NumOfDeletedVertices" is 0,
# move its "vertexstatus.bin" to the following folder.
# Run the script with "-r" will undo the change.
dest = "/tmp/vertexstatus_backup"

#find out gstore path
home = os.path.expanduser("~")
gstore_path = ""
for line in open(home + "/.gsql/gsql.cfg", "r"):
    if re.search("tigergraph.storage", line):
        gstore_path = line[:-1].split(": ")[1]
        break
if gstore_path == "":
    print "No gstore path found, please check \'grep -rn \"tiergraph.storage\" ~/.gsql/gsql.cfg\'"
else:
    print "gtore_path: %s" %gstore_path

#move target "vertexstatus.bin" to the dest folder
def Clean():
  partition_folder = gstore_path + "/0/part/"
  segment_folder_list =  [partition_folder + d  for d in os.listdir(partition_folder) if os.path.isdir(partition_folder +"/" + d) and d[0] != '.'];
  #print segment_folder_list
  mv_segments = list()
  for folder in segment_folder_list:
      config = yaml.load(open(folder + "/segmentconfig.yaml", "r"), Loader=yaml.FullLoader)
      statusFile = folder + "/vertexstatus.bin"
      if int(config["NumOfDeletedVertices"]) == 0 and os.path.isfile(statusFile):
          segid = folder.split("/")[-1]
          pstr = "Found target segment \'%3s\': " %(segid)
          if os.path.islink(folder):
              folder = os.readlink(folder)
          print "%s [SOLVED] mv to path: %s" % (pstr, dest + "/" + segid)
          os.mkdir(dest + "/" + segid, 0755);
          shutil.move(statusFile, dest + "/" + segid)
          mv_segments.append([folder, dest + "/" + segid])
  return mv_segments

def CleanUp():
  if (os.path.isdir(dest)):
    shutil.move(dest, dest + "_" + datetime.datetime.now().strftime("%Y-%m-%d_%H:%M:%S"))
  os.mkdir(dest, 0755);
  mv_segments = Clean();
  #print mv_segments
  if len(mv_segments) > 0:
    mvlist = ['|'.join(x) for x in mv_segments]
    with open(gstore_path + "/mv_info", 'w') as myfile:
        myfile.write('\n'.join(mvlist))
  else:
    os.rmdir(dest)
    print "No target segment found."

def Recover():
  mv_info_file = gstore_path + "/mv_info"
  if os.path.exists(mv_info_file):
    with open(mv_info_file, 'r') as myfile:
      data = myfile.read()
      if len(data) > 0 :
        print "Recover from mv_info"
        datalist = data.split("\n")
        for line in datalist:
          path = line.split('|')
          if len(path) == 2:
            print "mv vertexstatus.bin from \'%s\' to \'%s\'" %(path[1], path[0])
            shutil.move(path[1] + "/vertexstatus.bin", path[0])
          else:
            print "Got wrong info from line: \'%s\' with size = %d" %(line, len(path))

arglist =  sys.argv
if len(arglist) == 1:
  CleanUp()
elif arglist[1] == "-r":
  Recover()
  shutil.rmtree(dest)
