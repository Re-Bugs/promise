package com.onlypromise.promise.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) return true; // 로그인된 경우 요청을 허용
        else
        {
            String requestURI = request.getRequestURI();
            if (requestURI.startsWith("/api"))
            {
                // API 요청일 경우 JSON 응답 반환
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"message\": \"Unauthorized\"}");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            else response.sendRedirect("/login"); //웹 요청일 경우 로그인 페이지로 리다이렉트
            return false; // 요청을 차단
        }
    }
}
