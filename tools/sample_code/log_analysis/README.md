restpp_log_paser.py read the INFO log of RESTPP and collect the following info
1) the total number of incoming /query to restpp server
2) query distribution message (which gpe was sent, require debug message of zeromqwriter)
3) query return message
4) query timeout message
5) calculate the response time for each query including returned query and timeout query

To use this script, run the following command:
python restpp_log_parser.py /home/tigergraph/tigergraph/logs/RESTPP_1_1/log.INFO "I0303 22:25"

Two input parameter, restpp INFO log path and time pattern to search for time related log

The output of restpp_log_parser.py
```
The input restpp log path:'/home/tigergraph/tigergraph/logs/RESTPP_1_1/log.INFO'
The input time range pattern: '0303 22:25'

Time range match found 18/212 matched lines with querylines: 5 | returnlines: 8 | timeoutlines: 0 | msglines: 5, the last searched line:
	'I0303 22:26:28.469715 19578 connectionpool.cpp:126] Engine_ConnectionPool| new_request: 0, epoll_wait: 1, reused_request: 1'

Query distribution: 
{
    "get_requestQ_GPE_1Q_1": 2, 
    "get_requestQ_GPE_2Q_1": 1, 
    "get_requestQ_GPE_3Q_1": 1, 
    "get_requestQ_GPE_4Q_1": 1
}

The excuted query has the following response time:
	 MAX diff: 726.450928 ms, MIN diff: 0.175049 ms

The detailed log are stored at: /tmp/query_status.txt1583279760.58
```
The detailed log has the following info
```
head /tmp/query_status.txt1583279760.58
target,starttime,senttime,endtime,responsetime(ms),rid,timeout,birthepoc,url
get_requestQ_GPE_1Q_1,2020-03-03T22:25:25.947576,2020-03-03T22:25:25.947831,2020-03-03T22:25:26.609369,662,2.RESTPP_1_1.1583274325947.N,False,1583274325947,"/query/structure_graph/distinct_items_like?SI=k$fNfejdAaLUgD&"
get_requestQ_GPE_2Q_1,2020-03-03T22:25:27.638481,2020-03-03T22:25:27.638595,2020-03-03T22:25:28.260943,622,3.RESTPP_1_1.1583274327638.N,False,1583274327638,"/query/structure_graph/distinct_items_like?SI=k$fNfejdAaLUgD&"
get_requestQ_GPE_3Q_1,2020-03-03T22:25:29.026654,2020-03-03T22:25:29.026772,2020-03-03T22:25:29.698482,672,6.RESTPP_1_1.1583274329026.N,False,1583274329026,"/query/structure_graph/distinct_items_like?SI=k$fNfejdAaLUgD&"
get_requestQ_GPE_4Q_1,2020-03-03T22:25:30.486021,2020-03-03T22:25:30.486150,2020-03-03T22:25:31.212451,726,7.RESTPP_1_1.1583274330486.N,False,1583274330486,"/query/structure_graph/distinct_items_like?SI=k$fNfejdAaLUgD&"
get_requestQ_GPE_1Q_1,2020-03-03T22:25:32.062186,2020-03-03T22:25:32.062302,2020-03-03T22:25:32.670125,608,8.RESTPP_1_1.1583274332062.N,False,1583274332062,"/query/structure_graph/distinct_items_like?SI=k$fNfejdAaLUgD&"
```
