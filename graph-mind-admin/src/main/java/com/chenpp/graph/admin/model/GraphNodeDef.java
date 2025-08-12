package com.chenpp.graph.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图节点定义
 *
 * @author April.Chen
 * @date 2025/8/4 15:00
 */
@TableName(value = "graph_node_def", excludeProperty = {"properties"})
@Data
public class GraphNodeDef {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图ID
     */
    private Long graphId;

    /**
     * 标签
     */
    private String label;
    /**
     * 节点类型名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：0-未发布，1-已发布
     */
    private Integer status = 1;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 节点属性列表
     */
    private List<GraphPropertyDef> properties;
    
    /**
     * 获取包含默认uid属性的属性列表
     * @return 属性列表
     */
    public List<GraphPropertyDef> getPropertiesWithUid() {
        if (properties != null) {
            // 检查是否已存在uid属性
            boolean hasUid = properties.stream()
                    .anyMatch(prop -> "uid".equals(prop.getCode()));
            
            // 如果不存在uid属性，则添加默认的uid属性
            if (!hasUid) {
                GraphPropertyDef uidProperty = new GraphPropertyDef();
                uidProperty.setCode("uid");
                uidProperty.setName("唯一标识");
                uidProperty.setType("string");
                uidProperty.setDesc("节点唯一标识符");
                uidProperty.setStatus(1);
                uidProperty.setPropertyType("node");
                properties.add(0, uidProperty); // 将uid属性放在第一位
            }
        }
        return properties;
    }
}