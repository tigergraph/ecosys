create online_post job load_snap for graph snap_graph {
    load to vertex MyNode values($0),
        to vertex MyNode values($1),
        to edge MyEdge values($0, $1);
}
