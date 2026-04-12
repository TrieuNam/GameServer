package com.SouthMillion.gameworld_service.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * In local dev, make gameworld-service also listen on port 85 so the game
 * client can fetch battle report files directly at:
 *   http://localhost:85/h02_{spid}_s{fsid}/fightdata/common_pve/{type}/{battleId}
 *
 * Activate by setting gameworld.battle.extra-http-port=85 in application-local.yml.
 * In production, nginx proxies port 85 traffic to this service's main port instead.
 */
@Slf4j
@Configuration
@Profile("local")
public class LocalBattlePortConfig {

    @Value("${gameworld.battle.extra-http-port:0}")
    private int extraPort;

    @Bean
    @ConditionalOnProperty(name = "gameworld.battle.extra-http-port")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> battlePortCustomizer() {
        return factory -> {
            if (extraPort > 0) {
                Connector connector = new Connector("HTTP/1.1");
                connector.setPort(extraPort);
                factory.addAdditionalTomcatConnectors(connector);
                log.info("[LocalBattlePortConfig] Listening for battle reports on extra port {}", extraPort);
            }
        };
    }
}
