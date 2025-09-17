package com.chenpp.graph.nebula.ngql;

import com.chenpp.graph.nebula.exception.NebulaException;
import com.chenpp.graph.nebula.schema.NebulaDataType;
import com.chenpp.graph.nebula.schema.NebulaEdge;
import com.chenpp.graph.nebula.schema.NebulaIndex;
import com.chenpp.graph.nebula.schema.NebulaProperty;
import com.chenpp.graph.nebula.schema.NebulaTag;
import com.chenpp.graph.nebula.schema.SchemaType;
import com.chenpp.graph.nebula.schema.Space;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.chenpp.graph.nebula.schema.NebulaProperty.FIXED_STRING_SIZE;

/**
 * NGQL 构建器
 *
 * @author April.Chen
 * @date 2025/7/8 15:22
 */
@Slf4j
public class NGQLBuilder {
    protected final ZoneOffset zoneOffset;

    public NGQLBuilder(ZoneOffset zoneOffset) {
        this.zoneOffset = zoneOffset;
    }


    /**
     * 返回值的字面量字符串表示
     */
    protected String asLiteralString(Object object) {
        if (object == null) {
            return "null";
        } else if (object instanceof String) {
            String str = (String) object;
            // same as cn.tongdun.yuntu.haina.storage.db.nebula.pool.SSNebulaImportHelper.buildInsertValues
            if (str.contains("\"") || str.contains("'")) {
                str = str.replace("\"", "").replace("'", "");
            }
            return "\"" + str + "\"";
        } else if (object instanceof LocalDate) {
            LocalDate date = (LocalDate) object;
            return "date(\"" + date.format(DateTimeFormatter.ISO_DATE) + "\")";
        } else if (object instanceof LocalTime) {
            LocalTime time = (LocalTime) object;
            LocalTime target = OffsetTime.of(time, OffsetDateTime.now().getOffset()).withOffsetSameInstant(zoneOffset).toLocalTime();
            return "time(\"" + target.format(DateTimeFormatter.ISO_TIME) + "\")";
        } else if (object instanceof OffsetTime) {
            OffsetTime time = (OffsetTime) object;
            LocalTime target = time.withOffsetSameInstant(zoneOffset).toLocalTime();
            return "time(\"" + target.format(DateTimeFormatter.ISO_TIME) + "\")";
        } else if (object instanceof Date) {
            Date date = (Date) object;
            LocalDateTime target = date.toInstant().atOffset(ZoneOffset.UTC).withOffsetSameInstant(zoneOffset).toLocalDateTime();
            return "datetime(\"" + target.format(DateTimeFormatter.ISO_DATE_TIME) + "\")";
        } else if (object instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) object;
            LocalDateTime target = OffsetDateTime.of(dateTime, OffsetDateTime.now().getOffset()).withOffsetSameInstant(zoneOffset).toLocalDateTime();
            return "datetime(\"" + target.format(DateTimeFormatter.ISO_DATE_TIME) + "\")";
        } else if (object instanceof OffsetDateTime) {
            OffsetDateTime dateTime = (OffsetDateTime) object;
            LocalDateTime target = dateTime.withOffsetSameInstant(zoneOffset).toLocalDateTime();
            return "datetime(\"" + target.format(DateTimeFormatter.ISO_DATE_TIME) + "\")";
        } else if (object instanceof Instant) {
            return String.valueOf(((Instant) object).getEpochSecond());
        } else {
            return object.toString();
        }
    }

    protected String getPipedVertexId() {
        return "$-.VertexID";
    }

    protected String getPipedEdgeId() {
        return "$-.SrcVID -> $-.DstVID @$-.Ranking";
    }

    public String buildUseSpace(String spaceName) {
        return "USE " + spaceName;
    }

    public String buildCreateSpace(Space space) {
        return "CREATE SPACE IF NOT EXISTS " + space.getName()
                + " (PARTITION_NUM = " + space.getPartitionNum()
                + ", REPLICA_FACTOR = " + space.getReplicaFactor()
                + ", VID_TYPE = FIXED_STRING(" + space.getVidFixedStrLength() + "))";
    }

    public String buildDropSpace(String spaceName) {
        return "DROP SPACE IF EXISTS " + spaceName;
    }


    public String buildCreateProperty(NebulaProperty nebulaProperty) {
        StringBuilder builder = new StringBuilder();
        builder.append(nebulaProperty.getName()).append(" ").append(nebulaProperty.getDataType());

        if (NebulaDataType.FIXED_STRING == nebulaProperty.getDataType()) {
            builder.append("(").append(FIXED_STRING_SIZE).append(")");
        }

        if (BooleanUtils.isFalse(nebulaProperty.isNullable())) {
            builder.append(" NOT NULL");
        } else {
            builder.append(" NULL");
        }
        // 如果 默认值表达式不为空，那么使用 默认值表达式 设置默认值
        if (StringUtils.isNotBlank(nebulaProperty.getDefaultValueExpr())) {
            builder.append(" DEFAULT ").append(nebulaProperty.getDefaultValueExpr());
        }
        // 如果 默认值表达式为空，而 默认值不为空，那么使用 默认值 设置默认值
        else if (nebulaProperty.getDefaultValue() != null) {
            builder.append(" DEFAULT ").append(asLiteralString(nebulaProperty.getDefaultValue()));
        }

        return builder.toString();
    }

    public String buildCreateProperties(List<NebulaProperty> properties, String delimiter) {
        String ret = "";
        if (properties != null && !properties.isEmpty()) {
            ret = properties.stream().filter(Objects::nonNull).map(this::buildCreateProperty).collect(Collectors.joining(delimiter));
        }
        return ret;
    }

    public void createTag(NebulaTag tag) throws NebulaException {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TAG IF NOT EXISTS ").append(tag.getName())
                .append(" (").append(buildCreateProperties(tag.getProperties(), ", ")).append(")");

        if (tag.getTtlDuration() != null && tag.getTtlCol() != null) {
            if (!tag.getProperties().stream().map(NebulaProperty::getName).collect(Collectors.toSet()).contains(tag.getTtlCol())) {
                log.warn("TTL PropertyType name is not exist in PropertyTypes to be created");
                throw new NebulaException("TTL PropertyType name is not exist in PropertyTypes to be created");
            } else {
                builder.append(" TTL_DURATION = ").append(tag.getTtlDuration())
                        .append(", TTL_COL = ").append("\"").append(tag.getTtlCol()).append("\"");
            }
        }
    }

    public String buildAlterTag(Alter alter, NebulaTag tag) throws NebulaException {
        if (tag.getProperties() == null || tag.getProperties().isEmpty()) {
            //  如果是修改 TTL，那么 属性列表可以为空
            log.warn("TTL PropertyType name is not exist in PropertyTypes to be created");
            throw new NebulaException("TTL PropertyType name is not exist in PropertyTypes to be created");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("ALTER TAG ").append(tag.getName());
        if (Alter.ADD == alter || Alter.CHANGE == alter) {
            builder.append(" ").append(alter).append(" (").append(buildCreateProperties(tag.getProperties(), ", ")).append(")");
        } else if (alter == Alter.DROP) {
            builder.append(" ").append(alter).append(" (").append(tag.getProperties().stream().map(NebulaProperty::getName).collect(Collectors.joining(", "))).append(")");
        } //  TTL_DEFINITION
        return builder.toString();
    }

    public String buildDescribeTag(String tagName) {
        return "DESCRIBE TAG " + tagName;
    }

    public String buildShowTags() {
        return "SHOW TAGS";
    }

    public String buildDropTag(String tagName) {
        return "DROP TAG IF EXISTS " + tagName;
    }

    public String buildCreateEdge(NebulaEdge edge) throws NebulaException {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE EDGE IF NOT EXISTS ").append(edge.getName())
                .append(" (").append(buildCreateProperties(edge.getProperties(), ", ")).append(")");

        if (edge.getTtlDuration() != null && edge.getTtlCol() != null) {
            if (!edge.getProperties().stream().map(NebulaProperty::getName).collect(Collectors.toSet()).contains(edge.getTtlCol())) {
                log.warn("TTL PropertyType name is not exist in PropertyTypes to be created");
                throw new NebulaException("TTL PropertyType name is not exist in PropertyTypes to be created");
            } else {
                builder.append(" TTL_DURATION = ").append(edge.getTtlDuration())
                        .append(", TTL_COL = ").append("\"").append(edge.getTtlCol()).append("\"");
            }
        }
        return builder.toString();
    }

    public String buildAlterEdge(Alter alter, NebulaEdge edgeType) throws NebulaException {
        if (edgeType.getProperties() == null || edgeType.getProperties().isEmpty()) {
            //  如果是修改 TTL，那么 属性列表可以为空
            log.warn("TTL PropertyType name is not exist in PropertyTypes to be created");
            throw new NebulaException("TTL PropertyType name is not exist in PropertyTypes to be created");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("ALTER EDGE ").append(edgeType.getName());
        if (Alter.ADD == alter || Alter.CHANGE == alter) {
            builder.append(" ").append(alter).append(" (").append(buildCreateProperties(edgeType.getProperties(), ", ")).append(")");
        } else if (alter == Alter.DROP) {
            builder.append(" ").append(alter).append(" (").append(edgeType.getProperties().stream().map(NebulaProperty::getName).collect(Collectors.joining(", "))).append(")");
        }  //  TTL_DEFINITION
        return builder.toString();
    }

    public String buildDescribeEdge(String edgeTypeName) {
        return "DESCRIBE EDGE " + edgeTypeName;
    }

    public String buildShowEdgeTypes() {
        return "SHOW EDGES";
    }

    public String buildDropEdge(String edgeTypeName) {
        return "DROP EDGE IF EXISTS " + edgeTypeName;
    }

    public String buildCreateIndex(NebulaIndex index) {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE ").append(index.getIndexType()).append(" INDEX IF NOT EXISTS ").append(index.getIndexName())
                .append(" ON ").append(index.getTypeName()).append(" (");
        if (index.getPropNameList() != null && !index.getPropNameList().isEmpty()) {
            builder.append(String.join(", ", index.getPropNameList()));
        }
        builder.append(")");
        return builder.toString();
    }

    public String buildShowCreateIndex(SchemaType schemaType, String indexName) {
        return "SHOW CREATE " + schemaType + " INDEX " + indexName;
    }

    public String buildDescribeIndex(SchemaType schemaType, String indexName) {
        return "DESCRIBE " + schemaType + " INDEX " + indexName;
    }

    public String buildShowIndexes(SchemaType schemaType) {
        return "SHOW " + schemaType + " INDEXES";
    }

    public String buildDropIndex(SchemaType schemaType, String indexName) {
        return "DROP " + schemaType + " INDEX IF EXISTS " + indexName;
    }

}
