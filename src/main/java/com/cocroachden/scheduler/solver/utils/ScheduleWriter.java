package com.cocroachden.scheduler.solver.utils;

import com.cocroachden.scheduler.domain.Vocabulary;
import com.cocroachden.scheduler.solver.Availability;
import com.cocroachden.scheduler.solver.EmployeeSchedule;
import com.cocroachden.scheduler.solver.ShiftAssignment;
import com.cocroachden.scheduler.solver.ShiftType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.cocroachden.scheduler.solver.utils.ExcelUtils.toA1;

@Service
@RequiredArgsConstructor
public class ScheduleWriter {

    public static final Map<DayOfWeek, String> WEEK_DAY_TRANSLATIONS = new HashMap<>();
    private final Vocabulary vocabulary;
    private XSSFCellStyle WEEKEND_STYLE;
    private XSSFCellStyle WEEKEND_SCHEDULE_STYLE;
    private XSSFCellStyle DEFAULT_STYLE;
    private XSSFCellStyle DEFAULT_SCHEDULE_STYLE;
    private XSSFCellStyle CORRECT_STYLE;
    private XSSFCellStyle CORRECT_VACATION_STYLE;
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

    public void write(EmployeeSchedule schedule, File output) {
        this.write(schedule, output, "");
    }

    public void write(EmployeeSchedule schedule, File output, String summary) {
        var isBlank = schedule.getShiftAssignments().stream().noneMatch(sa -> sa.getEmployee() != null);
        var wb = new XSSFWorkbook();
        this.initializeStyles(wb);
        var resultSheet = wb.createSheet(vocabulary.translateFromEn(isBlank ? ScheduleProperties.ASSIGNMENT_SHEET_NAME : ScheduleProperties.RESULT_WB_NAME));
        this.writeSettings(schedule, resultSheet);
        this.writeHeader(schedule, resultSheet);
        var lastRow = this.writeSchedule(schedule, resultSheet, isBlank);
        this.writeFooter(schedule, resultSheet, lastRow + 1);
        resultSheet.createFreezePane(ScheduleProperties.HEADER_START.column() + 2, ScheduleProperties.HEADER_START.row() + 1);
        var lastCol = resultSheet.getRow(ScheduleProperties.HEADER_START.row()).getLastCellNum();
        for (int col = ScheduleProperties.HEADER_START.column(); col <= lastCol; col++) {
            resultSheet.autoSizeColumn(col);
            if (col == 0) {
                var width = resultSheet.getColumnWidth(col);
                resultSheet.setColumnWidth(col, width + ( 5 * 256 ));
            }
        }
        if (!summary.isBlank()) {
            var sheet = wb.createSheet(vocabulary.translateFromEn("Summary"));
            var cell = sheet.createRow(0).createCell(0);
            cell.setCellValue(summary);
            var style = wb.createCellStyle();
            style.setWrapText(true);
            cell.setCellStyle(style);
            sheet.autoSizeColumn(0);
        }
        try (var outStream = new FileOutputStream(output)) {
            wb.write(outStream);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO It will always print 3 day/3 night workers required no matter what. Fix it.
    private void writeSettings(EmployeeSchedule schedule, XSSFSheet sheet) {
        var startDateRow = sheet.createRow(ScheduleProperties.START_DATE_VALUE_CELL.row());
        var startDateHeader = startDateRow.createCell(ScheduleProperties.START_DATE_VALUE_CELL.column() - 1);
        var startDateValue = startDateRow.createCell(ScheduleProperties.START_DATE_VALUE_CELL.column());
        startDateHeader.setCellValue(vocabulary.translateFromEn("Start"));
        startDateValue.setCellValue(schedule.getStartDate().format(ScheduleProperties.SCHEDULE_DATE_FORMAT));
        startDateHeader.setCellStyle(DEFAULT_SCHEDULE_STYLE);
        startDateValue.setCellStyle(DEFAULT_STYLE);

        var endDateRow = sheet.createRow(ScheduleProperties.END_DATE_VALUE_CELL.row());
        var endDateHeader = endDateRow.createCell(ScheduleProperties.END_DATE_VALUE_CELL.column() - 1);
        var endDateValue = endDateRow.createCell(ScheduleProperties.END_DATE_VALUE_CELL.column());
        endDateHeader.setCellValue(vocabulary.translateFromEn("End"));
        endDateValue.setCellValue(schedule.getEndDate().format(ScheduleProperties.SCHEDULE_DATE_FORMAT));
        endDateHeader.setCellStyle(DEFAULT_SCHEDULE_STYLE);
        endDateValue.setCellStyle(DEFAULT_STYLE);

        var peopleOnDayShiftRow = sheet.createRow(ScheduleProperties.PEOPLE_ON_DAY_SHIFT.row());
        var peopleOnDayShiftRowHeader = peopleOnDayShiftRow.createCell(ScheduleProperties.PEOPLE_ON_DAY_SHIFT.column() - 1);
        peopleOnDayShiftRowHeader.setCellValue(vocabulary.translateFromEn("Required employee count on day shift"));
        peopleOnDayShiftRowHeader.setCellStyle(DEFAULT_SCHEDULE_STYLE);

        var peopleOnNightShiftRow = sheet.createRow(ScheduleProperties.PEOPLE_ON_NIGHT_SHIFT.row());
        var peopleOnNightShiftHeader = peopleOnNightShiftRow.createCell(ScheduleProperties.PEOPLE_ON_NIGHT_SHIFT.column() - 1);
        peopleOnNightShiftHeader.setCellValue(vocabulary.translateFromEn("Required employee count on night shift"));
        peopleOnNightShiftHeader.setCellStyle(DEFAULT_SCHEDULE_STYLE);
    }

    private void writeHeader(EmployeeSchedule schedule, XSSFSheet sheet) {
        var currentColumn = new AtomicInteger(ScheduleProperties.HEADER_START.column());
        var headerRow = sheet.createRow(ScheduleProperties.HEADER_START.row());
        var nameCell = headerRow.createCell(currentColumn.getAndIncrement());
        nameCell.setCellValue(vocabulary.translateFromEn("Name"));
        nameCell.setCellStyle(DEFAULT_STYLE);
        var shiftCountCell = headerRow.createCell(currentColumn.getAndIncrement());
        shiftCountCell.setCellValue(vocabulary.translateFromEn("Minimum shift count"));
        shiftCountCell.setCellStyle(DEFAULT_STYLE);
        schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1))
                .forEach(date -> {
                    var col = currentColumn.getAndIncrement();
                    var cell = headerRow.createCell(col);
                    var content = date.getDayOfMonth() + " " + WEEK_DAY_TRANSLATIONS.get(date.getDayOfWeek());
                    cell.setCellValue(content);
                    cell.setCellStyle(DEFAULT_STYLE);
                    if (date.getDayOfWeek().getValue() > 5) {
                        cell.setCellStyle(WEEKEND_STYLE);
                    }
                    var dayRow = sheet.getRow(ScheduleProperties.DAY_SHIFT_PPL_COUNT_ROW);
                    var nightRow = sheet.getRow(ScheduleProperties.NIGHT_SHIFT_PPL_COUNT_ROW);
                    var dayPplCountCell = dayRow.createCell(col);
                    var nightPplCountCell = nightRow.createCell(col);
                    dayPplCountCell.setCellValue(3);
                    dayPplCountCell.setCellStyle(DEFAULT_STYLE);
                    nightPplCountCell.setCellValue(3);
                    nightPplCountCell.setCellStyle(DEFAULT_STYLE);
                });
        Stream.of("Count of D", "Count of N", "Count of V", "Count of W", "Total")
              .forEach(content -> {
                  var cell = headerRow.createCell(currentColumn.getAndIncrement());
                  cell.setCellValue(vocabulary.translateFromEn(content));
                  cell.setCellStyle(DEFAULT_STYLE);
              });
    }

    //TODO add comments with original requests
    private Integer writeSchedule(EmployeeSchedule schedule, XSSFSheet sheet, final Boolean isBlank) {
        var currentColumn = new AtomicInteger(ScheduleProperties.SCHEDULE_TABLE_START.column());
        var currentRow = new AtomicInteger(ScheduleProperties.SCHEDULE_TABLE_START.row());
        schedule.getEmployees().forEach(employee -> {
            var employeeRow = sheet.createRow(currentRow.getAndIncrement());
            currentColumn.set(ScheduleProperties.SCHEDULE_TABLE_START.column());
            var nameCell = employeeRow.createCell(currentColumn.getAndIncrement());
            nameCell.setCellValue(employee.getEmployeeId().id());
            nameCell.setCellStyle(DEFAULT_SCHEDULE_STYLE);
            var assignmentsCell = employeeRow.createCell(currentColumn.getAndIncrement());
            assignmentsCell.setCellValue(employee.getMinimumShiftCount());
            if (isBlank) {
                assignmentsCell.setCellStyle(DEFAULT_STYLE);
            } else {
                if (employee.getMinimumShiftCount() <= employee.getShiftAssignments().size()) {
                    assignmentsCell.setCellStyle(CORRECT_STYLE);
                } else {
                    assignmentsCell.setCellStyle(FAILED_STYLE);
                }
            }
            var weekendCells = new ArrayList<Coordinates>();
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
                        final var currentCellCoords = Coordinates.of(employeeRow.getRowNum(), cell.getColumnIndex());
                        if (availabilities.isEmpty()) {
                            cell.setCellStyle(DEFAULT_SCHEDULE_STYLE);
                        } else if (availabilities.size() == 2) {
                            ExcelUtils.addComment(currentCellCoords, vocabulary.translateFromEn("Request") + ": V", sheet);
                            if (assignment.isEmpty()) {
                                cell.setCellStyle(CORRECT_VACATION_STYLE);
                            } else {
                                cell.setCellStyle(FAILED_STYLE);
                            }
                        } else if (availabilities.size() == 1) {
                            var availability = availabilities.get(0);
                            ExcelUtils.addComment(currentCellCoords, vocabulary.translateFromEn("Request") + ": " + availability.getSymbol(), sheet);
                            if (this.isAssignmentCorrect(assignment.map(ShiftAssignment::getShiftType).orElse(null), availability)) {
                                cell.setCellStyle(CORRECT_STYLE);
                            } else {
                                cell.setCellStyle(FAILED_STYLE);
                            }
                        }
                        if (date.getDayOfWeek().getValue() > 5) {
                            weekendCells.add(currentCellCoords);
                        }
                    });

            var formulaRow = employeeRow.getRowNum();
            var formulaFirstColumn = ScheduleProperties.SCHEDULE_TABLE_START.column() + 2;
            var formulaLastColumn = currentColumn.get() - 1;
            var countDFormula = "COUNTIF(%s:%s, \"D\")".formatted(
                    toA1(formulaRow, formulaFirstColumn),
                    toA1(formulaRow, formulaLastColumn)
            );
            var countNFormula = "COUNTIF(%s:%s, \"N\")".formatted(
                    toA1(formulaRow, formulaFirstColumn),
                    toA1(formulaRow, formulaLastColumn)
            );
            var countVFormula = "COUNTBLANK(%s:%s)".formatted(
                    toA1(formulaRow, formulaFirstColumn),
                    toA1(formulaRow, formulaLastColumn)
            );
            var countWFormula = "COUNTA(%s)".formatted(
                    String.join(", ", weekendCells.stream().map(ExcelUtils::toA1).toList())
            );
            var totalFormula = "COUNTA(%s:%s)".formatted(
                    toA1(formulaRow, formulaFirstColumn),
                    toA1(formulaRow, formulaLastColumn)
            );

            Stream.of(
                    countDFormula,
                    countNFormula,
                    countVFormula,
                    countWFormula,
                    totalFormula
            ).forEach(formula -> {
                var cell = employeeRow.createCell(currentColumn.getAndIncrement());
                cell.setCellFormula(formula);
                cell.setCellStyle(DEFAULT_STYLE);
            });
        });
        return currentRow.get();
    }

    private Boolean isAssignmentCorrect(@Nullable ShiftType assignment, Availability availability) {
        return switch (availability.type()) {
            case DESIRED, REQUIRED -> {
                if (assignment == null) {
                    yield false;
                }
                yield assignment.equals(availability.shiftType());
            }
            case UNDESIRED, UNAVAILABLE -> {
                if (assignment == null) {
                    yield true;
                }
                yield !assignment.equals(availability.shiftType());
            }
        };
    }

    private void writeFooter(EmployeeSchedule schedule, XSSFSheet sheet, Integer startRow) {
        var currentRow = new AtomicInteger(startRow);
        Stream.of("Count of D", "Count of N", "Count of V", "Total").forEach(content -> {
            var cell = sheet.createRow(currentRow.getAndIncrement()).createCell(ScheduleProperties.SCHEDULE_TABLE_START.column());
            cell.setCellValue(vocabulary.translateFromEn(content));
            cell.setCellStyle(DEFAULT_STYLE);
        });
        var currentColumn = new AtomicInteger(ScheduleProperties.SCHEDULE_TABLE_START.column() + 2);
        schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1))
                .forEach(date -> {
                    currentRow.set(startRow);
                    var lastRowOfAssignments = ScheduleProperties.SCHEDULE_TABLE_START.row() + schedule.getEmployees().size();
                    var countOfDFormula = "COUNTIF(%s:%s, \"D\")"
                            .formatted(
                                    toA1(ScheduleProperties.SCHEDULE_TABLE_START.row(), currentColumn.get()),
                                    toA1(lastRowOfAssignments, currentColumn.get())
                            );
                    var countOfNFormula = "COUNTIF(%s:%s, \"N\")"
                            .formatted(
                                    toA1(ScheduleProperties.SCHEDULE_TABLE_START.row(), currentColumn.get()),
                                    toA1(lastRowOfAssignments, currentColumn.get())
                            );
                    var countOfVFormula = "COUNTBLANK(%s:%s)"
                            .formatted(
                                    toA1(ScheduleProperties.SCHEDULE_TABLE_START.row(), currentColumn.get()),
                                    toA1(lastRowOfAssignments, currentColumn.get())
                            );
                    var totalFormula = "COUNTA(%s:%s)"
                            .formatted(
                                    toA1(ScheduleProperties.SCHEDULE_TABLE_START.row(), currentColumn.get()),
                                    toA1(lastRowOfAssignments, currentColumn.get())
                            );

                    Stream.of(countOfDFormula, countOfNFormula, countOfVFormula, totalFormula)
                          .forEach(formula -> {
                              var cell = sheet.getRow(currentRow.getAndIncrement()).createCell(currentColumn.get());
                              cell.setCellFormula(formula);
                              cell.setCellStyle(DEFAULT_STYLE);
                          });
                    currentColumn.getAndIncrement();
                });
    }

    private void initializeStyles(XSSFWorkbook workbook) {
        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);

        DEFAULT_STYLE = workbook.createCellStyle();
        DEFAULT_STYLE.setAlignment(HorizontalAlignment.CENTER);
        DEFAULT_STYLE.setBorderBottom(BorderStyle.THIN);
        DEFAULT_STYLE.setBorderTop(BorderStyle.THIN);
        DEFAULT_STYLE.setBorderLeft(BorderStyle.THIN);
        DEFAULT_STYLE.setBorderRight(BorderStyle.THIN);

        DEFAULT_SCHEDULE_STYLE = DEFAULT_STYLE.copy();
        DEFAULT_SCHEDULE_STYLE.setFont(boldFont);

        CORRECT_STYLE = DEFAULT_SCHEDULE_STYLE.copy();
        CORRECT_STYLE.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CORRECT_STYLE.setFillForegroundColor(IndexedColors.GREEN.getIndex());

        CORRECT_VACATION_STYLE = DEFAULT_SCHEDULE_STYLE.copy();
        CORRECT_VACATION_STYLE.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CORRECT_VACATION_STYLE.setFillForegroundColor(IndexedColors.BLUE1.getIndex());

        FAILED_STYLE = DEFAULT_SCHEDULE_STYLE.copy();
        FAILED_STYLE.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        FAILED_STYLE.setFillForegroundColor(IndexedColors.RED.getIndex());

        WEEKEND_STYLE = DEFAULT_STYLE.copy();
        WEEKEND_STYLE.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        WEEKEND_STYLE.setFillForegroundColor(IndexedColors.BLUE.getIndex());
    }
}














