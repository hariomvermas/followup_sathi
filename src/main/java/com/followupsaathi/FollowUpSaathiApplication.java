package com.followupsaathi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the FollowUp Saathi backend.
 *
 * The class lives at the base package so that component scanning finds every
 * controller, service and repository in the layers below it.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FollowUpSaathiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FollowUpSaathiApplication.class, args);
    }
}
