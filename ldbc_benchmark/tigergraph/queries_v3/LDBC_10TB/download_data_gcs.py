#!/usr/bin/env python3
from google.cloud import storage
from pathlib import Path
import argparse


parser = argparse.ArgumentParser(description='Download one partition of initial_snapshot/ and inserts/ and whole data set of deletes/.')
parser.add_argument('index', type=int, help='index of the node')
parser.add_argument('nodes', type=int, help='the total number of nodes')
parser.add_argument('--bucket', '-b', type=str, default='ldbc_snb_10k' ,help='bucket to download ldbc snb data from')
parser.add_argument('--target', '-t', type=Path, default=Path('sf10000_2'), help='target directory')
parser.add_argument('--root', '-r', type=str, default='v1/results/sf10000-compressed/runs/20210713_203448/social_network/csv/bi/composite-projected-fk/', 
  help='path to composite-projected-fk')

args = parser.parse_args()

PARTITION_OR_NOT = {
  'initial_snapshot': True,
  'inserts':False,
  'deletes':False,
}

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
"""
d1 ='initial_snapshot'
for d2 in ['static', 'dynamic']:
  for name in NAMES[d2]:
    loc = '/'.join([d1,d2,name]) + '/'
    prefix = args.root + loc
    target = args.target / loc
    target.mkdir(parents=True, exist_ok=True)
    i = -1
    for blob in client.list_blobs(args.bucket, prefix=prefix):
      if not blob.name.endswith('.csv.gz'): continue
      i += 1
      if PARTITION_OR_NOT[d1] and i % args.nodes != args.index: continue
      csv = blob.name.rsplit('/',1)[-1]
      print(name, i)
      blob.download_to_filename(target/csv)
"""
for d1 in ['inserts','deletes']:
  d2 = 'dynamic'
  for name in NAMES[d2]:
    loc = '/'.join([d1,d2,name]) + '/'
    prefix = args.root + loc
    i = -1
    for blob in client.list_blobs(args.bucket, prefix=prefix):
      if not blob.name.endswith('.csv.gz'): continue
      i += 1
      if PARTITION_OR_NOT[d1] and i % args.nodes != args.index: continue
      batch, csv = blob.name.rsplit('/',2)[-2:]
      print(name, batch, i)
      target = args.target / loc / batch
      target.mkdir(parents=True, exist_ok=True)
      blob.download_to_filename(target/csv)
