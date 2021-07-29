from multiprocessing import Pool, cpu_count
import gzip
from pathlib import Path

npart = 40
target = Path('~/sf30k/inserts').expanduser()
files = list(target.glob('**/*.csv.gz'))
#print(files[:3],len(files))

def split(f):
  with gzip.open(f, 'rt') as fin:
    outputs = [open(str(f).replace('.csv.gz',f'_{n:02d}.csv'),'w') for n in range(npart)]
    print(str(f))
    for i, row in enumerate(fin):
      outputs[i%npart].write(row)
    for fo in outputs: fo.close()
  f.unlink()

with Pool(processes=cpu_count()) as pool:
  pool.map(split, files)