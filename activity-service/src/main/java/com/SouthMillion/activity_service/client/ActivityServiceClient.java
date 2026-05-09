package com.SouthMillion.activity_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ActivityServiceClient {

    private final RestTemplate restTemplate;
    private final String activityServiceBaseUrl;

    public ActivityServiceClient(RestTemplate restTemplate, 
                                 @Value("${activity.service.baseUrl}") String activityServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.activityServiceBaseUrl = activityServiceBaseUrl;
    }

    public String getActivityById(Long id) {
        String url = String.format("%s/activity/%d", activityServiceBaseUrl, id);
        return restTemplate.getForObject(url, String.class);
    }

    // Other methods that use activityServiceBaseUrl instead of hardcoded values...
}