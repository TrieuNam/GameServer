package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.client.ConfigFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Service
public class ItemService {
    @Autowired
    private ConfigFeignClient configFeign;

    public Object getItemConfig(String name) { return configFeign.getConfig(name); }
}

