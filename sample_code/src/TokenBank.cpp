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

#include "TokenLib.hpp"

/**
 * This function convert iToken char array of size iTokenLen, reverse order
 * and put it in oToken.
 *
 * Only one token input.
 */
extern "C"  void ReverseStr (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum,
    char *const oToken, uint32_t& oTokenLen) {

  uint32_t j = 0;
  for (uint32_t i = iTokenLen[0]; i > 0; i--) {
    oToken[j++] = iToken[0][i-1];
  }
  oTokenLen = j;
}

