package com.cocroachden.scheduler.system.time.service;

import com.cocroachden.scheduler.domain.Event;

import java.time.Instant;

public record MinuteHasPassed(Instant instant) implements Event {
}
