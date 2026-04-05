package com.SouthMillion.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration for static resources
 * HIGHEST_PRECEDENCE to override Spring Boot Admin default mappings
 */
@Configuration
public class WebConfig implements WebMvcConfigurer, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources with high priority
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .setCachePeriod(0)
                .resourceChain(false);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward root to control-panel.html
        registry.addViewController("/").setViewName("forward:/control-panel.html");
        registry.addViewController("/doctor").setViewName("forward:/doctor.html");
    }
}
