/******************************************************************************
 * Copyright (c) 2016, TigerGraph Inc.
 * All rights reserved.
 * Project: TigerGraph Query Language
 *
 * - This library is for defining struct and helper functions that will be used
 *   in the user-defined functions in "ExprFunctions.hpp". Note that functions
 *   defined in this file cannot be directly called from TigerGraph Query scripts.
 *   Please put such functions into "ExprFunctions.hpp" under the same directory
 *   where this file is located.
 *
 * - Please don't remove necessary codes in this file
 *
 * - A backup of this file can be retrieved at
 *     <tigergraph_root_path>/dev_<backup_time>/gdk/gsql/src/QueryUdf/ExprUtil.hpp
 *   after upgrading the system.
 *
 ******************************************************************************/

#ifndef EXPRUTIL_HPP_
#define EXPRUTIL_HPP_

#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <gle/engine/cpplib/headers.hpp>

typedef std::string string; //XXX DON'T REMOVE

/*
 * Define structs that used in the functions in "ExprFunctions.hpp"
 * below. For example,
 *
 *   struct Person {
 *     string name;
 *     int age;
 *     double height;
 *     double weight;
 *   }
 *
 */

  /*******************************************************
   * Config this for grid size. The numer is in minunte.
   * the area of a geo grid is the square of GRID_SIDE_LENGTH.
   * The value of GRID_SIDE_LENGTH should be able to evenly devide 60.
   ******************************************************/
  static const int GRID_SIDE_LENGTH = 4;

  struct Degree {
    int day;
    int min;
    int sec;

    Degree() {
      day = 0;
      min = 0;
      sec = 0;
    }

    Degree(int d, int m, int s) {
      day = d;
      min = m;
      sec = s;
    }
  };

  const double pi = 3.14159265358979323846;
  static const float  earthRadiusKm = 6373.0;  
  static const int MAP_LAT_LOW_BOUND = -90;   // Low Bound of latitude of the map;
  static const int MAP_LAT_HIGH_BOUND = 90;   // High Bound of latitude of the map;
  static const int MAP_LONG_LOW_BOUND = -180; // Low Bound of longitude of the map;
  static const int MAP_LONG_HIGH_BOUND = 180; // High Bound of longitude of the map;
  static const int NUM_OF_COLS = (MAP_LONG_HIGH_BOUND*60 -MAP_LONG_LOW_BOUND*60)/GRID_SIDE_LENGTH;
  static const int NUM_OF_ROWS = (MAP_LAT_HIGH_BOUND*60 - MAP_LAT_LOW_BOUND*60)/GRID_SIDE_LENGTH;

  inline float km2Latitude(float km) { 
    return km/110.54; 
  } 
 
  inline float km2Longitude(float km, double lat) { 
    return km/(111.32*cos(lat)); 
  } 
 
  inline int gridNumlat (float km) { 
    return std::abs(round(km2Latitude(km)/(360.0/NUM_OF_COLS))); 
  } 
 
  inline int gridNumLong (float km, double lat) { 
    return std::abs(round(km2Longitude(km, lat)/(180.0/NUM_OF_ROWS))); 
  } 

  inline void deg_to_dms(double deg, Degree& degree) { 
    degree.day = int(deg); 
    double md = (deg - degree.day) * 60; 
    degree.min = int(md); 
    degree.sec = (md - degree.min) * 60; 
  } 
 
  inline string map_lat_long_grid_id(double latitude,double longitude) { 
    Degree lat_degree,long_degree; 
 
    deg_to_dms(latitude,lat_degree); 
    deg_to_dms(longitude,long_degree); 
 
    int64_t grid_id = ((lat_degree.day - MAP_LAT_LOW_BOUND) * 60 + lat_degree.min)/GRID_SIDE_LENGTH * NUM_OF_COLS + ((long_degree.day - MAP_LONG_LOW_BOUND) * 60 + long_degree.min)/GRID_SIDE_LENGTH; 

    return std::to_string(grid_id); 
  } 

  // public domain conversion of degrees to radians
  // used in geo-location.
  inline double deg2rad(double d) {
    return (d * pi / 180);
  }


#endif /* EXPRUTIL_HPP_ */
