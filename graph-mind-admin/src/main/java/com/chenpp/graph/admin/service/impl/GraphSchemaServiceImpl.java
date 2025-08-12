package com.chenpp.graph.admin.service.impl;

import com.chenpp.graph.admin.service.GraphSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author April.Chen
 * @date 2025/8/12 15:46
 */
@Slf4j
@Service
public class GraphSchemaServiceImpl implements GraphSchemaService {

    @Override
    public void publishSchema(Long graphId) {
        log.info("发布图Schema: {}", graphId);
        //todo
    }
}
