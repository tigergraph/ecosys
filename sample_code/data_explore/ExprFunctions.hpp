/******************************************************************************
 * Copyright (c) 2015-2016, TigerGraph Inc.
 * All rights reserved.
 * Project: TigerGraph Query Language
 * udf.hpp: a library of user defined functions used in queries.
 *
 * - This library should only define functions that will be used in
 *   TigerGraph Query scripts. Other logics, such as structs and helper
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
 *     <tigergraph_root_path>/dev_<backup_time>/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
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

namespace UDIMPL {
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

  inline VERTEX getTgtVid(const EDGE& e) {
    return e.tgtVid;
  }

  inline int GetSegmentId(ServiceAPI* api, const VERTEX& v) {
    return v.vid >> (api->GetTopologyMeta()->segmentsizeinpowerof2_);
  }

//ExprFunction.hpp
inline int64_t PrintUnknownVertices(ServiceAPI* api,
                            EngineServiceRequest& request,
                            ListAccum<VERTEX>& vlist,
                            const std::string& vtype,
                            int64_t pos,
                            const std::string& prefix,
                            const std::string& outputfolder) {
  std::fstream fs;
  fs.open(outputfolder.c_str(), std::fstream::out);
  if (!fs.is_open()) {
    GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
      << "Open filename: " 
      << outputfolder 
      << " failed, quit!!!";
    return 0;
  }
  fs.close();
  //scan all found internal ids, batch by batch (10000 per batch)
  GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
      << "The total \'" 
      << vtype 
      << "\' vertices that need to be scanned is: "
      << vlist.size();
  gvector<VertexLocalId_t> ids;
  gvector<VertexLocalId_t> unknownIds;
  size_t sz = 0;
  uint32_t batch = 0;
  for (auto& vt : vlist.data_) {
    ++sz;
    ids.push_back(vt.vid);
    //sending out one batch
    if (ids.size() == 10000 || sz == vlist.data_.size()) {
      ++batch;
      std::vector<std::string> uids = std::move(api->VIdtoUId(&request, ids));
      //Get the offset for current batch
      size_t offset = sz - ids.size();
      for (size_t k = 0; k < uids.size(); ++k) {
        if (uids[k] == "UNKNOWN") {
          unknownIds.push_back(vlist.data_[offset + k].vid);
        }
      }
      GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
          << "Finished scan " 
          << batch 
          << "-th batch with vertex size = " 
          << ids.size() 
          << " and the total unknown vertices size = " 
          << unknownIds.size();
      ids.clear();
    }
  }

  if (unknownIds.empty()) {
    GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
        << "No unknown vertex has been found, do nothing!!!";
    return 0;
  }

  GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
      << "There are total " 
      << unknownIds.size() 
      << " vertices have been found, start write data into target folder: " 
      << outputfolder;

  //open file for output
  fs.open(outputfolder.c_str(), std::fstream::out);
  if (fs.is_open()) {
    auto graphAPIptr = api->CreateGraphAPI(&request);
    VertexAttribute attr;
    for (auto id : unknownIds) {
      if (graphAPIptr->GetOneVertex(id, attr)) {
        uint32_t tid = -1;
        if (graphAPIptr->GetOneVertexType(id, tid) && tid != -1) {
          fs << tid << ',' << id << ',' << prefix << attr.GetString(pos) << '\n';
        } else {
          GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
              << "GetOneVertexType got wrong: id =  " 
              << id 
              << ", with obtained type id: " << tid; 
        }
      } else {
        GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
            << "Invalid vertex with iid: " << id;
      }
    }
    fs.close();
    GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
      << "all done!!!";
  } else {
    GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
      << "Open filename: " 
      << outputfolder 
      << " failed, quit!!!";
  }
  return unknownIds.size();
}
}
/****************************************/

#endif /* EXPRFUNCTIONS_HPP_ */
