package com.tigergraph.jdbc.common;

import com.tigergraph.jdbc.log.TGLoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;

import org.slf4j.Logger;


public class Array implements java.sql.Array {

    private static final Logger logger = TGLoggerFactory.getLogger(Array.class);

    protected String typeName;
    protected Object[] elements;

    protected Array(String typeName, Object[] elements) throws SQLException {
        if (typeName == null) {
            logger.error("typeName cannot be null");
            throw new SQLException("typeName cannot be null");
        }
        if (elements == null) {
            logger.error("elements cannot be null");
            throw new SQLException("elements cannot be null");
        }
        this.typeName = typeName.toUpperCase();
        // Remove null elements
        this.elements = Arrays.stream(elements).filter(Objects::nonNull).toArray(Object[]::new);
    }

    @Override
    public String getBaseTypeName() throws SQLException {
        return this.typeName;
    }

    @Override
    public int getBaseType() throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Object getArray() throws SQLException {
        return elements;
    }

    @Override
    public void free() throws SQLException {
        elements = null;
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }

    @Override
    public Object getArray(Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Object getArray(long index, int count) throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public ResultSet getResultSet(long index, int count) throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
