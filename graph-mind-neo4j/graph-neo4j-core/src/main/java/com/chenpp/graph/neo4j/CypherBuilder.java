package com.chenpp.graph.neo4j;

/**
 * @author April.Chen
 * @date 2025/7/14 10:01
 */
public class CypherBuilder {

    public static String buildCreateSingleIndex(String indexName, String labelName, String propertyName) {
        return "CREATE INDEX " + indexName + " ON " + labelName + "(" + propertyName + ")";
    }

    public static String buildCreateCompositeIndex(String indexName, String labelName, String... propertyNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ").append(indexName).append(" ON ").append(labelName).append("(");
        for (int i = 0; i < propertyNames.length; i++) {
            sb.append(propertyNames[i]);
            if (i < propertyNames.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}


