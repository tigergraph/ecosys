fname = raw_input("Enter file name: ")
num_lines = 0
with open(fname, 'r') as f:
    with open(fname+'_idnums', 'w') as outf:
        for line in f:
            outf.write(str(num_lines)+','+line)
            num_lines += 1
print("Number of lines:")
print(num_lines)
