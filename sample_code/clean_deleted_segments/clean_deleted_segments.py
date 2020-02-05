import yaml
import re
import sys
import os
import shutil

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
  unlink_info_file = gstore_path + "/unlink_info"
  mv_info_file = gstore_path + "/mv_info"

  if os.path.exists(unlink_info_file):
    with open(unlink_info_file, 'r') as myfile:
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
  if os.path.exists(mv_info_file):
    with open(mv_info_file, 'r') as myfile:
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

def Purge():
  unlink_info_file = gstore_path + "/unlink_info"
  mv_info_file = gstore_path + "/mv_info"
  mv_error_count = 0
  ln_error_count = 0

  if os.path.exists(unlink_info_file):
    with open(unlink_info_file, 'r') as myfile:
      data = myfile.read()
      if len(data) > 0 :
        print "Purge from unlink_info"
        datalist = data.split("\n")
        for line in datalist:
          path = line.split('|')
          if len(path) == 2:
            # get parent: we will remove this segment, not only this version
            parent_dir = os.path.abspath(os.path.join(path[1], os.pardir))
            # make sure the last subdir is a segment ID (int)
            last_dir = parent_dir.rpartition('/')[2]
            if last_dir.isdigit():
                print "Purge %s" %(parent_dir)
                shutil.rmtree(parent_dir)
            else:
                print "Skip invalid directory: %s" %(parent_dir)
                ln_error_count += 1
          else:
            print "Got wrong info from line: \'%s\' with size = %d" %(line, len(path))
            ln_error_count += 1

  if os.path.exists(mv_info_file):
    with open(mv_info_file, 'r') as myfile:
      data = myfile.read()
      if len(data) > 0 :
        print "Purge from mv_info"
        datalist = data.split("\n")
        for line in datalist:
          path = line.split('|')
          if len(path) == 2:
            print "Remove folder %s" %(path[1])
            shutil.rmtree(path[1])
          else:
            print "Got wrong info from line: \'%s\' with size = %d" %(line, len(path))
            mv_error_count += 1

  if ln_error_count == 0 and os.path.exists(unlink_info_file):
      print "Purge linked data finished. Please remove %s" %(unlink_info_file)
  if mv_error_count == 0 and os.path.exists(mv_info_file):
      print "Purge moved data finished.  Please remove %s" %(mv_info_file)


arglist =  sys.argv
if len(arglist) == 1:
  CleanUp()
elif arglist[1] == "-r":
  Recover()
elif arglist[1] == "-p":
  Purge()
