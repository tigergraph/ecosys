import argparse
import time
import subprocess
import paramiko
from scp import SCPClient


parser = argparse.ArgumentParser(description='Download and uncompress the data on all the nodes.')
parser.add_argument('data', type=str, help='the data size. 10t or 30t')
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
  targets = {
  '10t':'sf10k',
  '30t':'sf30k'}
  for i in range(args.nodes):
    ip4 = int(start_ip[-1]) + i
    ip = start_ip[:-1] + [str(ip4)]
    ip = '.'.join(ip)
    ssh = createSSHClient(ip, 22, user, pin)
    scp = SCPClient(ssh.get_transport())
    target = '/home/tigergraph/' + targets[args.data]
    print(f'logging to {ip}')
    scp.put('download_one_partition.py', workdir)
    scp.put('download_decompress.sh', workdir)
    stdin, stdout, stderr = ssh.exec_command(f''' 
      cd {workdir}
      . .profile
      export data={args.data}
      export index={i}
      export nodes={args.nodes}
      export target={target}
      nohup sh download_decompress.sh > foo.out 2>&1 < /dev/null &  
    ''')
    time.sleep(4)
    stdin, stdout, stderr = ssh.exec_command(f'tail {workdir}/foo.out')
    for line in stdout.read().splitlines():
      print(line)
    
    ssh.close()
    scp.close()  

if __name__ == '__main__':
  main()
