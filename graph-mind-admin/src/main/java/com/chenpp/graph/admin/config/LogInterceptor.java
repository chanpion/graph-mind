package com.chenpp.graph.admin.config;

import com.chenpp.graph.admin.constant.Constants;
import com.chenpp.graph.admin.model.OperationLog;
import com.chenpp.graph.admin.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 日志拦截器，记录所有API请求的详细信息
 *
 * @author April.Chen
 * @date 2025/8/4 10:30
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    public LogInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Long startTime = (Long) request.getAttribute("startTime");
        long costTime = 0;
        if (startTime != null) {
            costTime = System.currentTimeMillis() - startTime;
        }

        String username = extractUsernameFromRequest(request);

        String uri = request.getRequestURI();
        String method = request.getMethod();

        OperationLog operationLog = new OperationLog();
        operationLog.setUsername(username);
        operationLog.setUri(uri);
        operationLog.setMethod(method);
        operationLog.setTime(costTime);
        operationLog.setStatusCode(response.getStatus());
        operationLog.setIp(getClientIp(request));
        operationLog.setCreateTime(LocalDateTime.now());

        log.info("API Request - URI: {}, Method: {}, Username: {}, Cost: {}ms", uri, method, username, costTime);
    }

    /**
     * 从请求中提取用户名
     *
     * @param request HTTP请求
     * @return 用户名
     */
    private String extractUsernameFromRequest(HttpServletRequest request) {
        String token = jwtUtil.getJwtTokenFromRequest(request);
        if (token != null && token.startsWith(Constants.BEARER_PREFIX)) {
            token = token.substring(Constants.BEARER_PREFIX_LENGTH);

        }
        try {
            return jwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            // JWT解析失败，继续尝试其他方式
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }

        return Constants.ANONYMOUS_USER;
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        return Arrays.stream(ipHeaders)
                .map(request::getHeader)
                .filter(ip -> StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip))
                .map(ip -> ip.split(",")[0])
                .findFirst()
                .orElse(request.getRemoteAddr());
    }
}