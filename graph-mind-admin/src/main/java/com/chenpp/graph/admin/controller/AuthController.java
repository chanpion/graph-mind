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

    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        log.info("用户登录：{}", loginRequest.getUsername());
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtil.generateToken(loginRequest.getUsername());
        LoginResponse loginResponse = new LoginResponse(token, 24 * 60 * 60 * 1000L);
        loginResponse.setUsername(loginRequest.getUsername());

        return ResponseEntity.ok(Result.success(loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Result<String>> logout() {
        log.info("用户登出");
        SecurityContextHolder.clearContext();
        Result<String> result = Result.success("登出成功", "");
        return ResponseEntity.ok(result);
    }
}