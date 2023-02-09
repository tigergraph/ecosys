import argparse
import time
import subprocess
import paramiko
from scp import SCPClient
from pathlib import Path
from google.cloud import storage
import os


parser = argparse.ArgumentParser(description='Download and uncompress the data on all the nodes.')
parser.add_argument('data',  type=int, choices=[100, 300, 1000, 3000, 10000, 30000], help='data scale factor.')
parser.add_argument('ip', type=str, help='either an starting ip with number of nodes "ip_start:nodes" or a file of IP list')
parser.add_argument('--key','-k', type=str, default=None, help='location of the service key json file')
parser.add_argument('--thread','-t', type=int, default=1, help='number of threads for each node')
parser.add_argument('--parts','-p', type=int, default=0, help='number of parts to split the data (0 means the same as node number)')
parser.add_argument('--start','-s', type=int, default=0, help='the start index of the data partition (default 0)')
args = parser.parse_args()

user = "tigergraph"
pin = "tigergraph" # please change the pin here
workdir = '/home/tigergraph'
bucket_name = 'ldbc_bi'
root = f'sf{args.data}/'

def createSSHClient(server, port, user, password):
    client = paramiko.SSHClient()
    client.load_system_host_keys()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(server, port, user, password)
    return client

def main():
  key = ''
  if args.key:
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = args.key
    key = f'-k {args.key}'
  print("check data accessibility")
  storage_client = storage.Client()  
  bucket = storage_client.bucket(bucket_name)
  stats = storage.Blob(bucket=bucket, name=root).exists(storage_client)
  print("The bucket can be accessed")
  
  ip_list = []
  if ":" in args.ip:
    # args.ip is "start_ip:nodes" 
    start, nodes = args.ip.split(":")
    start_ip = start.split('.')
    for i in range(nodes):
      ip4 = int(start_ip[-1]) + i
      ip = start_ip[:-1] + [str(ip4)]
      ip_list.append('.'.join(ip))
  else:
    # args.ip is a file of ips
    with open(args.ip,'r') as f:
      for ip_str in f:
        if len(ip_str.split('.')) != 4: continue
        ip_list.append(ip_str.strip())
    
  for i,ip in enumerate(ip_list):
    ssh = createSSHClient(ip, 22, user, pin)
    scp = SCPClient(ssh.get_transport())
    print(f'logging to {ip}')
    scp.put('../k8s/download_one_partition.py', workdir)
    scp.put('../k8s/download_decompress.sh', workdir)
    if args.key:
      scp.put(args.key, workdir)
    
    stdin, stdout, stderr = ssh.exec_command(f''' 
      cd {workdir}
      . .profile
      pip3 install google-cloud-storage 
      export SF={args.data}
      export i={i + args.start}
      export NUM_NODES={args.parts if args.parts else len(ip_list)}
      export target=~
      export DOWNLOAD_THREAD={args.thread}
      export SERVICE_KEY="{key}"
      nohup sh download_decompress.sh $i $target > log.download 2>&1 < /dev/null &  
    ''')
    time.sleep(4)
    stdin, stdout, stderr = ssh.exec_command(f'tail {workdir}/log.download')
    for line in stdout.read().splitlines():
      print(line.decode('utf-8'))
    
    ssh.close()
    scp.close()  

if __name__ == '__main__':
  main()
