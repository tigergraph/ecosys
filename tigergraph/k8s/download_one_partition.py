#!/usr/bin/env python3
from google.cloud import storage
from pathlib import Path
import argparse
from multiprocessing import Pool, cpu_count
import os
parser = argparse.ArgumentParser(description='Download one partition of data from GCS bucket.')
parser.add_argument('data',  type=int, choices=[100, 300, 1000, 3000, 10000, 30000], help='data scale factor.')
parser.add_argument('index', type=int, help='index of the node')
parser.add_argument('nodes', type=int, help='the total number of nodes')
parser.add_argument('--thread','-t', type=int, default=4, help='number of threads')
parser.add_argument('--key','-k', type=str, default=None, help='service key file')
parser.add_argument('--target', type=Path, default=Path('~/tigergraph/data').expanduser(), help='target diretory to download')
args = parser.parse_args()

if args.key:
  os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = args.key

bucket = 'ldbc_bi'
root = f'sf{args.data}/'
target = args.target / f'sf{args.data}'

PARTITION_OR_NOT = {
  'initial_snapshot': True,
  'inserts': True,
  'deletes': False,}

STATIC_NAMES = [
  'Organisation',
  'Organisation_isLocatedIn_Place',
  'Place',
  'Place_isPartOf_Place',
  'Tag',
  'TagClass',
  'TagClass_isSubclassOf_TagClass',
  'Tag_hasType_TagClass',
]
DYNAMIC_NAMES = [
  'Comment',
  'Forum',
  'Person',
  'Post',
  'Comment_hasCreator_Person',
  'Comment_hasTag_Tag',
  'Comment_isLocatedIn_Country',
  'Comment_replyOf_Comment',
  'Comment_replyOf_Post',
  'Forum_containerOf_Post',
  'Forum_hasMember_Person',
  'Forum_hasModerator_Person',
  'Forum_hasTag_Tag',
  'Person_hasInterest_Tag',
  'Person_isLocatedIn_City',
  'Person_knows_Person',
  'Person_likes_Comment',
  'Person_likes_Post',
  'Person_studyAt_University',
  'Person_workAt_Company',
  'Post_hasCreator_Person',
  'Post_hasTag_Tag',
  'Post_isLocatedIn_Country',
]
NAMES = {'static':STATIC_NAMES, 'dynamic':DYNAMIC_NAMES}

client = storage.Client()  
jobs = []
d1 ='initial_snapshot'
for d2 in ['static', 'dynamic']:
  for name in NAMES[d2]:
    loc = '/'.join([d1,d2,name]) + '/'
    prefix = root + loc
    target_dir = target / loc
    target_dir.mkdir(parents=True, exist_ok=True)
    i = -1
    for blob in client.list_blobs(bucket, prefix=prefix):
      blob_name = blob.name
      if not blob_name.endswith('.csv.gz'): continue
      i += 1
      if PARTITION_OR_NOT[d1] and i % args.nodes != args.index: continue
      if name=='Comment': print(name, i)
      csv = blob_name.rsplit('/',1)[-1]

      if args.thread > 1:
        jobs.append((blob_name, target_dir/csv))
      else:
        blob.download_to_filename(target_dir/csv)

for d1 in ['inserts','deletes']:
  d2 = 'dynamic'
  for name in NAMES[d2]:
    loc = '/'.join([d1,d2,name]) + '/'
    prefix = root + loc
    i = -1
    for blob in client.list_blobs(bucket, prefix=prefix):
      blob_name = blob.name
      if not blob_name.endswith('.csv.gz'): continue
      i += 1
      if PARTITION_OR_NOT[d1] and i % args.nodes != args.index: continue
      batch, csv = blob_name.rsplit('/',2)[-2:]
      if name=='Comment': print(d1, name, batch, i)
      target_dir = target / loc / batch
      target_dir.mkdir(parents=True, exist_ok=True)
      if args.thread > 1:
        jobs.append((blob_name, target_dir/csv))
      else:
        blob.download_to_filename(target_dir/csv)
        
if args.thread > 1:
  #print('download to ', str(target))
  print(f'start downloading {len(jobs)} files ...')
  njobs = args.thread * 5
  jobs2 = [[] for i in range(njobs)]
  for i,job in enumerate(jobs):
    jobs2[i%njobs].append(job)

  def download(jobs):
    client = storage.Client()  
    gcs_bucket = client.bucket(bucket)
    for job in jobs:
      blob_name, target = job
      gcs_bucket.blob(blob_name).download_to_filename(target)


  with Pool(processes=args.thread) as pool:
    pool.map(download,jobs2)
  print("downloading is done")

# download parameters
print("download parameters")
client = storage.Client()  
gcs_bucket = client.bucket('ldbc_bi')
blobs = gcs_bucket.list_blobs(prefix=f'parameters-sf{args.data}/')  # Get list of files
for blob in blobs:
    if blob.name.endswith("/"):
      continue
    file_split = blob.name.split("/")
    directory = "/".join(file_split[0:-1])
    Path(directory).mkdir(parents=True, exist_ok=True)
    blob.download_to_filename(blob.name) 
print("download parameters done")