package com.cocroachden.scheduler.system.time.service;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class SystemTimeService {

    private final ApplicationEventPublisher publisher;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void oneSecond() {
        publisher.publishEvent(new SecondHasPassed(Instant.now()));
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void oneMinute() {
        publisher.publishEvent(new MinuteHasPassed(Instant.now()));
    }

}
