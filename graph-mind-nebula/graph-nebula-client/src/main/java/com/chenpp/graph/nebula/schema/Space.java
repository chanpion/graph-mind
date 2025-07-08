package com.chenpp.graph.nebula.schema;

import lombok.Builder;

/**
 * 图空间
 *
 * @author April.Chen
 * @date 2025/7/8 15:24
 */
@Builder
public class Space {

    public static final int DEFAULT_PARTITION_NUM = 15;

    public static final int DEFAULT_REPLICA_FACTOR = 3;

    public static final int DEFAULT_VID_FIXED_STRING_LENGTH = 32;

    /**
     * 图空间名
     */
    private String name;

    /**
     * 分区数
     */
    private int partitionNum = DEFAULT_PARTITION_NUM;

    /**
     * 副本数
     */
    private int replicaFactor = DEFAULT_REPLICA_FACTOR;

    /**
     * vid 数据类型固定为字符串，最大长度
     */
    private int vidFixedStrLength = DEFAULT_VID_FIXED_STRING_LENGTH;

    public String getName() {
        return name;
    }

    public int getPartitionNum() {
        return partitionNum;
    }

    public int getReplicaFactor() {
        return replicaFactor;
    }

    public int getVidFixedStrLength() {
        return vidFixedStrLength;
    }

    @Override
    public String toString() {
        return "Space{" +
                "name='" + name + '\'' +
                ", partitionNum=" + partitionNum +
                ", replicaFactor=" + replicaFactor +
                ", vidFixedStrLength=" + vidFixedStrLength +
                '}';
    }
}
