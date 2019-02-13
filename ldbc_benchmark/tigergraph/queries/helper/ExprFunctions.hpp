// Add this user-defined function in <tigergraph.root.dir>/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
  inline string bigint_to_string (double val) {
    char result[200];
    sprintf(result, "%.0f", val);
    return string(result);
  }