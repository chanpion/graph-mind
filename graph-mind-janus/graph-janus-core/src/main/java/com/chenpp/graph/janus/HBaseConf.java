package com.chenpp.graph.janus;

import lombok.Data;

/**
 * @author April.Chen
 * @date 2024/5/13 17:17
 */
@Data
public class HBaseConf {

    private String hbasePort;
    private String hbaseHost;
    private String hbaseZnode;
    private String hbaseNs = "default";
    private int hbaseRegionNum = 16;
    private String rootdir = "/hbase";
    private String regionServerPrincipal;
    private String masterPrincipal;
}
