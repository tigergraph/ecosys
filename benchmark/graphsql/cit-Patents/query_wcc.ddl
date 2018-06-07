// Algorithm comes from https://graphsql.atlassian.net/wiki/display/GRAP/Connected+Components
// Changes: (1) Code is modified to make it compile; (2) only return the number of wcc

drop query wcc

create query wcc() for graph snap_graph {
  SumAccum<int>    @@group_cnt = 0;
  int    loop_count = 0;
  MinAccum<int>    @cc_id;
  //MinAccum<bool>    @active = true;
  OrAccum<bool> @changed_group= false;
 
  Start = {MyNode.*};
  @@group_cnt = Start.size();
  Start = select x from Start:x accum x.@cc_id = uid_to_vid(x); 
 
  while (Start.size()>0) do
     Start= select y from Start:x - (MyEdge:e)-> :y
          where x.@cc_id < y.@cc_id
     accum y.@cc_id += x.@cc_id
     post-accum if (y.@changed_group ==false) then 
        @@group_cnt += -1, y.@changed_group+=true
     end;

     loop_count += 1;
  end;
  Print @@group_cnt, loop_count;
}

install query wcc
