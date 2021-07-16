#!/usr/bin/env python3
import argparse
import subprocess
from pathlib import Path
import math
import os
import subprocess
import paramiko
from scp import SCPClient

main_parser = argparse.ArgumentParser(description='Split data')
main_parser.add_argument('data_dir', type=Path, help='Data directory')

main_parser.add_argument('--target','-t', type=Path, default=Path('~/snb_split'), help='target directory')
args = main_parser.parse_args()

local = True
ips = [f"192.168.99.{100+i}" for i in [2,3,5]]
npart = len(ips) + int(local)
user = "graphsql"
pin = "graphsql"

def createSSHClient(server, port, user, password):
    client = paramiko.SSHClient()
    client.load_system_host_keys()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(server, port, user, password)
    return client

def split(root, dir, target, ssh_clients, scp_clients):
  csvs = list(dir.glob('*.csv'))
  num = len(csvs)
  if num==0: #no csv detected, dig deeper
    for d in dir.glob('*/'):
      split(root, d, target, ssh_clients, scp_clients)
  avg = math.ceil(num/npart) 
  target_dir = target / dir
  print(target_dir)
  if num>0:
    if local: subprocess.run(f'mkdir -p {target_dir}', shell=True)
    for ssh in ssh_clients:
      ssh.exec_command(f'mkdir -p {target_dir}')
  
  if local:
    for i in range(avg):
      csv = csvs[i]
      subprocess.run(f'cp {root/csv} {target_dir}', shell=True)
  for i,scp in enumerate(scp_clients):
    i0 = int(local)
    start = (i+i0)*avg
    end = min(num, (i+i0+1)*avg)
    for csv in csvs[start:end]:
      scp.put(str(root/csv), str(target_dir))
     
def main():
  scp_clients = []
  ssh_clients = []
  for ip in ips:
    ssh = createSSHClient(ip, 22, user, pin)
    ssh_clients.append(ssh)
    scp = SCPClient(ssh.get_transport())
    scp_clients.append(scp)
    
  comp_dir = list(args.data_dir.glob('**/composite-projected-fk/'))[0]
  for f in ['initial_snapshot', 'inserts']:
    root = comp_dir/f
    os.chdir(root)
    split(root, Path('./'), args.target/f, ssh_clients, scp_clients)
  
  root = comp_dir/'deletes'
  subprocess.run(f'cp -r {root} {args.target}', shell=True)
  for ssh in ssh_clients:
    ssh.exec_command(f'chmod 777 /home/graphsql')
    ssh.exec_command(f'chmod -R 777 {args.target}')
  for ssh,scp in zip(ssh_clients, scp_clients): 
    ssh.close()
    scp.close()
  

if __name__ == '__main__':
  main()
