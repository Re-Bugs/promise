package com.onlypromise.promise.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
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
                .excludePathPatterns("/login", "/login/sign_up", "/static/**", "/images/**", "/error", "/api/**", "/health");
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
