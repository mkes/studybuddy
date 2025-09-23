package com.studytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudyTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyTrackerApplication.class, args);
    }

}