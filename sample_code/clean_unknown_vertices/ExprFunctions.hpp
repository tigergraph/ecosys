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

  inline void ReadVertexListFromIIDListFile(ListAccum<VERTEX>& vList,
                             const std::string& filename) {
    std::ifstream file(filename);
    if (file.is_open()) {
        std::string line;
        while (getline(file, line)) {
            // using printf() in all tests for consistency
            try {
              vList += VERTEX(strtoll(line.c_str(), nullptr, 10));
            } catch (std::exception& e) {
              GEngineInfo(InfoLvl::Brief, "ReadVertexListFromIIDListFile") << "Bad line: " << e.what();
            }
        }
        file.close();
    }
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

  inline std::string RemoveVertexByIID(ServiceAPI* api,
                              EngineServiceRequest& request,
                              gpelib4::MasterContext* context,
                              ListAccum<int64_t>& vlist,
                              std::string& msg,
                              bool dryrun) {
    if (vlist.size() == 0) {
      GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "Receive empty vlist, exist!!!";
      return "";
    }
    uint32_t vTypeId  = context->GraphAPI()->GetVertexType(vlist.data_[0]);
    if (vTypeId == -1) {
      GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "Get type id failed, the input vid = " << vlist.data_[0];
      return "";
    }

    std::string vtype = api->GetTopologyMeta()->GetVertexType(vTypeId).typename_;
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "Receive " << vlist.size() << " internal ids of type: \"" 
      << vtype << "\"" << std::endl;
    std::stringstream ss;
    ss << "Found " << vlist.size() << " \'" << vtype << "\' vertices" << std::endl;
    if (dryrun) {
     GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "Dryrun, nothing deleted!!!!" << std::endl;
     ss << "Dryrun, do nothing!!!" << std::endl;
     msg += ss.str();
     return vtype;
    }
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "\tStart deleting." << std::endl;
    gshared_ptr<topology4::GraphUpdates> graphupdates = api->CreateGraphUpdates(&request);
    for (auto id : vlist) {
      graphupdates->DeleteVertex(true, topology4::DeltaVertexId(vTypeId, id));
    }
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIID") << "\tFinished creating updates and now waiting for commit." << std::endl;
    graphupdates->Commit();
    msg += "\n" + ss.str();
    return vtype;
  }

  inline void ReadVertexListFromPath(const std::string& path,
                                     ListAccum<VERTEX>& vList) {
    std::vector<std::string> fileList;
    if (boost::filesystem::is_directory(path)) {
      // we cannot loop directory in GSQL side since it may not locate in m1
      boost::filesystem::directory_iterator it(path);
      boost::filesystem::directory_iterator end_it;
      while (it != end_it){
        //no hidden files
        std::string localfile 
          =  boost::filesystem::path(it->path().string()).filename().string();
        if (boost::filesystem::is_regular_file(it->status()) && localfile[0] != '.') {
          fileList.push_back(it->path().string());
        } else {
          GEngineInfo(InfoLvl::Brief, "ReadVertexListFromPath")
            << "[WARNING] The file \""
            << it->path().string()
            << "\" is skipped because it is either a irregular file or a hidden file.";
        }
        ++ it;
      }
    } else if (boost::filesystem::is_regular_file(path)){
      fileList.push_back(path);
    }
    GEngineInfo(InfoLvl::Brief, "ReadVertexListFromPath") << "Found total " << fileList.size() << " segment files.";
    for (auto& filename : fileList) {
      std::ifstream file(filename);
      if (file.is_open()) {
          std::string line;
          while (getline(file, line)) {
              // using printf() in all tests for consistency
             try {
              vList += VERTEX(strtoll(line.c_str(), nullptr, 10));
             } catch (std::exception& e) {
              GEngineInfo(InfoLvl::Brief, "ReadVertexListFromPath") << "Bad line: " << e.what();
             }
          }
          file.close();
      } else {
        GEngineInfo(InfoLvl::Brief, "ReadVertexListFromPath") << "Can not open the segment file: " << filename << " skip it.";
        continue;
      }
    }
    GEngineInfo(InfoLvl::Brief, "ReadVertexListFromPath") << "Total deleted vertex ids: " << vList.size();
  }

  inline void RemoveVertexByIIDFolder(ServiceAPI* api,
                              EngineServiceRequest& request,
                              gpelib4::MasterContext* context,
                              const std::string& path,
                              std::string& msg,
                              bool dryrun) {
    std::vector<std::string> fileList;
    if (boost::filesystem::is_directory(path)) {
      // we cannot loop directory in GSQL side since it may not locate in m1
      boost::filesystem::directory_iterator it(path);
      boost::filesystem::directory_iterator end_it;
      while (it != end_it){
        //no hidden files
        std::string localfile 
          =  boost::filesystem::path(it->path().string()).filename().string();
        if (boost::filesystem::is_regular_file(it->status()) && localfile[0] != '.') {
          fileList.push_back(it->path().string());
        } else {
          GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder")
            << "[WARNING] The file \""
            << it->path().string()
            << "\" is skipped because it is either a irregular file or a hidden file.";
        }
        ++ it;
      }
    } else if (boost::filesystem::is_regular_file(path)){
      fileList.push_back(path);
    }
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder") << "Found total " << fileList.size() << " segment files.";
    ListAccum<int64_t> vList;
    std::unordered_map<std::string, uint64_t> cntmap;
    for (auto& filename : fileList) {
      vList.data_.clear();
      std::ifstream file(filename);
      if (file.is_open()) {
          std::string line;
          while (getline(file, line)) {
              // using printf() in all tests for consistency
             try {
              vList += strtoll(line.c_str(), nullptr, 10);
             } catch (std::exception& e) {
              GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder") << "Bad line: " << e.what();
             }
          }
          file.close();
      } else {
        GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder") << "Can not open the segment file: " << filename << " skip it.";
        continue;
      }
      GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder") << "Start processing segment file: " << filename << " with total ids: " << vList.size() ;
      if (vList.size() > 0) {
        std::string m;
        std::string vtype = RemoveVertexByIID(api, request, context, vList, m, dryrun);
        if (!vtype.empty()) {
          cntmap[vtype] += vList.size();
        }
      }
      GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder") << "Finished processing segment file: " << filename << " with total ids: " << vList.size() ;
    }
    for (auto it : cntmap) {
      msg += "\'" + it.first + "\' : " + std::to_string(it.second) + " | ";
    }
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
      msg += ss.str();
      return;
    }

    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "There are total " << unknownIds.size() << " vertices have been found, start sending out delete signal." << std::endl;
    std::stringstream ss;
    ss << "Scanned " << vlist.size() << " \'" << vtype << "\' vertices, there are "
       << unknownIds.size() << " unknown vids being removed from engine." << std::endl;
    if (dryrun) {
      ss << " Dryrun is enabled, quit." << std::endl;
      msg += ss.str();
      return;
    }
    gshared_ptr<topology4::GraphUpdates> graphupdates = api->CreateGraphUpdates(&request);
    size_t vTypeId = api->GetTopologyMeta()->GetVertexTypeId(vtype);
    for (auto id : unknownIds) {
      graphupdates->DeleteVertex(true, topology4::DeltaVertexId(vTypeId, id));
    }
    GEngineInfo(InfoLvl::Brief, "RemoveUnknownID") << "Finished creating updates and now waiting for commit." << std::endl;
    graphupdates->Commit();
    msg += ss.str();
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
