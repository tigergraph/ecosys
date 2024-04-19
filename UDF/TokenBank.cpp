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

uint32_t MurmurHash2(const void *key, int len, uint32_t seed)
{
    // 'm' and 'r' are mixing constants generated offline.
    // They're not really 'magic', they just happen to work well.

    const uint32_t m = 0x5bd1e995;
    const int r = 24;

    // Initialize the hash to a 'random' value

    uint32_t h = seed ^ len;

    // Mix 4 bytes at a time into the hash

    const unsigned char *data = (const unsigned char *)key;

    while (len >= 4)
    {
        uint32_t k = *(uint32_t *)data;

        k *= m;
        k ^= k >> r;
        k *= m;

        h *= m;
        h ^= k;

        data += 4;
        len -= 4;
    }

    // Handle the last few bytes of the input array

    switch (len)
    {
    case 3:
        h ^= data[2] << 16;
    case 2:
        h ^= data[1] << 8;
    case 1:
        h ^= data[0];
        h *= m;
    };

    // Do a few final mixes of the hash to ensure the last few
    // bytes are well-incorporated.

    h ^= h >> 13;
    h *= m;
    h ^= h >> 15;

    return h;
}

extern "C" void minHash(const char *const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum,
                        char *const oToken, uint32_t &oTokenLen)
{

    if (iTokenLen[0] == 0)
    {
        oToken[0] = '\0';
        oTokenLen = 0;
        return;
    }
    // minHash take three input (int str, int shingleLen, int b, int r)
    uint32_t shingleLen = atoi(iToken[1]);
    int b = atoi(iToken[2]);
    // only handle the case for the number of hash functions is less or equal to 100
    uint32_t k = std::min(iTokenLen[0], shingleLen);
    char tmp[k];
    std::vector<uint32_t> signatures;
    // std::cout << "start signatures\n";
    for (int i = 0; i < iTokenLen[0] - k + 1; ++i)
    {
        signatures.push_back(MurmurHash2(&iToken[0][i], k, 0));
        // std::cout << "token: " << std::string(&iToken[0][i], k) << ", MurmurHash2: " <<  MurmurHash2(&iToken[0][i], k, 0) << std::endl;
    }
    // std::cout << "end signatures\n";
    // x = (a*x + b) % p
    uint64_t p = 4294967311; // the prime number bigger than 2^32 - 1
    uint32_t c1[101] = {1, 482067061, 1133999723, 2977257653, 2666089543, 3098200677, 3985189319, 1857983931, 3801236429, 522456919, 4057595205, 4176190031, 1652234975, 2294716503, 1644020081, 3407377875, 3749518465, 4244672803, 3053397733, 3273255487, 598272097, 3989043777, 1414109747, 697129027, 67285677, 98002333, 158583451, 1424122447, 2159224677, 3478101309, 277468927, 1902928727, 2459288935, 3941065327, 1244061689, 1898521317, 4205778491, 1987240989, 3446018461, 2407533397, 3151958893, 1553147067, 208156801, 2362352445, 2458343227, 4134443, 36216853, 932983869, 2800766507, 252990279, 2994662963, 2760285623, 4510445, 1458512423, 3500568231, 689831701, 887836659, 315834449, 2394075311, 1360826347, 439713319, 633358329, 749540625, 444867375, 531150885, 2871421439, 2347294453, 3975247983, 3255073387, 3561466319, 2616895667, 742825395, 3300710079, 1231551531, 3576325163, 3229203393, 2662941725, 3495109109, 2202779339, 2997513035, 1952088617, 2177967115, 1685362661, 2160536397, 2628206479, 1678152567, 775989269, 2114809421, 3882162141, 3267509575, 3869378355, 283353181, 306744579, 2793152333, 1454134621, 3021652657, 1664069155, 1711688171, 1264486497, 359065375, 1616608617};

    uint32_t c2[101] = {0, 3727790985, 1655242260, 422784933, 2834380338, 4079603720, 1017777578, 1055049545, 825468350, 3746952992, 2417510437, 3900896500, 3136156509, 1967993956, 884863111, 4005736455, 1938983485, 2483034815, 1473738861, 1601812014, 1032880017, 678118779, 1812018788, 3051015163, 2813145762, 682451094, 951775451, 3820751955, 2228245394, 1056831682, 427537107, 2657761231, 3814309543, 3334270873, 3235290147, 966385569, 1334131699, 2416080521, 2435664499, 1659112141, 2691180285, 2923984717, 221396509, 1668769566, 1550424660, 380560680, 842750068, 1766885112, 4154190178, 2485286538, 3541066000, 1618584604, 2380482404, 2292025459, 114224687, 2440503753, 2185819824, 3056187596, 1938153078, 1168725776, 816653688, 3394169238, 2371002911, 1307887949, 593463004, 2931928778, 3974621746, 2809084272, 2034840031, 771132519, 8056062, 1459555392, 313600432, 822723327, 102584381, 3018185789, 396652004, 1414061560, 3226032953, 2027177418, 3746841614, 3506805383, 184340437, 169978587, 294242210, 1958086314, 3662203479, 251991695, 2970678332, 3854518895, 3111516179, 1642607091, 1669640538, 3180287192, 1557513074, 3712923940, 3226089967, 396996256, 3520232177, 1934744235, 3239017990};
    int idx = 0;
    std::string hashStr = "";
    for (int i = 0; i < b; ++i)
    {
        // add separator for each block
        if (i > 0)
        {
            hashStr.push_back('|');
        }
        uint64_t minHash = 0xFFFFFFFF;
        for (auto s : signatures)
        {
            uint64_t x = (c1[idx] * s + c2[idx]) % p;
            if (x < minHash)
            {
                minHash = x;
            }
        }
        hashStr += std::to_string(minHash);
        idx++;
    }
    oTokenLen = hashStr.size();
    for (int i = 0; i < hashStr.size(); ++i)
    {
        oToken[i] = hashStr[i];
    }
    oToken[oTokenLen] = '\0';
}

extern "C" void get_dt_now(const char *const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum,
                           char *const oToken, uint32_t &oTokenLen)
{
    std::time_t now_t = std::time(0);
    char buf_string[64];
    strftime(buf_string, sizeof(buf_string),
             "%Y-%m-%d %H:%M:%S",
             localtime(&now_t));
    std::string dt_str(buf_string);
    for (int i = 0; i < dt_str.length(); i++)
    {
        oToken[i] = dt_str[i];
    }
    oTokenLen = dt_str.length();
}
