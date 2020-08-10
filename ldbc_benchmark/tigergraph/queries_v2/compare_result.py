import json
import ast
import os
import argparse

variable_list={
'is1': ['firstName','lastName','birthday','locationIP','browserUsed','cityId','gender','creationDate'],
'is2': ['messageId','messageContent','messageCreationDate','originalPostId',
         'originalPostAuthorId','originalPostAuthorFirstName','originalPostAuthorLastName'],
'is3': ['personId','firstName','lastName','friendshipCreationDate'],
'is4': ['messageCreationDate','messageContent'],
'is5': ['personId','firstName','lastName'],
'is6': ['forumId','forumTitle','moderatorId','moderatorFirstName','moderatorLastName'],
'is7': ['commentId','commentContent','commentCreationDate','replyAuthorId',
         'replyAuthorFirstName','replyAuthorLastName','replyAuthorKnowsOriginalMessageAuthor'],

'ic1': ['friendId','friendLastName','distanceFromPerson',
        'friendBirthday', 'friendCreationDate', 'friendGender',
        'friendBrowserUsed','friendLocationIp', #'friendEmails', ,'friendLanguages',
        'friendCityName','friendUniversities','friendCompanies'],
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
'ic14':['personIdsInPath', 'pathWeight'],


'bi1': ['theYear','isComment','lengthCategory','messageCount',
         'averageMessageLength','sumMessageLength','percentageOfMessages'],
'bi2': ['countryName','messageMonth','personGender','ageGroup','tagName','messageCount'],
'bi3': ['tagName','countMonth1','countMonth2','diff'],
'bi4': ['forumId','forumTitle','forumCreationDate','personId','postCount'],
'bi5': ['id','firstName','lastName','creationDate','postCount'],
'bi6': ['personId','replyCount','likeCount','messageCount','score'],
'bi7': ['personId','authorityScore'],
'bi8': ['relatedTagName','replyCount'],
'bi9': ['forumId','count1','count2'],
'bi10':['personId','score','friendsScore'],
'bi11':['personId', 'tagName', 'likeCount', 'replyCount'],
'bi12':['messageId', 'messageCreationDate','creatorFirstName','creatorLastName','likeCount'],
'bi13':['year', 'month','popularTags'],
'bi14':['personId','personFirstName','personLastName','threadCount','messageCount'],
'bi15':['personId','count_'], 
'bi16':['personId','tagName','messageCount'],
'bi17':[],
'bi18':['messageCount','personCount'], 
'bi19':['personId', 'strangerCount', 'interactionCount'],
'bi20':['tagClassName','messageCount'],
'bi21':['zombieId','zombieLikeCount','totalLikeCount','zombieScore'], 
'bi22':['person1Id','person2Id','city1Name','score'], 
'bi23':['messageCount','destinationName','month'], 
'bi24':['messageCount','likeCount','year','month','continentName'], 
'bi25':['personIdsInPath', 'pathWeight'],
}

parser = argparse.ArgumentParser(description='Parse the results to python str and compare to target result')
parser.add_argument('-q','--queries', default='all', help='queries to parse and compare (default: all). example: -q ic1,ic2 -q ic')
parser.add_argument('-c','--compare', default=None,help='folder of target results to compare (default: None). example: -c result/SF10000')
parser.add_argument('-l','--log', default='log/', help='folder of the current results (default: log)')
parser.add_argument('-e','--err', default='err/', help='folder of the running time (default: err)')
parser.add_argument('-s','--save',default='parsed_result', help='folder to save the parsed format of current results (default: parsed_result)')
parser.add_argument('-sc','--save_compare',default=None, help='folder to save the parsed target results (default: None)')
parser.add_argument('--old', action='store_true', help='True if the target results to compare is in the old JSON format')
args = parser.parse_args()
'''
parse the -q/--query option
ic,is,bi for one category of queries
queries are separated by comma, i.e., "ic1,ic2"
'''
q_list = {"is":['is'+str(i+1) for i in range(7)],
"ic": ['ic'+str(i+1) for i in range(14)],
"bi": ['bi'+str(i+1) for i in range(25)]}
if args.queries == 'all':
    qs = q_list["is"] + q_list["ic"] + q_list["bi"]
elif  args.queries in ["ic","is","bi"]:
    qs = q_list[args.queries]
else:
    qs = args.queries.split(',')

'''
for ic1 friendEmails and friendLanguages are sets, 
friendUniversities and friendCompanies are sets of dict 
and are stored as storted list of list
'''
def modify_table(table,q):
    if q == 'ic1':
        vlist = variable_list['ic1']
        for row in table:
            for v in ['friendEmails','friendLanguages']:
                if v in vlist:
                    i=vlist.index(v)
                    row[i]=set(row[i])
            for v in ['friendUniversities','friendCompanies']:
                if v in vlist:
                    i=vlist.index(v)
                    row[i] = [[u['orgName'],u['orgYear'],u['orgPlace']] for u in row[i]]
                    row[i].sort()
    if q == 'ic12':
        for row in table:
            row[3]=set(row[3])

def node2table(rows, variable):
    table = []
    for r in rows:
        if 'attributes' in r.keys(): r = r['attributes']
        row = [r[v] for v in variable]        
        table += [row]
    return table
        
def txt2table(q):
    filename = os.path.join(args.compare, q)
    if not os.path.isfile(filename): 
        print ('No benchmark for {}'.format(q)) 
        return None
    with open(filename,'r',encoding='utf-8') as f:
        lines = f.readlines()[2:]
        txt = ''.join(lines).replace('\n','')
        res = ast.literal_eval(txt)
        if len(res)==1:
            (_, res), = res[0].items()
            if isinstance(res,int): return res
        table = node2table(res,variable_list[q])
        modify_table(table,q) #ic1 has a dictionary to parse and order
        return table
        
def log2table(q):
    filename = os.path.join(args.log, q)
    if not os.path.isfile(filename): 
        print ('No result for {}'.format(q)) 
        return None
    with open(filename,'r',encoding='utf-8') as f:
        try:
            res = json.load(f)['results'][0]
        except:
            print("{}:invalid JSON file".format(filename))
            return
        (_, res), = res.items()
        if isinstance(res,int): return res
        table = node2table(res,variable_list[q])
        modify_table(table,q) #ic1 has a dictionary to parse and order
        return table
 
def write_table(table, filename):    
    with open(filename,'w',encoding='utf-8') as f:
        if isinstance(table,int): 
            f.write(str(table))
            return
        f.write('\n'.join([str(row) for row in table]))

def read_table(q):    
    filename = os.path.join(args.compare, q)
    with open(filename,'r',encoding='utf-8') as f:
        lines = f.readlines()
        table = [ast.literal_eval(l) for l in lines]
        if len(table)==1 and isinstance(table[0],int): return table[0]
        return table

def compare_table(table1, table2, nprint=1):
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
        time_list = []
        for l in f:
            if l.startswith('real'):
                time = float(l.split(' ')[1].replace('\n',''))
                time_list.append(time)
        return sorted(time_list)[len(time_list)//2+1]
    

if args.save_compare and not os.path.exists(args.save_compare):
    os.mkdir(args.save_compare)   
if not os.path.exists(args.save):
    os.mkdir(args.save)

'''
Main function
'''     
for q in qs:
    print(q, end=':')
    # if -c/--compare is specified, parse the target results to table1
    if args.compare: 
        table1 = read_table(q) if not args.old else txt2table(q) 
    else:
        table1 = None
    # parse the current results to table2
    table2 = log2table(q)
    
    # compare the results
    if table1:
        compare_table(table1,table2)
        if args.save_compare:
        	write_table(table1, os.path.join(args.save_compare, q))
    
    # print the running time for queries 
    if table2:
        err_file = os.path.join(args.err, q)
        time = err2time(err_file)
        print('time:{}s'.format(time))
        result_file = os.path.join(args.save, q)
        # save the parsed format of current results
        write_table(table2, result_file)
