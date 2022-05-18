package com.tigergraph.jdbc.log;

public class Util {

    /**
     * Convert slf4j message format to java.util.logging style
     * "hello {}, this is {}" -> "hello {0}, this is {1}"
     * 
     * @param format
     * @return
     */
    public static String formatConverter(String format) {
        StringBuilder sb = new StringBuilder();
        int argCount = 0;
        for (int i = 0; i < format.length(); i++) {
            if (i < format.length() - 1 && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
                sb.append(String.format("{%d}", argCount));
                argCount++;
                i++;
            } else {
                sb.append(format.charAt(i));
            }
        }
        return sb.toString();
    }

    public static final String getSysProperty(String key) {
        try {
            String val = System.getProperty(key);
            return val;
        } catch (SecurityException e) {
            System.out.printf(">>> Unable to get %s: %s, assume a null value.\n", key, e.getMessage());
            return null;
        }
    }
}
