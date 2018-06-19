// This code comes from https://graphsql.atlassian.net/wiki/display/GRAP/Connected+Components
drop query wcc_sequential

create query wcc_sequential() for graph snap_graph{
   SumAccum<int> @@group_count = 0;
   SetAccum<Vertex<MyNode>> @@unvisited;
   SetAccum<Vertex<MyNode>> @@visited;

   OrAccum          @bfs_visited = false;

   Start = {MyNode.*};
   L0 = select v from Start:v
        accum @@unvisited += v;

   //Gquery does not support 
   foreach x:@@unvisited do
      if @@visited.contains(x) then
        continue;
      end;
      //Print x;
      //Print @@visited.size();
      @@group_count += 1;
      Frontier = {x};
      Frontier = select v
          from Frontier:v
          accum v.@bfs_visited = true, @@visited += v;

      while (Frontier.size() > 0) do
          Frontier = select v
              from Frontier:u - (MyEdge:e)-> : v
              where v.@bfs_visited == false
              accum v.@bfs_visited = true, @@visited += v;
      end;
      //Print @@visited.size();
   end;
   Print @@group_count;
}

install query wcc_sequential
