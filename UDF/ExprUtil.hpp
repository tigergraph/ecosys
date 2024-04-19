#ifndef EXPRUTIL_HPP_
#define EXPRUTIL_HPP_

#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <gle/engine/cpplib/headers.hpp>
#include <vector>

typedef std::string string;
const double JARO_WEIGHT_STRING_A(1.0 / 3.0);
const double JARO_WEIGHT_STRING_B(1.0 / 3.0);
const double JARO_WEIGHT_TRANSPOSITIONS(1.0 / 3.0);
const unsigned long int JARO_WINKLER_PREFIX_SIZE(4);
const double JARO_WINKLER_SCALING_FACTOR(0.1);
const double JARO_WINKLER_BOOST_THRESHOLD(0.7);

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

#endif