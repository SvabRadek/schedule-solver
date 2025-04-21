package com.cocroachden.scheduler.solver.utils;

public class ScheduleParserException extends RuntimeException {
    public ScheduleParserException(String message) {
        super(message);
    }

    public static ScheduleParserException becauseCouldNotReadField(String fieldName) {
        return new ScheduleParserException("Could not read field " + fieldName);
    }

    public static ScheduleParserException becauseCouldNotFindSheet(String sheetName) {
        return new ScheduleParserException("Could not find sheet " + sheetName);
    }
}
