import argparse
import time
import subprocess
import paramiko
from scp import SCPClient
from pathlib import Path
from google.cloud import storage
import os


parser = argparse.ArgumentParser(description='Download and uncompress the data on all the nodes.')
parser.add_argument('ip', type=str, help='starting ip address')
parser.add_argument('nodes', type=int, help='the number of nodes')
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

def main():
  start_ip = args.ip.split('.')
  for i in range(args.nodes):
    ip4 = int(start_ip[-1]) + i
    ip = start_ip[:-1] + [str(ip4)]
    ip = '.'.join(ip)
    ssh = createSSHClient(ip, 22, user, pin)
    scp = SCPClient(ssh.get_transport())
    print(f'logging to {ip}')
    stdin, stdout, stderr = ssh.exec_command(f''' 
      cd {workdir}
      pkill -f "download_one_partition"
      nohup rm -r sf30k > log.clean 2>&1 < /dev/null &  
    ''')
    time.sleep(4)
    stdin, stdout, stderr = ssh.exec_command(f'tail {workdir}/log.clean')
    for line in stdout.read().splitlines():
      print(line.decode('utf-8'))
    
    ssh.close()
    scp.close()  

if __name__ == '__main__':
  main()
