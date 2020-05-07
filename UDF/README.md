# UDF Library

* [String Based UDF](#string-based-udf)
  * [substring](#substring) - Given a string of text, return a substring from index begin to index end.
  * [str_regex_match](#string-regex-match) - Given a string determine if regex matches, return a boolean.

## String BASED UDF
### substring
Given a string of text, return a substring from index begin to index end

| Variable | Description| Example |
| -------- | -------- | -------- |
| str    | input string of text     | The Apple is Red   | 
| b    | index of string you would like to begin with     | index 4 = A     |
| e    |index of string you would like to end with    | index 8 = e|

**UDF Code**
```
inline string substring(string str, int b, int e) {
return str.substr (b,e);
}
```

**Example**
str = "The Apple is Red"
substring(s, 4, 8)

return = Apple

-----------

### str_regex_match
Given a string determine if regex matches, return a boolean.

| Column 1 | Column 2 | Column 3 |
| -------- | -------- | -------- |
| Text     | Text     | Text     |

**UDF Code**
```
inline bool str_regex_match (string toCheck, string regEx) {
bool res = false;
// only performs the check when the input is valid
try {
  res = boost::regex_match(toCheck, boost::regex(regEx));
} catch (boost::regex_error err) {
  res = false;
}
return res;
}
```
**Example**
