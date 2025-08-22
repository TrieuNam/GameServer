package com.SouthMillion.user_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Id id = new Id();
    private H5 h5 = new H5();

    @Getter @Setter
    public static class Id {
        private String prefix = "";
    }

    @Getter @Setter
    public static class H5 {
        private long skewSeconds = 300;
        /** map spid -> secret */
        private Map<String,String> channels;
    }
}