package com.cocroachden.scheduler.solver;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShiftType {
    DAY("D"),
    NIGHT("N");
    private final String symbol;
}
