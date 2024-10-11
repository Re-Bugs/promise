package com.onlypromise.promise.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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

    //외부 이미지 접근
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "/image/**" 경로 요청은 "file:현재 작업 디렉토리 경로/image/"에서 파일을 찾도록 매핑
        registry.addResourceHandler("/image/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/image/");
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
