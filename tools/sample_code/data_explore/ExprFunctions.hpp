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
inline string GetExternalID(gpr::Context& context,
                            VERTEX& src,
                            int64_t pos,
                            const std::string& prefix) {
  auto graphAPIptr = context.GraphAPI();
  VertexAttribute attr;
  std::stringstream ss;
  if (graphAPIptr->GetVertex(src.vid, attr)) {
    uint32_t tid = graphAPIptr->GetVertexType(src.vid);
    if ( tid != -1) {
      ss << tid << ',' << src.vid << ',' << prefix << attr.GetString(pos);
    } else {
      GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
          << "GetOneVertexType got wrong: id =  " 
          << src.vid 
          << ", with obtained type id: " << tid; 
    }
  } else {
    GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
        << "Invalid vertex with iid: " << src.vid;
  }
  return ss.str();
}

inline string GetExternalIDII(gpr::Context& context,
                            VERTEX& src,
                            const std::string& prefix,
                            SumAccum<string>& eid) {
  auto graphAPIptr = context.GraphAPI();
  std::stringstream ss;
  uint32_t tid = graphAPIptr->GetVertexType(src.vid);
  if ( tid != -1) {
    ss << tid << ',' << src.vid << ',' << prefix << eid.data_;
  } else {
    GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
        << "GetOneVertexType got wrong: id =  " 
        << src.vid 
        << ", with obtained type id: " << tid; 
  }
  return ss.str();
}

inline ListAccum<VERTEX> GetUnknownVertices(ServiceAPI* api,
                            EngineServiceRequest& request,
                            ListAccum<VERTEX>& vlist,
                            const std::string& vtype) {
  //scan all found internal ids, batch by batch (10000 per batch)
  GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
      << "The total \'" 
      << vtype 
      << "\' vertices that need to be scanned is: "
      << vlist.size();
  gvector<VertexLocalId_t> ids;
  ListAccum<VERTEX> unknownIds;
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
          unknownIds += VERTEX(vlist.data_[offset + k].vid);
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

  if (unknownIds.size() == 0) {
    GEngineInfo(InfoLvl::Brief, "PrintUnknownVertices") 
        << "No unknown vertex has been found, do nothing!!!";
  }
  return unknownIds;
}

inline int64_t GetUnknownVertexCount(ServiceAPI* api,
                            EngineServiceRequest& request,
                            ListAccum<VERTEX>& vlist,
                            const std::string& vtype,
                            const std::string& outputfolder) {
  ListAccum<VERTEX> unknownIds = GetUnknownVertices(api, request, vlist, vtype);
  std::string filename = outputfolder + "/" + vtype + ".unknown";
  std::fstream fs;
  fs.open(filename.c_str(), std::fstream::out);
  if (!fs.is_open()) {
    GEngineInfo(InfoLvl::Brief, "GetUnknownVertexCount") 
      << "Open filename: " << filename << " failed, quit!!!" <<std::endl;
    return unknownIds.size();
  }
  for (auto& vt: unknownIds.data_) {
    fs << vt.vid << "\n";
  }
  fs.close();
  return unknownIds.size();
}

  inline ListAccum<VERTEX> CreateVertexListByIIDFolder(const std::string& path) {
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
    GEngineInfo(InfoLvl::Brief, "CreateVertexListByIIDFolder") << "Found total " << fileList.size() << " local files.";
    ListAccum<VERTEX> vList;
    for (auto& filename : fileList) {
      std::ifstream file(filename);
      if (file.is_open()) {
          std::string line;
          while (getline(file, line)) {
              // using printf() in all tests for consistency
             try {
              vList += VERTEX(strtoll(line.c_str(), nullptr, 10));
             } catch (std::exception& e) {
              GEngineInfo(InfoLvl::Brief, "CreateVertexListByIIDFolder") << "Bad line: " << e.what();
             }
          }
          file.close();
      } else {
        GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder") << "Can not open the file: " << filename << " skip it.";
        continue;
      }
    }
    GEngineInfo(InfoLvl::Brief, "RemoveVertexByIIDFolder") << "Read total ids: " << vList.size() ;
    return vList;
  }

inline string GetAllAttrToStr(gpr::Context& context, VERTEX src, const string& separator) {
   std::stringstream out;
   topology4::VertexAttribute instance;
   if (context.GraphAPI()->GetVertex(src.vid, instance)) {
     out << instance.type_name() << separator << src.vid;
     gvector<topology4::AttributeMeta>& attributemetas = instance.GetAttributesMeta()->attributelist_;
     for (uint64_t i = 0; i < instance.numberofattrs(); ++i) {
       AttributeMeta& meta = attributemetas[i];
       if (meta.is_deleted_) continue;
       out << separator;
       uint8_t* ptr = instance.MoveToPosition(i);
       if (!ptr) {
         out << "NULL";
       } else {
         topology4::Attribute::PrintOneAttribute(out, meta, ptr);
       }
     }
   } else {
    GEngineInfo(InfoLvl::Brief, "GetAllAttrToStr") << "Failed to get attribute: " << src.vid ;
    out << src.vid;
   }
   out << std::endl;
   return out.str();
}

}
/****************************************/

#endif /* EXPRFUNCTIONS_HPP_ */
