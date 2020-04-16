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

#include <math.h>
#include <random>
#include <chrono>


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

  inline double randn () {
      unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
      std::default_random_engine generator(seed);
      std::normal_distribution<double> distribution(0,1);
      double y = distribution(generator);
      return y;
  }
    
    
  inline int64_t rand_choice (MapAccum<int64_t, double>& Prob_Map) {
      std::vector<double> y_prob;
      std::vector<int64_t> y_id;
      for (auto yp : Prob_Map){
        y_id.push_back(yp.first);
        y_prob.push_back(yp.second);
      }
      unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
      std::default_random_engine generator(seed);
      std::discrete_distribution<int> distribution(y_prob.begin(),y_prob.end());
      int id = distribution(generator);
      return y_id[id];
  }
    
  inline double sum_ArrayAccum (ArrayAccum<SumAccum<double>>& xArray) {
      double res = 0;
      for (uint32_t i=0; i < xArray.data_.size(); i++){
        res += xArray.data_[i];
    }
      return res;
  }
    
  inline ArrayAccum<SumAccum<double>> product_List_const (const ListAccum<double>& xList, double x) {
      gvector<int64_t> dim;
      dim.push_back(xList.size());
      ArrayAccum<SumAccum<double>> yArray(dim);
    std::vector<SumAccum<double>>& data = yArray.data_;
      for (uint32_t i=0; i < xList.size(); i++){
        data[i] = x*xList.get(i);
    }
// return std::move(yArray);
      return yArray;
  }
    
  inline ListAccum<double> AXplusBY_List (double a, const ListAccum<double>& xList, double b, const ListAccum<double>& yList) {
      ListAccum<double> resList;
      for (uint32_t i=0; i < xList.size(); i++){
        resList.data_.push_back(a*xList.get(i)+b*yList.get(i));
    }
      return resList;
  }
    
  inline ListAccum<double> updateFeatures (double alpha, double a, const ListAccum<double>& xList, double b, const ListAccum<double>& yList) {
      ListAccum<double> resList;
      for (uint32_t i=0; i < xList.size(); i++){
        resList.data_.push_back(yList.get(i)-alpha*(a*xList.get(i)+b*yList.get(i)));
    }
      return resList;
  }

  inline ArrayAccum<SumAccum<double>> product_ArrayAccum_const (ArrayAccum<SumAccum<double>>& xArray, double x) {
      ArrayAccum<SumAccum<double>> yArray(xArray.dim_);
    std::vector<SumAccum<double>>& data = yArray.data_;
      for (uint32_t i=0; i < xArray.data_.size(); i++){
        data[i] = x*xArray.data_[i];
    }
//    return std::move(yArray);
      return yArray;
  }
    
  inline ArrayAccum<SumAccum<double>> diff_ArrayAccum_List (ArrayAccum<SumAccum<double>>& aArray, const ListAccum<double>& bList) {
      ArrayAccum<SumAccum<double>> cArray(aArray.dim_);
      std::vector<SumAccum<double>>& data = cArray.data_;
      for (uint32_t i=0; i < aArray.data_.size(); i++){
        data[i] = aArray.data_[i]-bList.get(i);
    }
      return cArray;
  }
  
  inline double dotProduct_ArrayAccum_List (ArrayAccum<SumAccum<double>>& aArray, const ListAccum<double>& bList) {
      double c = 0;
      for (uint32_t i=0; i < aArray.data_.size(); i++){
        c += bList.get(i)*aArray.data_[i];
    }
      return c;
  }
    
  inline double dotProduct_List_List (const ListAccum<double>& aList, const ListAccum<double>& bList) {
      double c = 0;
      for (uint32_t i=0; i < aList.size(); i++){
        c += aList.get(i)*bList.get(i);
    }
      return c;
  }
    
  inline double dotProduct_ArrayAccum_ArrayAccum (ArrayAccum<SumAccum<double>>& aArray, ArrayAccum<SumAccum<double>>& bArray) {
      double c = 0;
      for (uint32_t i=0; i < aArray.data_.size(); i++){
        c += bArray.data_[i]*aArray.data_[i];
    }
      return c;
  }
    
    //delta = t.@delta[i]*t.@a[i]*(1-t.@a[i])
  /*
  inline void delta_ArrayAccum (ArrayAccum<SumAccum<double>>& aArray, ArrayAccum<SumAccum<double>>& bArray) {
      for (uint32_t i=0; i < aArray.data_.size(); i++){
        aArray.data_[i] = aArray.data_[i]*bArray.data_[i]*(1-bArray.data_[i]);
    }
  } */
    
  inline ArrayAccum<SumAccum<double>> delta_ArrayAccum (ArrayAccum<SumAccum<double>>& aArray, ArrayAccum<SumAccum<double>>& bArray) {
      ArrayAccum<SumAccum<double>> yArray(aArray.dim_);
      std::vector<SumAccum<double>>& data = yArray.data_;
      for (uint32_t i=0; i < aArray.data_.size(); i++){
        yArray.data_[i] = aArray.data_[i]*bArray.data_[i]*(1-bArray.data_[i])-aArray.data_[i];
    }
      return yArray;
  }
    
  // cost = -(s.y.get(i)*log(s.@a[i])+(1-s.y.get(i))*log(1-s.@a[i]))
  inline double cost_ArrayAccum_List (ArrayAccum<SumAccum<double>>& aArray, const ListAccum<double>& bList) {
      double c = 0;
      for (uint32_t i=0; i < aArray.data_.size(); i++){
        c += -(bList.get(i)*log(aArray.data_[i])+(1-bList.get(i))*log(1-aArray.data_[i]));
    }
      return c;
  }
    
  /*
  inline void sigmoid_ArrayAccum (ArrayAccum<SumAccum<double>>& xArray) {
      for (uint32_t i=0; i < xArray.data_.size(); i++){
        xArray.data_[i] = 1/(1+exp(-xArray.data_[i]));
    }
  } */
  
  inline ArrayAccum<SumAccum<double>> sigmoid_ArrayAccum (ArrayAccum<SumAccum<double>>& xArray) {
      ArrayAccum<SumAccum<double>> yArray(xArray.dim_);
      std::vector<SumAccum<double>>& data = yArray.data_;
      for (uint32_t i=0; i < xArray.data_.size(); i++){
        yArray.data_[i] = 1/(1+exp(-xArray.data_[i]))-xArray.data_[i];
    }
      return yArray;
  }

    
  inline ListAccum<double> unit_List (int len) {
      ListAccum<double> yList;
      for (uint32_t i=0; i < len; i++){
        yList.data_.push_back(1);
    }
      return yList;
  }
    
  inline ArrayAccum<SumAccum<double>> unit_ArrayAccum (int len) {
      gvector<int64_t> dim;
      dim.push_back(len);
      ArrayAccum<SumAccum<double>> yArray(dim);
      std::vector<SumAccum<double>>& data = yArray.data_;
      for (uint32_t i=0; i < yArray.data_.size(); i++){
        data[i] = 1;
    }
      return yArray;
  }
}
/****************************************/

#endif /* EXPRFUNCTIONS_HPP_ */
