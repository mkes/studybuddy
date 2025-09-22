package com.studytracker.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;

/**
 * Configuration for WebClient with SSL handling for Canvas API integration.
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${canvas.api.base-url}")
    private String canvasBaseUrl;

    @Value("${canvas.api.ssl.trust-all-certs:false}")
    private boolean trustAllCerts;

    @Bean
    public WebClient canvasWebClient() {
        try {
            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(30))
                    .followRedirect(true);

            // Configure SSL if needed
            if (trustAllCerts) {
                log.warn("SSL certificate verification is disabled for Canvas API - this should only be used in development!");
                SslContext sslContext = SslContextBuilder
                        .forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
            }

            return WebClient.builder()
                    .baseUrl(canvasBaseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (SSLException e) {
            log.error("Failed to configure SSL for WebClient", e);
            // Fallback to default WebClient
            return WebClient.builder()
                    .baseUrl(canvasBaseUrl)
                    .build();
        }
    }
}