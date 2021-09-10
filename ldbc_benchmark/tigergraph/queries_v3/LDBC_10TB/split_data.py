from multiprocessing import Pool, cpu_count
import gzip
from pathlib import Path

npart = 40
target = Path('~/sf30k/inserts').expanduser()
files = list(target.glob('**/*c000.csv.gz'))
#print(files[:3],len(files))

def split(f):
  with gzip.open(f, 'rb') as fin:
    outputs = [gzip.open(str(f).replace('c000.csv.gz',f'c000_{n:02d}.csv.gz'),'wb') for n in range(npart)]
    print(str(f))
    for i, row in enumerate(fin):
      outputs[i%npart].write(row)
    for fo in outputs: fo.close()
  f.unlink()

with Pool(processes=cpu_count()) as pool:
  pool.map(split, files)