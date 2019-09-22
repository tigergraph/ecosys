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

  inline void GetVertexListFromIIDList(const ListAccum<int64_t>& iidList,
                                       ListAccum<VERTEX>& vList) {
    for (auto it: iidList.data_) {
      vList += VERTEX(it);
    }
    return;
  }

  inline void PrintSetToDisk(const SetAccum<int64_t>& idSet,
                             const std::string& filename) {
    GEngineInfo(InfoLvl::Brief, "PrintSetToDisk") << "Collected " << idSet.size() << " vertices and start printing" << std::endl; 
    std::fstream fs;
    fs.open(filename.c_str(), std::fstream::out);
    if (!fs.is_open()) {
      GEngineInfo(InfoLvl::Brief, "PrintSetToDisk") 
        << "Open filename: " << filename << " failed, quit!!!" <<std::endl;
      return;
    }
    for (int64_t it : idSet.data_) {
      fs << it << "\n";
    }
    fs.close();
    GEngineInfo(InfoLvl::Brief, "PrintSetToDisk") << " Printing finished." << std::endl; 
  }

  inline void RemoveVertexByIID(ServiceAPI* api,
                              EngineServiceRequest& request,
                              const std::string& vtype,
                              ListAccum<int64_t>& vlist,
                              std::string& msg,
                              bool dryrun) {
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "Receive " << vlist.size() << " internal ids of type: \"" 
      << vtype << "\"" << std::endl;
    std::stringstream ss;
    ss << "Found " << vlist.size() << " \'" << vtype << "\' vertices" << std::endl;
    if (dryrun) {
     GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "Dryrun, nothing deleted!!!!" << std::endl;
     ss << "Dryrun, do nothing!!!" << std::endl;
     msg = ss.str();
     return;
    }
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "\tStart deleting." << std::endl;
    gshared_ptr<topology4::GraphUpdates> graphupdates = api->CreateGraphUpdates(&request);
    size_t vTypeId = api->GetTopologyMeta()->GetVertexTypeId(vtype);
    for (auto id : vlist) {
      graphupdates->DeleteVertex(true, topology4::DeltaVertexId(vTypeId, id));
    }
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "\tFinished creating updates and now waiting for commit." << std::endl;
    graphupdates->Commit();
    msg = ss.str();
  }

  inline void RemoveUnknownIID(ServiceAPI* api,
                              EngineServiceRequest& request,
                              const std::string& vtype,
                              ListAccum<int64_t>& vlist,
                              std::string& msg,
                              bool dryrun) {
    gvector<VertexLocalId_t> ids;
    gvector<VertexLocalId_t> unknownIds;
    size_t sz = 0;
    uint32_t batch = 0;
    //scan all found internal ids, batch by batch (10000 per batch)
    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "The total \'" << vtype << "\' vertices that need to be scanned is: " << vlist.size() << std::endl;
    for (auto& vt : vlist.data_) {
      ++sz;
      ids.push_back(vt);
      //sending out one batch
      if (ids.size() == 10000 || sz == vlist.data_.size()) {
        ++batch;
        std::vector<std::string> uids = std::move(api->VIdtoUId(&request, ids));
        //Get the offset for current batch
        size_t offset = sz - ids.size();
        for (size_t k = 0; k < uids.size(); ++k) {
          if (uids[k] == "UNKNOWN") {
            unknownIds.push_back(vlist.data_[offset + k]);
          }
        }
        GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "Finished scan " << batch << "-th batch with vertex size = " << ids.size() << " and the total unknown vertices size = " << unknownIds.size() << std::endl;
        ids.clear();
      }
    }

    if (unknownIds.empty()) {
      GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "No unknown vertex has been found, do nothing!!!" << std::endl;
      std::stringstream ss;
      ss << "Scanned " << vlist.size() << " \'" << vtype << "\' vertices and no unknown vid has been found, do nothing!!!";
      msg = ss.str();
      return;
    }

    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "There are total " << unknownIds.size() << " vertices have been found, start sending out delete signal." << std::endl;
    std::stringstream ss;
    ss << "Scanned " << vlist.size() << " \'" << vtype << "\' vertices, there are "
       << unknownIds.size() << " unknown vids being removed from engine." << std::endl;
    if (dryrun) {
      ss << " Dryrun is enabled, quit." << std::endl;
      msg = ss.str();
      return;
    }
    gshared_ptr<topology4::GraphUpdates> graphupdates = api->CreateGraphUpdates(&request);
    size_t vTypeId = api->GetTopologyMeta()->GetVertexTypeId(vtype);
    for (auto id : unknownIds) {
      graphupdates->DeleteVertex(true, topology4::DeltaVertexId(vTypeId, id));
    }
    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "Finished creating updates and now waiting for commit." << std::endl;
    graphupdates->Commit();
    msg = ss.str();
  }

  inline void RemoveUnknownID(ServiceAPI* api,
                              EngineServiceRequest& request,
                              const std::string& vtype,
                              ListAccum<VERTEX>& vlist,
                              std::string& msg,
                              bool dryrun) {
    gvector<VertexLocalId_t> ids;
    gvector<VertexLocalId_t> unknownIds;
    size_t sz = 0;
    uint32_t batch = 0;
    //scan all found internal ids, batch by batch (10000 per batch)
    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "The total \'" << vtype << "\' vertices that need to be scanned is: " << vlist.size() << std::endl;
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
        GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "Finished scan " << batch << "-th batch with vertex size = " << ids.size() << " and the total unknown vertices size = " << unknownIds.size() << std::endl;
        ids.clear();
      }
    }

    if (unknownIds.empty()) {
      GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "No unknown vertex has been found, do nothing!!!" << std::endl;
      std::stringstream ss;
      ss << "Scanned " << vlist.size() << " \'" << vtype << "\' vertices and no unknown vid has been found, do nothing!!!";
      msg = ss.str();
      return;
    }

    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "There are total " << unknownIds.size() << " vertices have been found, start sending out delete signal." << std::endl;
    std::stringstream ss;
    ss << "Scanned " << vlist.size() << " \'" << vtype << "\' vertices, there are "
       << unknownIds.size() << " unknown vids being removed from engine." << std::endl;
    if (dryrun) {
      ss << " Dryrun is enabled, quit." << std::endl;
      msg = ss.str();
      return;
    }
    gshared_ptr<topology4::GraphUpdates> graphupdates = api->CreateGraphUpdates(&request);
    size_t vTypeId = api->GetTopologyMeta()->GetVertexTypeId(vtype);
    for (auto id : unknownIds) {
      graphupdates->DeleteVertex(true, topology4::DeltaVertexId(vTypeId, id));
    }
    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "Finished creating updates and now waiting for commit." << std::endl;
    graphupdates->Commit();
    msg = ss.str();
  }
}
/****************************************/

#endif /* EXPRFUNCTIONS_HPP_ */
