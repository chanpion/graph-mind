package com.chenpp.graph.admin;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author April.Chen
 * @date 2025/8/1 15:30
 */
public class SecurityTest {

    @Test
    public void testPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String pass = passwordEncoder.encode("admin123");
        System.out.println(pass);
        System.out.println(passwordEncoder.matches("admin123", pass));
    }
}
