package com.cocroachden.scheduler;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SchedulerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SchedulerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
//        SpringApplication.run(SchedulerApplication.class, args);
    }

}
