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

  inline ArrayAccum<SumAccum<double>> multiply_ArrayAccum_ListOfList (ArrayAccum<SumAccum<double>>& aArray, const ListAccum<ListAccum<double>>& bList) {
      gvector<int64_t> dim;
      dim.push_back(bList.get(0).size());
      ArrayAccum<SumAccum<double>> yArray(dim);
      for (uint32_t i=0; i < bList.get(0).size(); i++){
	for (uint32_t j=0; j < aArray.data_.size(); j++){
          yArray.data_[i] += bList.get(j).get(i) * aArray.data_[j];
	}
    }
      return yArray;
  }

// multiply the transpose of the ListAccum<ListAccum<double>>
  inline ArrayAccum<SumAccum<double>> multiply_ArrayAccum_ListOfListT (ArrayAccum<SumAccum<double>>& aArray, const ListAccum<ListAccum<double>>& bList) {
      gvector<int64_t> dim;
      dim.push_back(bList.size());
      ArrayAccum<SumAccum<double>> yArray(dim);
      for (uint32_t i=0; i < bList.size(); i++){
        for (uint32_t j=0; j < aArray.data_.size(); j++){
          yArray.data_[i] += bList.get(i).get(j) * aArray.data_[j];
        }
    }
      return yArray;
  }

  
// multiply 2 vectors and return a matrix
  inline ArrayAccum<SumAccum<double>> multiply_ArrayAccum_ArrayAccum (ArrayAccum<SumAccum<double>>& aArray, ArrayAccum<SumAccum<double>>& bArray) {
      gvector<int64_t> dim;
      dim.push_back(aArray.data_.size());
      dim.push_back(bArray.data_.size());
      ArrayAccum<SumAccum<double>> yArray(dim);
      for (uint32_t i=0; i < aArray.data_.size(); i++){
        for (uint32_t j=0; j < bArray.data_.size(); j++){
          yArray.data_[i * bArray.data_.size() + j] = aArray.data_[i] * bArray.data_[j];
        }
    }
      return yArray;
  }


  inline ArrayAccum<SumAccum<double>> elementProduct_ArrayAccum_ArrayAccum (ArrayAccum<SumAccum<double>>& aArray, ArrayAccum<SumAccum<double>>& bArray) {
      ArrayAccum<SumAccum<double>> yArray(aArray.dim_);
      for (uint32_t i=0; i < aArray.data_.size(); i++){
      	yArray.data_[i] = aArray.data_[i] * bArray.data_[i];
      }
      return yArray;  
  }


  inline ArrayAccum<SumAccum<double>> multiply_ArrayAccum_const (ArrayAccum<SumAccum<double>>& xArray, double x) {
      ArrayAccum<SumAccum<double>> yArray(xArray.dim_);
      std::vector<SumAccum<double>>& data = yArray.data_;
      for (uint32_t i=0; i < xArray.data_.size(); i++){
        data[i] = x * xArray.data_[i];
    }
      return yArray;
  }

  inline ArrayAccum<SumAccum<double>> tanh_ArrayAccum (ArrayAccum<SumAccum<double>>& xArray) {
      ArrayAccum<SumAccum<double>> yArray(xArray.dim_);
      for (uint32_t i=0; i < xArray.data_.size(); i++){
        yArray.data_[i] = tanh(xArray.data_[i]);
    }
      return yArray;
  }

  inline ArrayAccum<SumAccum<double>> tanh_prime_ArrayAccum (ArrayAccum<SumAccum<double>>& xArray) {
      ArrayAccum<SumAccum<double>> yArray(xArray.dim_);
      for (uint32_t i=0; i < xArray.data_.size(); i++){
        yArray.data_[i] = 1- pow(tanh(xArray.data_[i]), 2);
    }
      return yArray;
  }


}
/****************************************/

#endif /* EXPRFUNCTIONS_HPP_ */
