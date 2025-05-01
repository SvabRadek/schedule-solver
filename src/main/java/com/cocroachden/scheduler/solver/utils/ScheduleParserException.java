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

    public static ScheduleParserException becauseCellNotFound(String cellName, String address) {
        return new ScheduleParserException("Could not find %s cell value at %s".formatted(cellName, address));
    }

    public static ScheduleParserException becauseUnknownShiftSymbol(String symbol, String address) {
        return new ScheduleParserException("Unknown shift symbol (%s) at cell %s".formatted(symbol, address));
    }
}
