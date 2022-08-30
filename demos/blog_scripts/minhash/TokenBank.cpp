/******************************************************************************
 * Copyright (c) 2014, 2016, TigerGraph Inc.
 * All rights reserved.
 * Project: TigerGraph Loader
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
 *   TigerGraph loader binary will automatically use the library at runtime.
 *
 * - You can unit test your token function in the main function in this file.
 *   To run your test, you can do
 *
 *     g++ TokenBank.cpp
 *     ./a.out
 *
 * Created on: Dec 11, 2014
 * Updated on: July 19, 2016
 * Author: Mingxi Wu
 ******************************************************************************/

#include <stdio.h>
#include <stdint.h>
#include <iostream>
#include <cstring>
#include <vector>
#include <string>
#include <stdbool.h>
#include <cstdlib>
#include <assert.h>
#include <string.h>
#include "TokenLib.hpp"

 /**
  * this function concatenate all input tokens into one big token
  *
  */
/*extern "C" void _Concat(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum,
    char* const oToken, uint32_t& oTokenLen){

  uint32_t k = 0;
  for (uint32_t i=0; i < iTokenNum; i++){
    for (uint32_t j =0; j < iTokenLen[i]; j++) {
           oToken[k++]=iToken[i][j];
    }
  }
  oTokenLen = k;
}*/

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

  for (uint32_t i=0; i < iTokenNum; i++) {
    int tmp = atoi(iToken[i]);
    sum += tmp;
  }

  if (sum > 3) {
    return true;
  }

  return false;
}
uint32_t MurmurHash2 ( const void * key, int len, uint32_t seed )
{
  // 'm' and 'r' are mixing constants generated offline.
  // They're not really 'magic', they just happen to work well.

  const uint32_t m = 0x5bd1e995;
  const int r = 24;

  // Initialize the hash to a 'random' value

  uint32_t h = seed ^ len;

  // Mix 4 bytes at a time into the hash

  const unsigned char * data = (const unsigned char *)key;

  while(len >= 4)
  {
    uint32_t k = *(uint32_t*)data;

    k *= m;
    k ^= k >> r;
    k *= m;

    h *= m;
    h ^= k;

    data += 4;
    len -= 4;
  }

  // Handle the last few bytes of the input array

  switch(len)
  {
  case 3: h ^= data[2] << 16;
  case 2: h ^= data[1] << 8;
  case 1: h ^= data[0];
      h *= m;
  };

  // Do a few final mixes of the hash to ensure the last few
  // bytes are well-incorporated.

  h ^= h >> 13;
  h *= m;
  h ^= h >> 15;

  return h;
}

extern "C" void minHash (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum,
    char* const oToken, uint32_t& oTokenLen) {
  //minHash take three input (int str, int shingleLen, int b, int r)
  uint32_t shingleLen = atoi(iToken[1]);
  int b = atoi(iToken[2]);
  //only handle the case for the number of hash functions is less or equal to 100
  uint32_t k = std::min(iTokenLen[0], shingleLen);
  char tmp[k];
  std::vector<uint32_t> signatures;
  //std::cout << "start signatures\n";
  for (int i = 0; i < iTokenLen[0] - k + 1; ++i) {
    signatures.push_back(MurmurHash2(&iToken[0][i], k, 0));
    //std::cout << "token: " << std::string(&iToken[0][i], k) << ", MurmurHash2: " <<  MurmurHash2(&iToken[0][i], k, 0) << std::endl;
  }
  //std::cout << "end signatures\n";
  //x = (a*x + b) % p
  uint64_t p = 4294967311; //the prime number bigger than 2^32 - 1
  uint32_t c1[101] = {1, 482067061, 1133999723, 2977257653, 2666089543, 3098200677, 3985189319, 1857983931, 3801236429, 522456919, 4057595205, 4176190031, 1652234975, 2294716503, 1644020081, 3407377875, 3749518465, 4244672803, 3053397733, 3273255487, 598272097, 3989043777, 1414109747, 697129027, 67285677, 98002333, 158583451, 1424122447, 2159224677, 3478101309, 277468927, 1902928727, 2459288935, 3941065327, 1244061689, 1898521317, 4205778491, 1987240989, 3446018461, 2407533397, 3151958893, 1553147067, 208156801, 2362352445, 2458343227, 4134443, 36216853, 932983869, 2800766507, 252990279, 2994662963, 2760285623, 4510445, 1458512423, 3500568231, 689831701, 887836659, 315834449, 2394075311, 1360826347, 439713319, 633358329, 749540625, 444867375, 531150885, 2871421439, 2347294453, 3975247983, 3255073387, 3561466319, 2616895667, 742825395, 3300710079, 1231551531, 3576325163, 3229203393, 2662941725, 3495109109, 2202779339, 2997513035, 1952088617, 2177967115, 1685362661, 2160536397, 2628206479, 1678152567, 775989269, 2114809421, 3882162141, 3267509575, 3869378355, 283353181, 306744579, 2793152333, 1454134621, 3021652657, 1664069155, 1711688171, 1264486497, 359065375, 1616608617};

 uint32_t c2[101] = {0, 3727790985, 1655242260, 422784933, 2834380338, 4079603720, 1017777578, 1055049545, 825468350, 3746952992, 2417510437, 3900896500, 3136156509, 1967993956, 884863111, 4005736455, 1938983485, 2483034815, 1473738861, 1601812014, 1032880017, 678118779, 1812018788, 3051015163, 2813145762, 682451094, 951775451, 3820751955, 2228245394, 1056831682, 427537107, 2657761231, 3814309543, 3334270873, 3235290147, 966385569, 1334131699, 2416080521, 2435664499, 1659112141, 2691180285, 2923984717, 221396509, 1668769566, 1550424660, 380560680, 842750068, 1766885112, 4154190178, 2485286538, 3541066000, 1618584604, 2380482404, 2292025459, 114224687, 2440503753, 2185819824, 3056187596, 1938153078, 1168725776, 816653688, 3394169238, 2371002911, 1307887949, 593463004, 2931928778, 3974621746, 2809084272, 2034840031, 771132519, 8056062, 1459555392, 313600432, 822723327, 102584381, 3018185789, 396652004, 1414061560, 3226032953, 2027177418, 3746841614, 3506805383, 184340437, 169978587, 294242210, 1958086314, 3662203479, 251991695, 2970678332, 3854518895, 3111516179, 1642607091, 1669640538, 3180287192, 1557513074, 3712923940, 3226089967, 396996256, 3520232177, 1934744235, 3239017990};
  int idx = 0;
  std::string hashStr = "";
  for (int i = 0; i < b; ++i) {
    //add separator for each block
    if (i > 0) {
      hashStr.push_back('|');
    }
    uint64_t minHash = 0xFFFFFFFF;
    for (auto s : signatures) {
      uint64_t x = (c1[idx] * s + c2[idx]) % p;
      if (x < minHash) {
        minHash = x;
      }
    }
    hashStr += std::to_string(minHash);
    idx++;
  }
  oTokenLen = hashStr.size();
  for (int i = 0; i < hashStr.size(); ++i) {
    oToken[i] = hashStr[i];
  }
  oToken[oTokenLen] = '\0';
}

/**
 *  Unit testing of the token bank functions
 */
/*int main(){

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
*/
