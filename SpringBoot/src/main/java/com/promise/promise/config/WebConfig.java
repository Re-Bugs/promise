package com.promise.promise.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") // 모든 경로에 대해 인터셉터를 적용
                .excludePathPatterns("/login", "/login/sign_up", "/resources/**", "/error", "/api/login/**", "/health"); // 로그인, 회원가입, 정적 자원, 에러 페이지는 예외
    }

//    @Override
//    public void addCorsMappings(CorsRegistry registry)
//    {
//        registry.addMapping("/api/**")
//                .allowedOrigins("localhost") // 올바른 도메인 설정
//                .allowedMethods("GET", "POST", "PUT", "DELETE")
//                .allowCredentials(true);
//    }
}
