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
  inline int64_t str_to_int (string str) {
    return atoll(str.c_str());
  }

  inline int64_t float_to_int (float val) {
    return (int64_t) val;
  }

  inline string to_string (double val) {
    char result[200];
    sprintf(result, "%g", val);
    return string(result);
  }
  inline string bigint_to_string (int64_t val) {
    char result[200];
    sprintf(result, "%Ld", val);
    return string(result);
  }

  inline MapAccum<int64_t,int64_t> string_to_map (string str) {
    size_t pos = 0;
    string d1 = ";";
    string d2 = ",";
    string token;
    MapAccum<int64_t,int64_t> res;
    while ((pos = str.find(d1)) != std::string::npos) {
      token = str.substr(0, pos);
      size_t p = token.find(d2);
      int64_t key = atoll(token.substr(0, p).c_str());
      int64_t val = atoll(token.substr(p+1, token.size()-1).c_str());
      res += MapAccum<int64_t, int64_t>(key,val);
      str.erase(0, pos + 1);
    }
    size_t p = str.find(d2);
    int64_t key = atoll(str.substr(0, p).c_str());
    int64_t val = atoll(str.substr(p+1, str.size()-1).c_str());
    res += MapAccum<int64_t, int64_t>(key,val);
    return res;
  }
}
/****************************************/

#endif /* EXPRFUNCTIONS_HPP_ */
