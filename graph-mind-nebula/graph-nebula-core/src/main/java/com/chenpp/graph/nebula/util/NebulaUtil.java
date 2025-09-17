package com.chenpp.graph.nebula.util;

import com.chenpp.graph.core.schema.GraphEntity;
import com.chenpp.graph.core.schema.GraphIndex;
import com.chenpp.graph.core.schema.GraphRelation;
import com.chenpp.graph.nebula.NebulaConf;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

/**
 * @author April.Chen
 * @date 2025/4/9 15:24
 */
@Slf4j
public class NebulaUtil {

    public static String buildCreateSpace(NebulaConf nebulaConf) {
        return "CREATE SPACE IF NOT EXISTS " + nebulaConf.getSpace() + " (PARTITION_NUM = " + nebulaConf.getPartitionNum() + ", REPLICA_FACTOR = " + nebulaConf.getReplicaFactor() + ", VID_TYPE = FIXED_STRING(" + nebulaConf.getVidFixedStrLength() + "))";
    }

    public static String buildDropSpace(String spaceName) {
        return "DROP SPACE IF EXISTS " + spaceName;
    }

    public static void createUniqueIndex(NebulaConf nebulaConf, GraphIndex index) {
        String vertexIndex = "CREATE UNIQUE INDEX <index_name> ON <tag_name> (<property_name> [, <property_name> ...]);";
        String edgeIndex = "CREATE UNIQUE INDEX <index_name> ON <edge_type> (<property_name> [, <property_name> ...]);";
        String idx = "CREATE INDEX person_name_age_index ON person (name, age);";
        String dropIndex = "DROP INDEX person_name_index;";
    }

    public static String buildCreateTag(GraphEntity entity) {
        StringBuilder tagBuilder = new StringBuilder();
        String properties = entity.getProperties().stream().map(prop -> prop.getCode() + " " + prop.getDataType().name())
                .collect(Collectors.joining(", "));
        tagBuilder.append("CREATE TAG IF NOT EXISTS ").append(entity.getLabel()).append(" (").append(properties).append(")");

        return tagBuilder.toString();
    }

    public static String buildCreateEdge(GraphRelation relation) {
        StringBuilder edgeBuilder = new StringBuilder();
        String properties = relation.getProperties().stream().map(prop -> prop.getName() + " " + prop.getDataType().name())
                .collect(Collectors.joining(", "));
        edgeBuilder.append("CREATE EDGE IF NOT EXISTS ").append(relation.getLabel()).append("(").append(properties).append(")");
        return edgeBuilder.toString();
    }

    public static String buildDescribeTag(String tagName) {
        return "DESCRIBE TAG " + tagName;
    }

    public static String buildShowTags() {
        return "SHOW TAGS";
    }

    public static String buildDropTag(String tagName) {
        return "DROP TAG IF EXISTS " + tagName;
    }

    public static String buildDescribeEdge(String edgeTypeName) {
        return "DESCRIBE EDGE " + edgeTypeName;
    }

    public static String buildShowEdges() {
        return "SHOW EDGES";
    }

    public static String buildDropEdge(String edgeTypeName) {
        return "DROP EDGE IF EXISTS " + edgeTypeName;
    }
}