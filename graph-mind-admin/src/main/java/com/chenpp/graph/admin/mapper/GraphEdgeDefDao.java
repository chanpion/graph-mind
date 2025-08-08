package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenpp.graph.admin.model.GraphEdgeDef;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图边定义Mapper接口
 *
 * @author April.Chen
 * @date 2025/8/4 15:30
 */
@Mapper
public interface GraphEdgeDefDao extends BaseMapper<GraphEdgeDef> {
}