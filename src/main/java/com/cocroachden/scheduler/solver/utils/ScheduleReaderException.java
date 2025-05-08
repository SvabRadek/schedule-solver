package com.cocroachden.scheduler.solver.utils;

public class ScheduleReaderException extends RuntimeException {
    public ScheduleReaderException(String message) {
        super(message);
    }

    public static ScheduleReaderException becauseCouldNotReadField(String fieldName) {
        return new ScheduleReaderException("Could not read field " + fieldName);
    }

    public static ScheduleReaderException becauseCouldNotFindSheet(String sheetName) {
        return new ScheduleReaderException("Could not find sheet " + sheetName);
    }

    public static ScheduleReaderException becauseCellNotFound(String cellName, String address) {
        return new ScheduleReaderException("Could not find %s cell value at %s".formatted(cellName, address));
    }

    public static ScheduleReaderException becauseUnknownShiftSymbol(String symbol, String address) {
        return new ScheduleReaderException("Unknown shift symbol (%s) at cell %s".formatted(symbol, address));
    }
}
