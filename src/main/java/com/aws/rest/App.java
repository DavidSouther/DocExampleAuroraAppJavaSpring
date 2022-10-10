package com.aws.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import software.amazon.awssdk.regions.Region;

@SpringBootApplication
public class App {
    public static final Region region = Region.US_EAST_1;
    static final String database = "auroraappdb";

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
