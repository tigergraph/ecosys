package com.tigergraph.jdbc;

import org.apache.spark.sql.jdbc.JdbcDialect;
import org.apache.spark.sql.jdbc.JdbcType;
import org.apache.spark.sql.types.*;
import org.apache.spark.sql.execution.datasources.jdbc.JdbcUtils;
import java.sql.Types;

import scala.Option;

public class TigergraphDialect extends JdbcDialect {
    
    public TigergraphDialect() {
        super();
    }

    @Override
    public boolean canHandle(String url) {
        return url.startsWith("jdbc:tg");
    }

    @Override
    public Option<DataType> getCatalystType(int sqlType, String typeName, int size, MetadataBuilder md) {
        if (sqlType == Types.ARRAY) {
            // LIST.ELEMENT_TYPE => ELEMENT_TYPE
            Option<DataType> elementType = toCatalystType(typeName.split("\\.")[1]);
            if (!elementType.isEmpty()) {
                return Option.apply(new ArrayType(elementType.get(), false));
            }
        }
        return Option.empty();
    }

    /**
     * Get element type of an array.
     * For TG LIST/SET, only support int, double, string, datetime or UDT as their element type in DDL.
     */
    private Option<DataType> toCatalystType(String typeName) {
        switch (typeName.toLowerCase()) {
            // For basic types but not supported in collection.
            // Will throw unsupportedJdbcTypeError.
            case "bool":
            case "uint":
            case "string compress":
            case "float":
                return Option.empty();
            // For supported TG collection element type.
            // The query response of retrieving LIST<DOUBLE> might be parsed as Integer/Long[] if the double is actually an integer,
            // then Spark will force casting Integer/Long to Double instead of calling `setDouble()`, which can cause ClassCastException.
            // In case of the situation above, both TG LIST<INT> and LIST<DOUBLE> will be converted to Scala Array[Double].
            case "int":
            case "double":
                return Option.apply(DataTypes.DoubleType);
            // For string, datatime and UDT
            case "string":
            case "datetime":
            default:
                return Option.apply(DataTypes.StringType);
        }
    }

    @Override
    public Option<JdbcType> getJDBCType(DataType dt) {
        if (dt instanceof ArrayType) {
            // Nested LIST/SET/BAG are not supported by TG DDL.
            if (((ArrayType) dt).elementType() instanceof AtomicType) {
                Option<JdbcType> jdbcType = JdbcUtils.getCommonJDBCType(((ArrayType) dt).elementType());
                if (!jdbcType.isEmpty()) {
                    return Option.apply(new JdbcType(jdbcType.get().databaseTypeDefinition() + "()", Types.ARRAY));
                }
            }
        }
        return Option.empty();
    }
}
