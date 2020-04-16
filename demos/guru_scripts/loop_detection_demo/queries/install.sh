gdev=$(grep gdev ~/.gsql/gsql.cfg | cut -d " " -f 2)
gdk=$(grep GDKDir ${gdev}/gdk/gsql/config | cut -d '=' -f 2)
cp ExprFunctions.hpp $gdev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
if [ $? -eq 0 ]; then
  echo -e "cp ExprFunctions.hpp successful\n"
else
  echo -e $gdev" is the system dev path"
  echo -e "cp expressionFunctions/ExprFunctions.hpp fails !!!\n exit"
  exit 1
fi
gsql install_queries.gsql
