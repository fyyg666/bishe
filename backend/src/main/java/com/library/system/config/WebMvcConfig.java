package com.library.system.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RedisCacheInterceptor redisCacheInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(redisCacheInterceptor)
                .addPathPatterns("/books", "/categories", "/statistics/overview", "/borrows/rules");
    }

    @Bean
    public FilterRegistrationBean<Filter> contentCachingFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                if (response instanceof HttpServletResponse httpServletResponse
                        && !(response instanceof ContentCachingResponseWrapper)) {
                    ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(httpServletResponse);
                    chain.doFilter(request, wrapper);
                    wrapper.copyBodyToResponse();
                } else {
                    chain.doFilter(request, response);
                }
            }
        });
        registration.addUrlPatterns("/books", "/categories", "/statistics/overview", "/borrows/rules");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("contentCachingFilter");
        return registration;
    }
}
