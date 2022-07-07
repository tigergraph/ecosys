package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.Array;
import com.tigergraph.jdbc.log.TGLoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.slf4j.Logger;

public class RestppArray extends Array {

    private static final Logger logger = TGLoggerFactory.getLogger(RestppConnection.class);

    public RestppArray(String typeName, Object[] elements) throws SQLException {
        super(typeName, elements);

        if (this.elements.length == 0) {
            return;
        }

        switch (this.typeName) {
            case "SHORT":
            case "INTEGER":
                checkClass(Integer.class, Short.class);
                break;
            case "BYTE":
                checkClass(Byte.class);
                break;
            case "BIGINT":
            case "LONG":
                checkClass(Long.class);
                break;
            case "DOUBLE":
            case "DOUBLE PRECISION":
                checkClass(Double.class);
                break;
            case "REAL":
            case "FLOAT":
                checkClass(Float.class);
                break;
            case "BIT":
            case "BOOLEAN":
                checkClass(Boolean.class);
                break;
            case "TEXT":
            case "STRING":
                checkClass(String.class);
                break;
            case "TIMESTAMP":
                checkClass(Timestamp.class);
                break;
            case "DATE":
                checkClass(Date.class);
                break;
            case "DECIMAL":
                checkClass(BigDecimal.class);
                break;
            case "BINARY":
            case "BLOB":
                checkClass(byte[].class);
                break;
            case "ANY":
                break;
            default:
                logger.error("Unsupported element type: {}", this.typeName);
                throw new SQLException("Unsupported element type: " + this.typeName);
        }
    }

    private void checkClass(Class<?>... expected) throws SQLException {
        for (int i = 0; i < expected.length; i++) {
            if (expected[i].isInstance(this.elements[0]))
                return;
        }
        throw new SQLException("For array of " + getBaseTypeName() + ", elements of type "
                + expected + " are expected.");
    }

    private void checkClass(Class<?> expected) throws SQLException {
        if (!expected.isInstance(this.elements[0]))
            throw new SQLException("For array of " + getBaseTypeName() + ", elements of type "
                    + expected + " are expected.");

    }
}
