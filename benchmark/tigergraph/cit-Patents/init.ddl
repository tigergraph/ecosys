drop all
#clear graph store -HARD
 
create vertex MyNode (primary_id id string)
create directed edge MyEdge(from MyNode, to MyNode)
create graph snap_graph(MyNode, MyEdge)
