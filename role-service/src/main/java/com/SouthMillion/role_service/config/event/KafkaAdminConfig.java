package com.SouthMillion.role_service.config.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaAdminConfig {

    @Bean
    public NewTopic bagGrantTopic(@Value("${app.kafka.bag-grant-topic:gameh5.bag.grant}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
