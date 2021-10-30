import argparse
import time
import subprocess
import paramiko
from scp import SCPClient
from pathlib import Path
from google.cloud import storage
import os


parser = argparse.ArgumentParser(description='Download and uncompress the data on all the nodes.')
parser.add_argument('data', type=str, choices=['1k', '10k', '30k'], help='data scale factor')
parser.add_argument('ip', type=str, help='starting ip address')
parser.add_argument('nodes', type=int, help='the number of nodes')
parser.add_argument('--key','-k', type=str, default=None, help='location of the service key json file')
parser.add_argument('--thread','-t', type=int, default=1, help='number of threads for each node')
parser.add_argument('--parts','-p', type=int, default=0, help='number of parts to split the data (0 means the same as node number)')
parser.add_argument('--start','-s', type=int, default=0, help='the start index of the data partition (default 0)')
args = parser.parse_args()

user = "tigergraph"
pin = "tigergraph"
workdir = '/home/tigergraph'
def createSSHClient(server, port, user, password):
    client = paramiko.SSHClient()
    client.load_system_host_keys()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(server, port, user, password)
    return client

buckets = {
    '1k': 'ldbc_snb_1t',
    '10k': 'ldbc_snb_10t',
    '30k': 'ldbc_snb_30t',}
roots = {
  '1k': 'sf1k/',
  '10k':'v1/results/sf10000-compressed/runs/20210713_203448/social_network/csv/bi/composite-projected-fk/',
  '30k':'results/sf30000-compressed/runs/20210728_061923/social_network/csv/bi/composite-projected-fk/'}

def main():
  key = ''
  if args.key:
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = args.key
    key = f'-k {args.key}'
  print("check data accessibility")
  storage_client = storage.Client()  
  bucket_name = 'my_bucket_name'
  bucket = storage_client.bucket(buckets[args.data])
  stats = storage.Blob(bucket=bucket, name=roots[args.data]).exists(storage_client)
  print("The bucket can be accessed")
  
  
  start_ip = args.ip.split('.')
  for i in range(args.nodes):
    ip4 = int(start_ip[-1]) + i
    ip = start_ip[:-1] + [str(ip4)]
    ip = '.'.join(ip)
    ssh = createSSHClient(ip, 22, user, pin)
    scp = SCPClient(ssh.get_transport())
    target = f'/home/tigergraph/sf{args.data}'
    print(f'logging to {ip}')
    scp.put('download_one_partition.py', workdir)
    scp.put('download_decompress.sh', workdir)
    if args.key:
      scp.put(args.key, workdir)
    
    stdin, stdout, stderr = ssh.exec_command(f''' 
      cd {workdir}
      . .profile
      pip3 install google-cloud-storage 
      export data={args.data}
      export index={i + args.start}
      export nodes={args.parts if args.parts else args.nodes}
      export target={target}
      export thread={args.thread}
      export key="{key}"
      nohup sh download_decompress.sh > log.download 2>&1 < /dev/null &  
    ''')
    time.sleep(4)
    stdin, stdout, stderr = ssh.exec_command(f'tail {workdir}/log.download')
    for line in stdout.read().splitlines():
      print(line.decode('utf-8'))
    
    ssh.close()
    scp.close()  

if __name__ == '__main__':
  main()
