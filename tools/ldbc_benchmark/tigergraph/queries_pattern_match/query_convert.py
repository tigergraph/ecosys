import re, os, subprocess, time

DEFAULT_PATH_TO_QUERIES = "/home/tigergraph/ecosys/ldbc_benchmark/tigergraph/queries_pattern_match/"    

fileDir = ""

for i in range(1, 8):        

    fileDir = DEFAULT_PATH_TO_QUERIES + "interactive_short/is_0" + str(i) 
    fileDir += ".gsql"

    f = open(fileDir, "r")

    printThis = 0
    is1st = 0
    res = ""

    for x in f:
    
        if "CREATE" in x:
            is1st = 1
            printThis = 1
        
            res = re.sub('{', 'SYNTAX("v2"){', x)
        elif "INSTALL" in x:
            printThis = 0
        elif printThis == 1:
            res += x
        else:
            pass

    fileName = "./GSQL2/is_" + str(i) + ".gsql"
    f = open(fileName, "x")
    f.write(res)


for i in range(1, 15):

    if i < 10:
        fileDir = DEFAULT_PATH_TO_QUERIES + "interactive_complex/ic_0" + str(i)
    else:
        fileDir = DEFAULT_PATH_TO_QUERIES + "interactive_complex/ic_" + str(i)
    fileDir += ".gsql"
    if os.path.isfile(fileDir):
        f = open(fileDir, "r")

        printThis = 0
        is1st = 0
        res = ""

        for x in f:
    
            if "CREATE" in x:
                is1st = 1
                printThis = 1
                res = re.sub('{', 'SYNTAX("v2"){', x)
            elif "INSTALL" in x:
                printThis = 0
            elif printThis == 1:
                res += x
            else:
                pass
    
        fileName = "./GSQL2/ic_" + str(i) + ".gsql"
        f = open(fileName, "x")
        f.write(res)


for i in range(1, 26):

    if i < 10:
        fileDir = DEFAULT_PATH_TO_QUERIES + "business_intelligence/bi_0" + str(i)
    else:
        fileDir = DEFAULT_PATH_TO_QUERIES + "business_intelligence/bi_" + str(i)
    fileDir += ".gsql"
    
    if os.path.isfile(fileDir):
        f = open(fileDir, "r")
    
        printThis = 0
        is1st = 0
        res = ""
    
        for x in f:
    
            if "CREATE" in x:
                is1st = 1
                printThis = 1
    
                res = re.sub('{', 'SYNTAX("v2"){', x)
            elif "INSTALL" in x:
                printThis = 0
            elif printThis == 1:
                res += x
            else:
                pass
    
        fileName = "./GSQL2/bi_" + str(i) + ".gsql"
        f = open(fileName, "x")
        f.write(res)

