package com.chenpp.graph.neo4j;

import lombok.extern.slf4j.Slf4j;

/**
 * @author April.Chen
 * @date 2025/7/14 10:01
 */
@Slf4j
public class CypherBuilder {

    public static String buildCreateSingleIndex(String indexName, String labelName, String propertyName) {
        if (indexName == null || indexName.isEmpty()) {
            indexName = String.format("idx_%s_%s", labelName, propertyName);
        }
        return "CREATE INDEX " + indexName + " ON " + labelName + "(" + propertyName + ")";
    }

    public static String buildCreateCompositeIndex(String indexName, String labelName, String... propertyNames) {
        if (propertyNames == null || propertyNames.length == 0) {
            log.warn("Property names array is null or empty");
            return "";
        }

        if (indexName == null || indexName.isEmpty()) {
            indexName = String.format("idx_%s_%s", labelName, String.join("_", propertyNames));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ").append(indexName).append(" ON ");

        sb.append(labelName).append("(");
        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i] == null || propertyNames[i].isEmpty()) {
                log.warn("Property name at index {} is null or empty, skipping", i);
                continue;
            }

            sb.append(propertyNames[i]);
            if (i < propertyNames.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}