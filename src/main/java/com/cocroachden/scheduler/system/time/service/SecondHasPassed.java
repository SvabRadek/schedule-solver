package com.cocroachden.scheduler.system.time.service;

import com.cocroachden.scheduler.domain.Event;

import java.time.Instant;

public record SecondHasPassed(Instant instant) implements Event {
}
