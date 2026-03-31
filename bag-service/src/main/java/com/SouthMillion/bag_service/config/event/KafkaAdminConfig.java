package com.SouthMillion.bag_service.config.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaAdminConfig {
    @Bean
    public NewTopic bagChangedTopic(
            @Value("${app.kafka.bag-changed-topic:gameh5.bag.changed}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}