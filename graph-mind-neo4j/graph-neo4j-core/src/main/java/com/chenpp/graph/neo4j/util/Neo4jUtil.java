package com.chenpp.graph.neo4j.util;

import java.util.Map;

/**
 * @author April.Chen
 * @date 2025/4/30 13:52
 */
public class Neo4jUtil {

    public static String buildPropertiesClause(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder().append(" {");
        int index = 0;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (index > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(": $").append(entry.getKey());
            index++;
        }
        sb.append("} ");
        return sb.toString();
    }
}
