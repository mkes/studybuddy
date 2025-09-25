package com.studytracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Email configuration for testing when SMTP is not available
 */
@Configuration
public class EmailTestConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailTestConfig.class);
    
    /**
     * Create a test mail sender when no SMTP credentials are provided
     */
    @Bean
    @ConditionalOnProperty(name = "spring.mail.username", havingValue = "", matchIfMissing = true)
    public JavaMailSender testMailSender() {
        logger.warn("No SMTP credentials configured. Creating test mail sender that will log emails instead of sending them.");
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Set up a basic configuration that won't actually send emails
        mailSender.setHost("localhost");
        mailSender.setPort(25);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "true");
        
        return mailSender;
    }
}