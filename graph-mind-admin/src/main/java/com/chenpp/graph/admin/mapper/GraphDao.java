package com.chenpp.graph.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenpp.graph.admin.model.Graph;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图管理Mapper接口
 *
 * @author April.Chen
 * @date 2025/8/1 17:00
 */
@Mapper
public interface GraphDao extends BaseMapper<Graph> {
}