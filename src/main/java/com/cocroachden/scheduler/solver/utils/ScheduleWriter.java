package com.cocroachden.scheduler.solver.utils;

import com.cocroachden.scheduler.domain.Vocabulary;
import com.cocroachden.scheduler.solver.Availability;
import com.cocroachden.scheduler.solver.AvailabilityType;
import com.cocroachden.scheduler.solver.EmployeeSchedule;
import com.cocroachden.scheduler.solver.ShiftType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScheduleWriter {

    public static final Map<DayOfWeek, String> WEEK_DAY_TRANSLATIONS = new HashMap<>();
    private final Vocabulary vocabulary;
    private XSSFCellStyle WEEKEND_STYLE;
    private XSSFCellStyle DEFAULT_STYLE;
    private XSSFCellStyle NAME_STYLE;
    private XSSFCellStyle CORRECT_STYLE;
    private XSSFCellStyle FAILED_STYLE;

    static {
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.MONDAY, "Po");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.TUESDAY, "Ut");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.WEDNESDAY, "St");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.THURSDAY, "Ct");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.FRIDAY, "Pa");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.SATURDAY, "So");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.SUNDAY, "Ne");
    }

    private XSSFCellStyle createDefaultStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private XSSFCellStyle createNameStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.getFont().setBold(true);
        return style;
    }

    private XSSFCellStyle createCorrectStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        return style;
    }

    private XSSFCellStyle createFailedStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        return style;
    }

    private XSSFCellStyle createWeekendStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        return style;
    }

    @SneakyThrows
    public void write(EmployeeSchedule schedule, File output) {
        var wb = new XSSFWorkbook();
        WEEKEND_STYLE = this.createWeekendStyle(wb);
        DEFAULT_STYLE = this.createDefaultStyle(wb);
        NAME_STYLE = this.createNameStyle(wb);
        CORRECT_STYLE = this.createCorrectStyle(wb);
        FAILED_STYLE = this.createFailedStyle(wb);
        var newDetailedSheet = wb.createSheet("New detailed");
        final var startRow = 4;
        final var startColumn = 0;
        var lastRow = startRow;
        lastRow = this.writeHeader(schedule, newDetailedSheet, lastRow, startColumn);
        lastRow = this.writeDetailedSchedule(schedule, newDetailedSheet, lastRow + 1, startColumn);
        lastRow = this.writeFooter(schedule, newDetailedSheet, lastRow + 1, startColumn);
        newDetailedSheet.createFreezePane(startColumn + 1, lastRow + 1);
        var lastCol = newDetailedSheet.getRow(startRow).getLastCellNum();
        for (int col = startColumn; col < lastCol + 1; col++) {
            newDetailedSheet.autoSizeColumn(col);
        }
        this.createScheduleSheet(schedule, wb);
        this.createDetailedScheduleSheet(schedule, wb);
        try (var outStream = new FileOutputStream(output)) {
            wb.write(outStream);
            wb.close();
        }
    }

    private Integer writeHeader(EmployeeSchedule schedule, XSSFSheet sheet, Integer row, Integer startColumn) {
        var currentColumn = new AtomicInteger(startColumn);
        var headerRow = sheet.createRow(row);
        headerRow.createCell(currentColumn.getAndIncrement()).setCellValue(vocabulary.translate("Name"));
        schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1))
                .forEach(date -> {
                    var cell = headerRow.createCell(currentColumn.getAndIncrement());
                    var content = date.getDayOfMonth() + " " + WEEK_DAY_TRANSLATIONS.get(date.getDayOfWeek());
                    cell.setCellValue(content);
                    cell.setCellStyle(DEFAULT_STYLE);
                    if (date.getDayOfWeek().getValue() > 5) {
                        cell.setCellStyle(WEEKEND_STYLE);
                    }
                });
        Stream.of("Count of D", "Count of N", "Count of V", "Total")
              .forEach(content -> {
                  var cell = headerRow.createCell(currentColumn.getAndIncrement());
                  cell.setCellValue(vocabulary.translate(content));
                  cell.setCellStyle(DEFAULT_STYLE);
              });
        return row + 1;
    }

    private Integer writeDetailedSchedule(EmployeeSchedule schedule, XSSFSheet sheet, Integer startRow, Integer startColumn) {
        var currentColumn = new AtomicInteger(startColumn);
        var currentRow = new AtomicInteger(startRow);
        var scheduleLength = ChronoUnit.DAYS.between(schedule.getStartDate(), schedule.getEndDate());
        schedule.getEmployees().forEach(employee -> {
            var employeeRow = sheet.createRow(currentRow.getAndIncrement());
            currentColumn.set(startColumn);
            var nameCell = employeeRow.createCell(currentColumn.getAndIncrement());
            nameCell.setCellValue(employee.getEmployeeId().id());
            nameCell.setCellStyle(NAME_STYLE);
            schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1))
                    .forEach(date -> {
                        var availabilities = schedule.getAvailabilities().stream()
                                                     .filter(a -> a.employee().equals(employee) && a.date().equals(date))
                                                     .toList();

                        var assignment = schedule.getShiftAssignments().stream()
                                                 .filter(a -> a.getEmployee().equals(employee))
                                                 .filter(a -> a.getDate().equals(date))
                                                 .findAny();
                        var assignmentSymbol = assignment
                                .map(a -> a.getShiftType().getSymbol())
                                .orElse("");
                        var cell = employeeRow.createCell(currentColumn.getAndIncrement());
                        cell.setCellValue(assignmentSymbol);
                        if (availabilities.isEmpty()) {
                            cell.setCellStyle(DEFAULT_STYLE);
                        } else if (availabilities.size() == 2) {
                            if (assignment.isEmpty()) {
                                cell.setCellStyle(CORRECT_STYLE);
                            } else {
                                cell.setCellStyle(FAILED_STYLE);
                            }
                        } else if (availabilities.size() == 1) {
                            ShiftType shouldHaveAssign;
                            var availability = availabilities.get(0);
                            shouldHaveAssign = switch (availability.shiftType()) {
                                case DAY -> switch (availability.type()) {
                                    case DESIRED, REQUIRED -> ShiftType.DAY;
                                    case UNDESIRED, UNAVAILABLE -> null;
                                };
                                case NIGHT -> switch (availability.type()) {
                                    case DESIRED, REQUIRED -> ShiftType.NIGHT;
                                    case UNDESIRED, UNAVAILABLE -> null;
                                };
                            };
                            //TODO finish this
                            if (assignment.isEmpty()) {
                                if (shouldHaveAssign == null) {
                                    cell.setCellStyle(CORRECT_STYLE);
                                } else {

                                }

                            } else {

                            }
                            //TODO fix this.
                            if ( assignment.get().equals(shouldHaveAssign)) {
                                cell.setCellStyle(CORRECT_STYLE);
                            } else {
                                cell.setCellStyle(FAILED_STYLE);
                            }
                        }
                    });
            Stream.of(
                    employee.getAssignmentInfo().getDayShifts(),
                    employee.getAssignmentInfo().getNightShifts(),
                    (int) scheduleLength - employee.getAssignmentInfo().getTotalCount(),
                    employee.getAssignmentInfo().getTotalCount()
            ).forEach(value -> {
                var cell = employeeRow.createCell(currentColumn.getAndIncrement());
                cell.setCellValue(value);
                cell.setCellStyle(DEFAULT_STYLE);
            });
        });
        return currentRow.get();
    }

    private Integer writeFooter(EmployeeSchedule schedule, XSSFSheet sheet, Integer startRow, Integer startColumn) {
        var currentRow = new AtomicInteger(startRow);
        Stream.of("Count of D", "Count of N", "Count of V", "Total").forEach(content -> {
            var cell = sheet.createRow(currentRow.getAndIncrement()).createCell(startColumn);
            cell.setCellValue(content);
            cell.setCellStyle(DEFAULT_STYLE);
        });
        var currentColumn = new AtomicInteger(startColumn + 1);
        schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1))
                .forEach(date -> {
                    var assignmentsForTheDay = schedule.getShiftAssignments().stream()
                                                       .filter(sa -> sa.getDate().equals(date))
                                                       .toList();
                    var countOfD = assignmentsForTheDay.stream()
                                                       .filter(sa -> sa.getShiftType().equals(ShiftType.DAY))
                                                       .count();
                    var countOfN = assignmentsForTheDay.stream()
                                                       .filter(sa -> sa.getShiftType().equals(ShiftType.NIGHT))
                                                       .count();
                    var countOfV = schedule.getAvailabilities().stream()
                                           .filter(a -> a.date().equals(date))
                                           .collect(Collectors.groupingBy(Availability::employee))
                                           .values().stream()
                                           .filter(
                                                   availabilities -> availabilities.size() > 1
                                                           && availabilities.stream().allMatch(a -> a.type().equals(AvailabilityType.UNAVAILABLE)))
                                           .count();
                    currentRow.set(startRow);
                    Stream.of(countOfD, countOfN, countOfV, (long) assignmentsForTheDay.size())
                          .forEach(value -> {
                              var cell = sheet.getRow(currentRow.getAndIncrement()).createCell(currentColumn.get());
                              cell.setCellValue(value);
                              cell.setCellStyle(DEFAULT_STYLE);
                          });
                    currentColumn.getAndIncrement();
                });
        return startRow + 3;
    }

    private void createDetailedScheduleSheet(final EmployeeSchedule schedule, final XSSFWorkbook wb) {
        var sheet = wb.createSheet("Detailni rozvrh");
        var header = sheet.createRow(0);
        var col = new AtomicInteger(1);
        schedule.getStartDate()
                .datesUntil(schedule.getEndDate().plusDays(1))
                .forEach(localDate -> {
                    var cell = header.createCell(col.getAndIncrement());
                    cell.setCellValue(localDate.getDayOfMonth() + " " + WEEK_DAY_TRANSLATIONS.get(localDate.getDayOfWeek()));
                    if (localDate.getDayOfWeek().getValue() > 5) {
                        cell.getCellStyle().getFont().setBold(true);
                    }
                });
        var row = new AtomicInteger(1);
        schedule.getEmployees().forEach(employee -> {
            var employeeRow = sheet.createRow(row.getAndIncrement());
            employeeRow.createCell(0).setCellValue(employee.getEmployeeId().id() + " (%s)".formatted(employee.getShiftAssignments().size()));
            schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1))
                    .forEach(date -> {
                        var availabilities = schedule.getAvailabilities().stream()
                                                     .filter(a -> a.employee().equals(employee) && a.date().equals(date))
                                                     .toList();
                        var availabilitySymbol = "";
                        if (availabilities.size() == 2) {
                            availabilitySymbol = "V -> ";
                        } else if (availabilities.size() == 1) {
                            availabilitySymbol = availabilities.get(0).type().getSymbol() + availabilities.get(0).shiftType().getSymbol() + " -> ";
                        }
                        var assignment = schedule.getShiftAssignments().stream()
                                                 .filter(a -> a.getEmployee().equals(employee))
                                                 .filter(a -> a.getDate().equals(date))
                                                 .findAny()
                                                 .map(a -> a.getShiftType().getSymbol())
                                                 .orElse("");
                        var daysSinceScheduleStart = (int) ChronoUnit.DAYS.between(schedule.getStartDate(), date);
                        employeeRow.createCell(daysSinceScheduleStart + 1)
                                   .setCellValue(availabilitySymbol + assignment);
                    });
        });

    }

    //TODO refactor different parts of results into different methods
    //TODO add assigned shift statistics to the result
    private void createScheduleSheet(final EmployeeSchedule schedule, final XSSFWorkbook wb) {
        var headerWorkdayStyle = wb.createCellStyle();
        var headerWeekendStyle = wb.createCellStyle();
        var assignmentStyle = wb.createCellStyle();
        headerWorkdayStyle.getFont().setBold(true);
        headerWorkdayStyle.setFillForegroundColor(IndexedColors.BLUE.index);
        headerWeekendStyle.getFont().setBold(true);
        headerWeekendStyle.setFillForegroundColor(IndexedColors.RED.index);
        assignmentStyle.getFont().setBold(true);
        var sheet = wb.createSheet("Rozvrh");
        var header = sheet.createRow(0);
        var col = new AtomicInteger(1);
        schedule.getStartDate()
                .datesUntil(schedule.getEndDate().plusDays(1))
                .forEach(localDate -> {
                    var cell = header.createCell(col.getAndIncrement());
                    cell.setCellValue(localDate.getDayOfMonth() + " " + WEEK_DAY_TRANSLATIONS.get(localDate.getDayOfWeek()));
                    if (localDate.getDayOfWeek().getValue() > 5) {
                        cell.setCellStyle(headerWeekendStyle);
                    } else {
                        cell.setCellStyle(headerWorkdayStyle);
                    }
                });
        var row = new AtomicInteger(1);
        schedule.getEmployees().forEach(employee -> {
            var employeeRow = sheet.createRow(row.getAndIncrement());
            employeeRow.createCell(0).setCellValue(employee.getEmployeeId().id() + " (%s)".formatted(employee.getShiftAssignments().size()));
            schedule.getShiftAssignments().stream()
                    .filter(shiftAssignment -> shiftAssignment.getEmployee().equals(employee))
                    .forEach(shiftAssignment -> {
                        var daysSinceScheduleStart = (int) ChronoUnit.DAYS.between(schedule.getStartDate(), shiftAssignment.getDate());
                        var cell = employeeRow.createCell(daysSinceScheduleStart + 1);
                        cell.setCellValue(shiftAssignment.getShiftType().name().substring(0, 1));
                        cell.setCellStyle(assignmentStyle);
                    });
        });
    }

}
