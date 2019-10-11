import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np
import sys

def rgb2gray(rgb):
    r, g, b = rgb[:,:,0], rgb[:,:,1], rgb[:,:,2]
    gray = (0.2989 * r + 0.5870 * g + 0.1140 * b)/256
    return gray

def crop(image):
    h, w = image.shape
    if h > w:
        h_start = (h-w)//2
        h_end = h_start + w
        w_start = 0
        w_end = w
    else:
        h_start = 0
        h_end = h
        w_start = (w - h) // 2
        w_end = w_start + h
    res = image[h_start:h_end,w_start:w_end]
    return res


def resize(image, toH, toW):
    h, w = image.shape
    block_H, block_W = h//toH, w//toW
    res = np.zeros((toH, toW))
    for i in range(0, toH):
        for j in range(0, toW):
            cnt = 0
            avg = 0
#            Min = 0
            for ii in range(0,block_H):
                for jj in range(0,block_W):
                    cnt += 1
                    avg += image[i*block_H+ii,j*block_W+jj]
#                    Min = min(Min, image[i*block_H+ii,j*block_W+jj])
            avg /= cnt
            res[i,j] = avg
    return res


inputfile = sys.argv[1]
basename = inputfile[0: inputfile.find('.')]
RGB = mpimg.imread(inputfile)
Gray = rgb2gray(RGB)

Gray_crop = crop(Gray)

Gray_20 = np.ones((20,20)) - resize(Gray_crop, 20, 20)

Gray_20 = np.transpose(Gray_20)

# imgplot = plt.imshow(Gray_20)
# plt.show()

drawing = np.reshape(Gray_20, (1, 400))

filename = basename+'.csv'
with open(filename, "w") as file:
    file.write('v_Val_id,dataX\n')
    for i in range(400):
        file.write(str(i+1)+','+str(drawing[0,i])+'\n')
