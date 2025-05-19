package com.cocroachden.scheduler.solver.utils;

public class SheetRange {

    private final Coordinates start;
    private Coordinates end;

    public SheetRange(final Coordinates start) {
        this.start = start;
        this.end = start;
    }

    public void setEnd(final Coordinates end) {
        if (end == null) {
            return;
        }
        this.end = end;
    }

    public String asA1() {
        if (start == end) {
            return ExcelUtils.toA1(start);
        }
        return ExcelUtils.toA1(start) + ":" + ExcelUtils.toA1(end);
    }

}
