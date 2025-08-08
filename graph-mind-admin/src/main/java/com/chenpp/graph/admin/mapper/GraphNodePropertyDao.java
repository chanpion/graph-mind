package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenpp.graph.admin.model.GraphNodeProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图节点属性定义Mapper接口
 *
 * @author April.Chen
 * @date 2025/8/4 15:40
 */
@Mapper
public interface GraphNodePropertyDao extends BaseMapper<GraphNodeProperty> {
}