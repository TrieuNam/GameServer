package com.SouthMillion.drop_service.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.SouthMillion.dto.drop.CompiledDrop;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DropBeans {
    @Bean
    public Cache<Integer, CompiledDrop> dropCompiledCache(AppProperties p) {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(p.getCache().getCompiledTtlMinutes()))
                .maximumSize(p.getCache().getCompiledMaxSize())
                .build();
    }

    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }
}