drop all
#clear graph store -HARD
 
create vertex MyNode (primary_id id string)
create undirected edge MyEdge(from MyNode, to MyNode)
create graph snap_graph(MyNode, MyEdge)
