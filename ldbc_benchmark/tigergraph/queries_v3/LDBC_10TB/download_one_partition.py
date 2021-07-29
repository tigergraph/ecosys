#!/usr/bin/env python3
from google.cloud import storage
from pathlib import Path
import argparse
from multiprocessing import Pool, cpu_count

parser = argparse.ArgumentParser(description='Download one partition of data from GCS bucket.')
parser.add_argument('data',  type=str, help='the data size. 10t or 30t')
parser.add_argument('index', type=int, help='index of the node')
parser.add_argument('nodes', type=int, help='the total number of nodes')
args = parser.parse_args()

pool = Pool(processes=cpu_count())
client = storage.Client()  
gcs_bucket = client.bucket(bucket)
def download(job):
  blob_name, target = job
  gcs_bucket.blob(blob_name).download_to_filename(target)
  
def download_one_partition(data):
  buckets = {
    '10t': 'ldbc_snb_10k',
    '30t': 'ldbc_snb_30k',}
  roots = {
    '10t':'v1/results/sf10000-compressed/runs/20210713_203448/social_network/csv/bi/composite-projected-fk/',
    '30t':'results/sf30000-compressed/runs/20210728_061923/social_network/csv/bi/composite-projected-fk/'}
  targets = {
    '10t':'sf10k',
    '30t':'sf30k'}
  bucket = buckets[data]
  root = roots[data]
  target = Path(targets[data])

  PARTITION_OR_NOT = {
    'initial_snapshot': True,
    'inserts_split': True,
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
        csv = blob_name.rsplit('/',1)[-1]
        if name=='Comment' and i<100: print(name, i)
        jobs.append((blob_name, target_dir/csv))

  for d1 in ['inserts_split','deletes']:
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
        if name=='Comment' and i<100: print(name, batch, i)
        target_dir = target / loc / batch
        target_dir.mkdir(parents=True, exist_ok=True)
        jobs.append((blob_name, target_dir/csv))

  #print('download to ', str(target))
  print(f'start downloading {len(jobs)} files ...')
  pool.map(download,jobs)
  print("downloading is done")

if __name__ == '__main__':
  download_one_partition(args.data)