package com.cocroachden.scheduler.solver.utils;

import java.time.format.DateTimeFormatter;

public class ScheduleProperties {

    public static final DateTimeFormatter SCHEDULE_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yy");

    public static final String ASSIGNMENT_SHEET_NAME = "Assignment";
    public static final String ASSIGNMENT_WB_NAME = "Assignment";
    public static final String RESULT_WB_NAME = "Result";

    public static final Integer DAY_SHIFT_PPL_COUNT_ROW = 3;
    public static final Integer NIGHT_SHIFT_PPL_COUNT_ROW = 4;

    public static final Coordinates START_DATE_VALUE_CELL = new Coordinates(1, 1);
    public static final Coordinates END_DATE_VALUE_CELL = new Coordinates(2, 1);
    public static final Coordinates PEOPLE_ON_DAY_SHIFT = new Coordinates(3, 1);
    public static final Coordinates PEOPLE_ON_NIGHT_SHIFT = new Coordinates(4, 1);

    public static final Coordinates HEADER_START = new Coordinates(6, 0);
    public static final Coordinates SCHEDULE_TABLE_START = new Coordinates(7, 0);


}
