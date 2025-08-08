package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenpp.graph.admin.model.GraphNodeDef;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图节点定义Mapper接口
 *
 * @author April.Chen
 * @date 2025/8/4 15:20
 */
@Mapper
public interface GraphNodeDefDao extends BaseMapper<GraphNodeDef> {
}