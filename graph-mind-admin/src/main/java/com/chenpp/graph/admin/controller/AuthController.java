package com.chenpp.graph.admin.controller;

import com.chenpp.graph.admin.model.LoginRequest;
import com.chenpp.graph.admin.model.LoginResponse;
import com.chenpp.graph.admin.model.Result;
import com.chenpp.graph.admin.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author April.Chen
 * @date 2025/8/1 18:30
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录接口
     *
     * @param loginRequest 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        log.info("用户登录：{}", loginRequest.getUsername());
        try {
            String password = passwordEncoder.encode(loginRequest.getPassword());
            // 创建认证令牌
            UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

            // 执行认证
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 设置安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 生成JWT令牌
            String token = jwtUtil.generateToken(loginRequest.getUsername());

            // 创建登录响应
            LoginResponse loginResponse = new LoginResponse(token, 24 * 60 * 60 * 1000L);

            return ResponseEntity.ok(Result.success(loginResponse));
        } catch (Exception e) {
            Result<LoginResponse> errorResult = Result.error(401, "用户名或密码错误", null);
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 用户登出接口
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout() {
        log.info("用户登出");
        // 清除安全上下文
        SecurityContextHolder.clearContext();
        Result<String> result = Result.success("登出成功", "");
        return ResponseEntity.ok(result);
    }
}