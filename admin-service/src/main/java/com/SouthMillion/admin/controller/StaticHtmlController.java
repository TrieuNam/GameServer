package com.SouthMillion.admin.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to serve static HTML files
 */
@Controller
public class StaticHtmlController {

    @GetMapping("/control-panel.html")
    public ResponseEntity<String> controlPanel() throws IOException {
        return serveStaticFile("static/control-panel.html");
    }

    @GetMapping("/process-manager.html")
    public ResponseEntity<String> processManager() throws IOException {
        return serveStaticFile("static/process-manager.html");
    }

    @GetMapping("/doctor.html")
    public ResponseEntity<String> doctorDashboard() throws IOException {
        return serveStaticFile("static/doctor.html");
    }

    private ResponseEntity<String> serveStaticFile(String path) throws IOException {
        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String content = StreamUtils.copyToString(
            resource.getInputStream(), 
            StandardCharsets.UTF_8
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(content);
    }
}
