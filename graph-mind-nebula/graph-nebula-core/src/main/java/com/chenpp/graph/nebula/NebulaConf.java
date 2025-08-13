package com.chenpp.graph.nebula;

import com.chenpp.graph.core.model.GraphConf;
import lombok.Data;

/**
 * @author April.Chen
 * @date 2024/5/15 10:25
 */
@Data
public class NebulaConf extends GraphConf {
    private static final long serialVersionUID = -8700103886209913425L;

    private String hosts;
    private int port;
    private String username;
    private String password;
    private String space;
    /**
     * partition_num 表示数据分片数量。默认值为 100。建议为硬盘数量的 5 倍。
     */
    private int partitionNum;
    /**
     * replica_factor 表示副本数量。默认值是 1，生产集群建议为 3。由于采用多数表决原理，因此需为奇数。
     */
    private int replicaFactor;

    private int vidFixedStrLength;
}
