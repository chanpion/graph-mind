package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenpp.graph.admin.model.GraphEdgeProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图边属性定义Mapper接口
 *
 * @author April.Chen
 * @date 2025/8/4 15:50
 */
@Mapper
public interface GraphEdgePropertyDao extends BaseMapper<GraphEdgeProperty> {
}