package com.cocroachden.scheduler.solver;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AvailabilityType {
    DESIRED("+"),
    UNDESIRED("-"),
    UNAVAILABLE("#"),
    REQUIRED("");

    private final String symbol;
}
