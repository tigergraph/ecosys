drop query ksubgraph
create query ksubgraph(vertex<MyNode> start_node, int depth) for graph snap_graph {

    OrAccum          @visited = false;
    SumAccum<int>    @depth=0;
    SumAccum<int>    @@loop=0;
    SumAccum<int>    @@subgraph_size=0;
    //ListAccum<vertex<MyEdge>> @@sub_graph;

    Start = {start_node};
    Start = select v
        from Start:v
        accum v.@visited = true;

    while (@@loop < depth) do
        Start = select v
            from Start:u - (MyEdge:e)-> : v
            where v.@visited == false
            accum v.@depth = u.@depth + 1, v.@visited = true
            post-accum @@subgraph_size += 1; //, @@sub_graph += v;
                       
        @@loop += 1;
    end;
    print @@subgraph_size;
    //print @@sub_graph;
}
 
install query ksubgraph
