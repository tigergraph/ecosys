package com.tigergraph.jdbc.log;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import static org.junit.Assert.*;

public class TGLoggerFactoryTest {

    @Test
    public void shouldGetJULByDefault(){
        TGLoggerFactory.initializeLogger(2);
        Logger logger = TGLoggerFactory.getLogger(TGLoggerFactoryTest.class);
        assertTrue(logger instanceof JULAdapter);
        assertEquals("JUL_DEFAULT", TGLoggerFactory.getLoggerType());
    }

    @Test
    public void shouldGetJULWithNonSLF4JProperty(){
        System.setProperty("com.tigergraph.jdbc.loggerImpl", "arbitrary_impl");
        TGLoggerFactory.initializeLogger(2);
        Logger logger = TGLoggerFactory.getLogger(TGLoggerFactoryTest.class);
        assertTrue(logger instanceof JULAdapter);
        assertEquals("JUL_DEFAULT", TGLoggerFactory.getLoggerType());
        System.clearProperty("com.tigergraph.jdbc.loggerImpl");

    }

    @Test
    public void shouldGetJULWithConfig(){
        System.setProperty("java.util.logging.config.file", "path_to_config_file");
        TGLoggerFactory.initializeLogger(2);
        Logger logger = TGLoggerFactory.getLogger(TGLoggerFactoryTest.class);
        assertTrue(logger instanceof JULAdapter);
        assertEquals("JUL_WITH_CONFIG", TGLoggerFactory.getLoggerType());
        System.clearProperty("java.util.logging.config.file");
    }

    @Test
    public void shouldGetSLF4JLoggerWithSLF4JProperty(){
        System.setProperty("com.tigergraph.jdbc.loggerImpl", "SLF4J");
        TGLoggerFactory.initializeLogger(2);
        Logger logger = TGLoggerFactory.getLogger(TGLoggerFactoryTest.class);
        assertFalse(logger instanceof JULAdapter);
        assertEquals("SLF4J", TGLoggerFactory.getLoggerType());
        System.clearProperty("com.tigergraph.jdbc.loggerImpl");
    }

    @Test
    public void shouldGetSLF4JLoggerWithSLF4JPropertyIgnoreCase(){
        System.setProperty("com.tigergraph.jdbc.loggerImpl", "sLf4J");
        TGLoggerFactory.initializeLogger(2);
        Logger logger = TGLoggerFactory.getLogger(TGLoggerFactoryTest.class);
        assertFalse(logger instanceof JULAdapter);
        assertEquals("SLF4J", TGLoggerFactory.getLoggerType());
        System.clearProperty("com.tigergraph.jdbc.loggerImpl");
    }
}
