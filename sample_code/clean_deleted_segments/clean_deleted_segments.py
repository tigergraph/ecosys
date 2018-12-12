import yaml
import re
import sys
import os
import shutil
import csv

# The folder for the mv operation, user need to make sure that this folder is large enough to hold all deleted segments
dest = "/tmp/deleted_segments/"

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

#find out deleted typid
def ReadDeletedTypeList():
  yaml_config = gstore_path + "/0/part/config.yaml"
  config = yaml.load(open(yaml_config, "r"))
  #print config["VertexTypes"]
  deleted_typeid_list = list()
  for vtype in config["VertexTypes"]:
      if "Deleted" in vtype:
          deleted_typeid_list.append(vtype["VertexId"])
  return deleted_typeid_list


#move deleted folder into /tmp
def Clean(deleted_typeid_list):
  partition_folder = gstore_path + "/0/part/"
  segment_folder_list =  [partition_folder + d  for d in os.listdir(partition_folder) if os.path.isdir(partition_folder +"/" + d) and d[0] != '.'];
  #print segment_folder_list
  unlinked_segments = list();
  mv_segments = list()
  for folder in segment_folder_list:
      config = yaml.load(open(folder + "/segmentconfig.yaml", "r"))
      if config["VertexTypeId"] in deleted_typeid_list:
          segid = folder.split("/")[-1]
          pstr = "Found deleted segment \'%3s\': " %(segid)
          if os.path.islink(folder):
              linkto = os.readlink(folder)
              print "%s [SOLVED] Unlink with path: %s" %(pstr, linkto)
              os.unlink(folder)
              unlinked_segments.append([folder, linkto])
          else:
              print "%s [SOLVED] mv to path: %s" % (pstr, dest + segid)
              shutil.move(folder, dest + segid)
              mv_segments.append([folder, dest + segid])
  return unlinked_segments, mv_segments

def CleanUp():
  deletedlist = ReadDeletedTypeList();
  print "The deleted type id list: %r" %deletedlist
  unlinked_segments, mv_segments = Clean(deletedlist);
  #print unlinked_segments
  #print mv_segments
  if len(unlinked_segments) > 0:
    unlinklist = ['|'.join(x) for x in unlinked_segments]
    with open(gstore_path + "/unlink_info", 'w') as myfile:
        myfile.write('\n'.join(unlinklist))
  else:
    print "No dropped segment found with symlinked folder."
  if len(mv_segments) > 0:
    mvlist = ['|'.join(x) for x in mv_segments]
    with open(gstore_path + "/mv_info", 'w') as myfile:
        myfile.write('\n'.join(mvlist))
  else:
    print "No dropped segment found with regluar folder."

def Recover():
  with open(gstore_path + "/unlink_info", 'r') as myfile:
      data = myfile.read()
      if len(data) > 0 :
        print "Recover from unlink_info"
        datalist = data.split("\n")
        for line in datalist:
          path = line.split('|')
          if len(path) == 2:
            print "create link \'%s\' to \'%s\'" %(path[0], path[1])
            os.symlink(path[1], path[0])
          else:
            print "Got wrong info from line: \'%s\' with size = %d" %(line, len(path))
  with open(gstore_path + "/mv_info", 'r') as myfile:
      data = myfile.read()
      if len(data) > 0 :
        print "Recover from mv_info"
        datalist = data.split("\n")
        for line in datalist:
          path = line.split('|')
          if len(path) == 2:
            print "mv folder from \'%s\' to \'%s\'" %(path[1], path[0])
            shutil.move(path[1], path[0])
          else:
            print "Got wrong info from line: \'%s\' with size = %d" %(line, len(path))

arglist =  sys.argv
if len(arglist) == 1:
  CleanUp()
elif arglist[1] == "-r":
  Recover()
