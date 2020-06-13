import json
import ast
import os
import argparse

variable_list={
'ic1': ['friendId','friendLastName','distanceFromPerson',
        'friendBirthday', 'friendCreationDate', 'friendGender',
        'friendBrowserUsed','friendLocationIp', 'friendEmails'
        ,'friendLanguages','friendCityName','friendUniversities','friendCompanies'],
'ic2': ['personId','personFirstName','personLastName','messageId','messageContent','messageCreationDate'],
'ic3': ['personId','personFirstName','personLastName','xCount','yCount','xyCount'],
'ic4': ['tagName','postCount'],
'ic5': ['forumTitle','postCount'],
'ic6': ['tagName','postCount'],
'ic7': ['personId','personFirstName','personLastName','likeCreationDate','commentOrPostId','commentOrPostContent','isNew'],
'ic8': ['personId','personFirstName','personLastName','commentCreationDate','commentId','commentContent'],
'ic9': ['personId','personFirstName','personLastName','messageId','messageContent','messageCreationDate'],
'ic10':['personId','personFirstName','personLastName','commonInterestScore','personGender','personCityName'],
'ic11':['personId','personFirstName','personLastName','organizationName','organizationWorkFromYear'],
'ic12':['personId','personFirstName','personLastName','tagNames','replyCount'],
'ic13':['shortestPathLength'],
#ic_14
'is1': ['firstName','lastName','birthday','locationIP','browserUsed','cityId','gender','creationDate'],
'is2': ['messageId','messageContent','messageCreationDate','originalPostId',
         'originalPostAuthorId','originalPostAuthorFirstName','originalPostAuthorLastName'],
'is3': ['personId','firstName','lastName','friendshipCreationDate'],
'is4': ['messageCreationDate','messageContent'],
'is5': ['personId','firstName','lastName'],
'is6': ['forumId','forumTitle','moderatorId','moderatorFirstName','moderatorLastName'],
'is7': ['commentId','commentContent','commentCreationDate','replyAuthorId',
         'replyAuthorFirstName','replyAuthorLastName','replyAuthorKnowsOriginalMessageAuthor'],

'bi1': ['theYear','isComment','lengthCategory','messageCount',
         'averageMessageLength','sumMessageLength','percentageOfMessages'],
'bi2': ['countryName','messageMonth','personGender','ageGroup','tagName','messageCount'],
'bi3': ['tagName','countMonth1','countMonth2','diff'],
'bi4': ['forumId','forumTitle','forumCreationDate','personId','postCount'],
'bi5': ['id','firstName','lastName','creationDate','postCount'],
'bi6': ['personId','replyCount','likeCount','messageCount','score'],
#bi_7
'bi8': ['relatedTagName','replyCount'],
'bi9': ['forumId','count1','count2'],
'bi10':['it.personId','it.score','it.friendScore'],
'bi11':['personId', 'tagName', 'likeCount', 'replyCount'],
'bi12':['messageId', 'messageCreationDate','creatorFirstName','creatorLastName','likeCount'],
'bi14':['personId','personFirstName','personLastName','threadCount','messageCount'],
'bi15':['personId','count_'], 
'bi16':['personId','tagName','messageCount'],
#bi_17
'bi18':['messageCount','personCount'], 
#bi_19
'bi20':['tagClassName','messageCount'],
'bi21':['zombieId','zombieLikeCount','totalLikeCount','zombieScore'], 
'bi22':['person1Id','person2Id','city1Name','score'], 
'bi23':['messageCount','destinationName','month'], 
'bi24':['messageCount','likeCount','year','month','continentName'], 
}

parser = argparse.ArgumentParser(description='Process the results and compare')
parser.add_argument('--result', default='SF100/')
parser.add_argument('--log', default='log/')
parser.add_argument('--err', default='err/')
parser.add_argument('--queries', 
  default='ic1,ic2,ic3,ic4,ic5,ic6,ic7,ic8,ic9,ic10,ic11,ic12,ic13,bi1,bi2,bi3,bi4,bi5,bi8,bi9,bi10,bi11,bi12,bi14,bi15,bi16,bi17,bi18,bi20,bi22,bi23,is1,is2,is3,is4,is5,is6')

args = parser.parse_args()

q_ic = ['ic'+str(i+1) for i in range(13)]
q_is = ['is'+str(i+1) for i in range(7)]
q_bi = ['ic'+str(i+1) for i in range(25)]
if args.queries == 'all':
    qs = q_ic+q_is+q_bi
elif  args.queries == 'ic':
    qs = q_ic
elif  args.queries == 'is':
    qs = q_is
elif  args.queries == 'bi':
    qs = q_bi
else:
    qs = args.queries.split(',')

def node2table(rows, variable):
    table = []
    for r in rows:
        if 'attributes' in r.keys(): r = r['attributes']
        row = [r[v] for v in variable]        
        table += [row]
    return table

        
def txt2table(q):
    filename = os.path.join(args.result, q)
    if not os.path.isfile(filename): 
        print ('No results for {}'.format(q)) 
        return None
    with open(filename,'r',encoding='utf-8') as f:
        lines = f.readlines()[2:]
        txt = ''.join(lines).replace('\n','')
        res = ast.literal_eval(txt)
        if len(res)==1:
            (_, res), = res[0].items()
            if isinstance(res,int): return res
        return node2table(res,variable_list[q])

def log2table(q):
    filename = os.path.join(args.log, q)
    with open(filename,'r',encoding='utf-8') as f:
        try:
            res = json.load(f)['results'][0]
        except:
            print("{}:invalid JSON file".format(filename))
            return
        (_, res), = res.items()
        if isinstance(res,int): return res
        return node2table(res,variable_list[q])

def write_table(table, filename):    
    with open(filename,'w',encoding='utf-8') as f:
        if isinstance(table,int): 
            f.write(str(table))
            return
        f.write('\n'.join([str(row) for row in table]))

def read_table(filename):    
    with open(filename,'r',encoding='utf-8') as f:
        lines = f.readlines()
        table = [ast.literal_eval(l) for l in lines]
        return table

def compare_table(table1, table2, nprint=1):
    if table1 is None or table2 is None: return
    if isinstance(table1,int) and isinstance(table2,int):
        if table1 == table2: 
            print("PASS")
        else:
            print("fail")
        return
    
    if len(table1) != len(table2): 
        print("Fail: number of rows {} != {}".format(len(table1),len(table2)))
        return
    error = 0
    for i,(r1,r2) in enumerate(zip(table1,table2)):
        for j,(c1,c2) in enumerate(zip(r1,r2)):
            if c1 != c2: 
                error += 1
                print("Fail: ({},{}) {} != {}".format(i,j,c1,c2))
                if error >= nprint: return
    if error==0: print("PASS")


def err2time(err_file):
    with open(err_file,'r') as f:
        mintime = 10000.0
        for l in f:
            if l.startswith('real'):
                mintime = min(mintime, float(l.split(' ')[1].replace('\n','')))
        return mintime
    
'''
for ic_1 friendEmails and friendLanguages are sets, 
friendUniversities and friendCompanies are sets of dict 
and are stored as storted list of list
'''
def parse_table_ic1(table):
    for row in table:
        row[8]=set(row[8])
        row[9]=set(row[9])
        if 'orgName' in row[11][0].keys():
            row[11] = [[u['orgName'],u['orgYear'],u['orgPlace']] for u in row[11]]
            row[12] = [[u['orgName'],u['orgYear'],u['orgPlace']] for u in row[12]]
        else:
            row[11] = [[u['univName'],u['classYear'],u['cityName']] for u in row[11]]
            row[12] = [[u['comName'],u['workFrom'],u['countryName']] for u in row[12]]
        row[11].sort()
        row[12].sort()



output = 'result'
if not os.path.exists(output):
    os.mkdir(output)
        
for q in qs:
    print(q, end=':')
    table1 = txt2table(q)
    table2 = log2table(q)
    if q == 'ic_1':
        parse_table_ic1(table1)
        parse_table_ic1(table2)
        
    compare_table(table1,table2)
    err_file = os.path.join(args.err, q)
    time = err2time(err_file)
    print('time:{}s'.format(time))
    result_file = os.path.join(output, q)
    write_table(table2, result_file)
    #table3=read_table(result_file)