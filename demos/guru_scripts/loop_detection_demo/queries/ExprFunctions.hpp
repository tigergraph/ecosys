/******************************************************************************
 * Copyright (c) 2015-2016, GraphSQL Inc.
 * All rights reserved.
 * Project: GraphSQL Query Language
 * udf.hpp: a library of user defined functions used in queries.
 *
 * - This library should only define functions that will be used in
 *   GraphSQL Query scripts. Other logics, such as structs and helper
 *   functions that will not be directly called in the GQuery scripts,
 *   must be put into "ExprUtil.hpp" under the same directory where
 *   this file is located.
 *
 * - Supported type of return value and parameters
 *     - int
 *     - float
 *     - double
 *     - bool
 *     - string (don't use std::string)
 *     - accumulators
 *
 * - Function names are case sensitive, unique, and can't be conflict with
 *   built-in math functions and reserve keywords.
 *
 * - Please don't remove necessary codes in this file
 *
 * - A backup of this file can be retrieved at
 *     <graphsql_root_path>/dev_<backup_time>/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
 *   after upgrading the system.
 *
 ******************************************************************************/

#ifndef EXPRFUNCTIONS_HPP_
#define EXPRFUNCTIONS_HPP_

#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <gle/engine/cpplib/headers.hpp>

/**     XXX Warning!! Put self-defined struct in ExprUtil.hpp **
 *  No user defined struct, helper functions (that will not be directly called
 *  in the GQuery scripts) etc. are allowed in this file. This file only
 *  contains user-defined expression function's signature and body.
 *  Please put user defined structs, helper functions etc. in ExprUtil.hpp
 */
#include "ExprUtil.hpp"

typedef std::string string; //XXX DON'T REMOVE

/****** BIULT-IN FUNCTIONS **************/
/****** XXX DON'T REMOVE ****************/
inline int str_to_int (string str) {
  return atoi(str.c_str());
}

inline int float_to_int (float val) {
  return (int) val;
}

inline string to_string (double val) {
  char result[200];
  sprintf(result, "%g", val);
  return string(result);
}
/****************************************/

inline int64_t GetEdgeId(ServiceAPI* api, bool send) {
  string EdgeType = "send";
  if (!send) {
    EdgeType = "rev_send";
  }
  return api->GetTopologyMeta()->GetEdgeTypeId(EdgeType);
}

inline EDGE RevertEdge(const EDGE& e, int64_t revEdgeTypeId) {
  return EDGE(e.tgtVid, e.srcVid, (uint32_t)revEdgeTypeId);
}

template<typename EdgeTuple>
inline bool PathContainsV(ListAccum<EdgeTuple>& pathAccum, VERTEX v) {
  std::vector<EdgeTuple>& path = pathAccum.data_;
  for (uint32_t i = 0; i < path.size(); ++i) {
    if (v == path[i].v) {
      return true;
    }
  }
  return false;
}

template<typename VAL>
inline int GetNeighborRank(gpelib4::SingleValueContext<VAL>* context,
                           VERTEX src,
                           int64_t edgeTypeId
                          ) {
  int flag = -1;
  gapi4::EdgesFilter_ByOneType filter(edgeTypeId);
  gapi4::EdgesCollection ec;
  context->GraphAPI()->GetEdges(src.vid, &filter, ec);
  //only one edge here, if it exist then it is the target
  if (ec.NextEdge()) {
    //initialize a attribute
    VertexAttribute tgtAttr;
    if (context->GraphAPI()->GetVertex(ec.GetCurrentToVId(), tgtAttr)) {
      flag = tgtAttr.GetInt("flag", -1);
    }
  }
  return flag;
}

template<typename EdgeTuple>
inline void GetValidPaths(ListAccum<ListAccum<EdgeTuple>>& circleEdgeTuplesAccum,
                          SetAccum<VERTEX>& vSetAccum,
                          ListAccum<ListAccum<EDGE>>& circlePathsAccum,
                          double drainRatio) {
  if (circleEdgeTuplesAccum.size() == 0) {
    return;
  }
  std::vector<ListAccum<EdgeTuple>>& circleEdgeTuples = circleEdgeTuplesAccum.data_;
  //loop over circleEdgeTuples to valid each path inside it
  for (uint32_t i = 0; i < circleEdgeTuples.size(); ++i) {
    std::vector<EdgeTuple>& tupleList = circleEdgeTuples[i].data_;
    uint32_t minMoney = (uint32_t)-1, maxMoney = 0;
    for (uint32_t j = 0; j < tupleList.size(); ++j) {
      uint32_t money = tupleList[j].amount;
      if (money < minMoney) {
        minMoney = money;
      }
      if (money > maxMoney) {
        maxMoney = money;
      }
    }
    //only update path and vSet if it is valid path
    //validPath must have drainRatio smaller than the given one
    if ((maxMoney >= minMoney)
        && (((maxMoney - minMoney) * 1.0 / maxMoney) <= drainRatio)) {
      //create the local paths and vSet to collect the path info
      ListAccum<EDGE> path;
      SetAccum<VERTEX> vSet;
      for (uint32_t j = 0; j < tupleList.size(); ++j) {
        path += tupleList[j].e;
        vSet += tupleList[j].v;
      }
      //update the global vSet and Paths
      vSetAccum += vSet;
      circlePathsAccum += path;
    }
  }
}

inline void printWriter(EngineServiceRequest& request, std::string JsonStr) {
  request.outputwriter_->WriteRaw(JsonStr.c_str(), JsonStr.size());
}

#endif /* EXPRFUNCTIONS_HPP_ */
