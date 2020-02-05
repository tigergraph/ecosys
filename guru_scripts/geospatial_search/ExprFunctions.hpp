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

#include "ExprUtil.hpp"

/**     XXX Warning!! Put self-defined struct in ExprUtil.hpp **
 *  No user defined struct, helper functions (that will not be directly called
 *  in the GQuery scripts) etc. are allowed in this file. This file only
 *  contains user-defined expression function's signature and body.
 *  Please put user defined structs, helper functions etc. in ExprUtil.hpp
 */

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


  /*inline string getGridId(float latitude, float longitude) {
    return map_lat_long_grid_id(latitude,longitude);
  } */

  inline SetAccum<string> getNearbyGridId (double distKm, double lat, double lon) {

    string gridIdStr = map_lat_long_grid_id(lat, lon); 
    uint64_t gridId = atoi(gridIdStr.c_str());

    int dia_long = gridNumLong (distKm, lat);
    int dia_lat = gridNumlat (distKm);

    int minus_dia_long = -1*dia_long;
    int minus_dia_lat  = -1*dia_lat;

    SetAccum<string> result;

    result += gridIdStr;

    int origin_lat = gridId/NUM_OF_COLS;
    int origin_lon = gridId%NUM_OF_COLS;

    for(int i = minus_dia_lat; i <= dia_lat; i++) {
      for(int j = minus_dia_long; j <= dia_long; j++) {
        int new_lat = origin_lat + i;
        int new_lon = origin_lon + j;

        // wrap around
        if (new_lat < 0) {
          new_lat = NUM_OF_ROWS + new_lat;
        } else if (new_lat > NUM_OF_ROWS) {
          new_lat = new_lat - NUM_OF_ROWS;
        }

        if (new_lon < 0) {
          new_lon = NUM_OF_COLS + new_lon;
        } else if (new_lon > NUM_OF_COLS) {
          new_lon = new_lon - NUM_OF_COLS;
        }

        int id = new_lon + NUM_OF_COLS * new_lat;

        result += std::to_string(id);
      }
    }
    return result;
  }

  inline double geoDistance(double latitude_from, double longitude_from, double latitude_to, double longitude_to) {
    double phi_1 = deg2rad(latitude_from);
    double lambda_1 = deg2rad(longitude_from);
    double phi_2 = deg2rad(latitude_to);
    double lambda_2 = deg2rad(longitude_to);
    double u = sin(phi_2 - phi_1)/2.0;
    double v = sin(lambda_2 - lambda_1)/2.0;
    return 2.0 * earthRadiusKm * asin(sqrt(u * u + cos(phi_1) * cos(phi_2) * v * v));
  }


}
/****************************************/

#endif /* EXPRFUNCTIONS_HPP_ */
