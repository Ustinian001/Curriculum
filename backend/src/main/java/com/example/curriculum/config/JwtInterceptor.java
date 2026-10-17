package com.example.curriculum.config;

import com.example.curriculum.common.Result;
import com.example.curriculum.utils.JwtUtil;
import com.example.curriculum.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 登录拦截器：校验 token，并按路径前缀做角色权限控制
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 从请求头取出 token（格式：Bearer xxx）
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            return writeError(response, 401, "未登录");
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);
            UserContext.set(userId, role);

            // 角色权限控制：按 URL 前缀限制
            String uri = request.getRequestURI();
            if (uri.startsWith("/student") && !"student".equals(role)) {
                return writeError(response, 403, "无权限");
            }
            if (uri.startsWith("/teacher") && !"teacher".equals(role)) {
                return writeError(response, 403, "无权限");
            }
            if (uri.startsWith("/admin") && !"admin".equals(role)) {
                return writeError(response, 403, "无权限");
            }
            return true;
        } catch (Exception e) {
            return writeError(response, 401, "登录已过期，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清除线程上下文，防止内存泄漏
        UserContext.clear();
    }

    private boolean writeError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
        return false;
    }
}
