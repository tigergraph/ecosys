/******************************************************************************
 * Copyright (c) 2014, 2016, GraphSQL Inc.
 * All rights reserved.
 * Project: GraphSQL Loader
 * TokenBank.cpp: a library of token conversion function declaration. 
 *
 * - It is an N-tokens-in, one-token-out function. N is one or more.
 * - All functions must use one of the following signatures, 
 *   but different function name.
 * - A token function can have nested other token function;
 *   The out-most token function should return the same type
 *   as the targeted attribute type specified in the 
 *   vertex/edge schema.
 *  
 *   1. string[] -> string 
 *
 *   The UDF token conversion functions will take N input char 
 *   array and do a customized conversion. Then, put the 
 *   converted char array to the output char buffer.
 *
 *     extern "C" void funcName (const char* const iToken[], uint32_t iTokenLen[], 
 *     uint32_t iTokenNum, char* const oToken, uint32_t& oTokenLen);
 *     
 *      @param: iToken: 1 or more input tokens, each pointed by one char pointer 
 *      @param: iTokenLen: each input token's length
 *      @param: iTokenNum: how many input tokens 
 *      @param: oToken: the output token buffer; caller will prepare this buffer.
 *      @param: oTokenLen: the output token length 
 *
 *      Note: extern "C" make C++ compiler not change/mangle the function name.
 *      Note: To avoid array out of boundary issue in oToken buffer, it is 
 *            recommended to add semantic check to ensure oToken length does 
 *            not exceed  OutputTokenBufferSize parameter specified in the 
 *            shell config. Default is 2000 chars.
 *             
 *
 *  
 *   2. string[] -> int/bool/float
 *
 *     extern "C" uint64_t funcName (const char* const iToken[], 
 *     uint32_t iTokenLen[], uint32_t iTokenNum)
 *
 *     extern "C" bool funcName (const char* const iToken[], uint32_t iTokenLen[], 
 *     uint32_t iTokenNum)
 *
 *     extern "C" float funcName (const char* const iToken[], uint32_t iTokenLen[], 
 *     uint32_t iTokenNum)
 *
 *      @param: iToken: 1 or more input tokens, each pointed by one char pointer 
 *      @param: iTokenLen: each input token's length
 *      @param: iTokenNum: how many input tokens 
 *
 *   Think token function as a UDF designed to combine N specific columns into 
 *   one column before we load them into graph store.
 * 
 * - All functions can be used in the loading job definition, in the VALUES caluse.
 *    e.g. Let a function named Concat(), we can use it in the DDL shell as below
 *      values( $1, Concat($2,$3), $3...)
 *
 *
 * - Once defined UDF, run the follow to compile a shared libary.
 *
 *    TokenBank/compile
 *
 *   GraphSQL loader binary will automatically use the library at runtime.
 *
 * - You can unit test your token function in the main function in this file.
 *   To run your test, you can do 
 *
 *     g++ TokenBank.cpp 
 *     ./a.out
 *
 * Created on: Dec 11, 2014
 * Updated on: July 19, 2016
 * Author: MW Wu
 ******************************************************************************/

#include <stdio.h>
#include <stdint.h>
#include <iostream>
#include <cstring>
#include <vector>
#include <string>
#include <stdbool.h>
#include <cstdlib>
#include <string.h>
#include <stdlib.h>

#include <TokenLib.hpp>

 /**
  * this function concatenate all input tokens into one big token
  *
  */
extern "C" void _Concat(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum,
    char* const oToken, uint32_t& oTokenLen){
      
  int k = 0;
  for (int i=0; i < iTokenNum; i++){
    for (int j =0; j < iTokenLen[i]; j++) {
           oToken[k++]=iToken[i][j];
    }
  }
  oTokenLen = k;
}

/**
 * This function convert iToken char array of size iTokenLen, reverse order 
 * and put it in oToken.
 *
 * Only one token input.
 */
extern "C"  void Reverse (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum, 
    char *const oToken, uint32_t& oTokenLen) {

  uint32_t j = 0;
  for (uint32_t i = iTokenLen[0]; i > 0; i--) {
    oToken[j++] = iToken[0][i-1];
  }
  oTokenLen = j;
}
/**
 * This function removes all heading and tailing spaces of the input single iToken
 */
extern "C"  void Trim (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum, 
    char *const oToken, uint32_t& oTokenLen) {

  uint32_t start = 0;
  uint32_t end = iTokenLen[0];
  //remove heading white spaces
  while(iToken[0][start] == ' ' && start < end) {
    ++start;
  }
  //remove tailing white spaces
  while(iToken[0][end - 1] == ' ' && end > start) {
    --end;
  }

  for (uint32_t i = start; i < end; ++i) {
    oToken[i - start] = iToken[0][i];
  }
  oTokenLen = end - start > 0 ? end - start : 0;
}

/**
 * This function formalize the date format from "%m/%d/%Y %H:%m:%s" to "%Y/%m/%d %H:%m:%s"
 */
extern "C"  void FormatDate (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum, 
    char *const oToken, uint32_t& oTokenLen) {
  int dateInfoLen = 0;
  const char* ptr = iToken[0];
  while (*ptr != '/') {
    ++ptr;
    ++dateInfoLen;
  }
  ++ptr;
  while (*ptr != '/') {
    ++ptr;
    ++dateInfoLen;
  }
  //Now ptr should point to '/' before "%Y" in old format
  const char* ptr_day_end = ptr;//record the end of "%m/%d"
  uint32_t i = 0;
  ++ptr;
  while(*ptr != ' ') {
    oToken[i++] = *ptr;
    ++ptr;
  }
  //Now oToken already has "%Y" info
  oToken[i++] = '/'; //adding "/" after "%Y"
  //Now ptr point to " %H:%m:%s"
  const char* ptr_time_start = ptr;//record the start position of " %H:%m:%s";
  const char* newptr = iToken[0];//newptr to back to the begining of "%m/%d/%Y %H:%m:%s"
  while (newptr != ptr_day_end) {
    oToken[i++] = *newptr;
    ++newptr;
  }
  //copy " %H:%m:%s" into oToken
  while (i < iTokenLen[0]) {
    oToken[i++] = *ptr_time_start;
    ++ptr_time_start;
  }
  oToken[i]='\0';
  oTokenLen = iTokenLen[0];
  //std::cout << "The old Time format:" << iToken[0] << ", The new Time Format:" << oToken << "." << std::endl;
}
/**
 * This function convert iToken char array of size iTokenLen, convert it first to epoch seconds use builtin
 * Then divided by 86400 to get the day
 */
extern "C"  void GetDateNum (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum, 
    char *const oToken, uint32_t& oTokenLen) {
  int ts = gsql_ts_to_epoch_seconds(iToken, iTokenLen, iTokenNum) / 86400;
  oTokenLen = sprintf(oToken, "%d", ts);
}

/**
 * This function check whether the input char array is in a valid DateTime format
 */
extern "C"  bool IsValidDateTime (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum) {
 struct tm tm;
 return strptime(iToken[0], "%m/%d/%Y %H:%M:%S", &tm) != NULL; 
 /*
 if (strptime(iToken[0], "%m/%d/%Y %H:%M:%S", &tm) == NULL) {
   std::cout << "Fails!!! The datetime is: " <<iToken[0] << std::endl;
   return false;
 } else {
   std::cout << "Sucess!!! The datetime is: " <<iToken[0] << std::endl;
   return true;
 }*/
}

/**
 * This function convert iToken to integer and compare with 3.
 * If it is greater than 3, return true. Otherwise, return false.
 *
 * Only one token input.
 */
extern "C"  bool GreaterThan3(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum) {

  int tmp = atoi(iToken[0]);

  if (tmp >3) {
    return true;
  }
  return false;
}

/**
 * This function convert each input token to an integer, sum them and compare 
 * the sum with 3. If it is greater than 3, return true. Otherwise, return false.
 *
 */
extern "C" bool SumGreaterThan3(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum) 
{
  int k = 0;
  int sum = 0;

  for (int i=0; i < iTokenNum; i++) {
    int tmp = atoi(iToken[i]);
    sum += tmp;
  }

  if (sum > 3) {
    return true;
  }

  return false;
}

/**
 *  Unit testing of the token bank functions
 */ 
int main(){

  char* a[2];
  char d[100]={'a','b','c','d','e','f'};

  a[0]=&d[0];
  a[1]=&d[3];

  uint32_t len[2];

  len[0] = 3;
  len[1] = 3;


  char b[100];
  uint32_t  outlen;
  _Concat(a,len,2, b, outlen);
  for(int i =0; i<outlen; i++){
    std::cout<<b[i]<<",";
  }
  std::cout<<std::endl;

}

