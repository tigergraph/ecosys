#!/usr/bin/env python3
import boto3
from pathlib import Path
import argparse
import os


parser = argparse.ArgumentParser(description='Download one partition of data from AWS S3 bucket.')
parser.add_argument('index', type=int, help='index of the node')
parser.add_argument('nodes', type=int, help='the total number of nodes')
args = parser.parse_args()

buckets = 'ldbc-snb-datagen-bi-2021-07'
roots = 'results/sf30000-compressed/runs/20210728_061923/social_network/csv/bi/composite-projected-fk/'
targets = Path('sf30k')

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
d1 ='initial_snapshot'
jobs = []
for d2 in ['static', 'dynamic']:
  for name in NAMES[d2]:
    loc = '/'.join([d1,d2,name])
    prefix = root + loc
    target_dir = target / loc
    target_dir.mkdir(parents=True, exist_ok=True)

s3 = boto3.client("s3")
def get_objects(bucket, prefix, suffix):
  paginator = s3.get_paginator("list_objects_v2")
  for page in paginator.paginate(Bucket=bucket, Prefix=prefix, RequestPayer='requester'):
    try:
      contents = page["Contents"]
    except KeyError:
      break
    for obj in contents:
      key = obj["Key"]
      if key.endswith(suffix):
        yield obj

i = -1
ii = 0
for obj in get_objects(bucket, root+d1, '.csv.gz'):
  key = obj["Key"]
  if not key.endswith('.csv.gz'): continue
  i += 1
  if i % 40 != 0: continue
  if ii == 0:
    ii = key.find('composite-projected-fk') + len('composite-projected-fk') + 1
  fn = str(target/key[ii:])
  print(fn)
  s3.download_file(bucket, key, Filename=fn, ExtraArgs={'RequestPayer':'requester'})
