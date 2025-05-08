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
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScheduleWriter {

    public static final Map<DayOfWeek, String> WEEK_DAY_TRANSLATIONS = new HashMap<>();
    private final Vocabulary vocabulary;
    private XSSFCellStyle WEEKEND_STYLE;
    private XSSFCellStyle DEFAULT_STYLE;
    private XSSFCellStyle DEFAULT_SCHEDULE_STYLE;
    private XSSFCellStyle NAME_STYLE;
    private XSSFCellStyle CORRECT_STYLE;
    private XSSFCellStyle FAILED_STYLE;
    private XSSFFont BOLD_FONT;

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
        var wb = new XSSFWorkbook();
        BOLD_FONT = this.createBoldFont(wb);
        WEEKEND_STYLE = this.createWeekendStyle(wb);
        DEFAULT_STYLE = this.createDefaultStyle(wb);
        NAME_STYLE = this.createNameStyle(wb);
        CORRECT_STYLE = this.createCorrectStyle(wb);
        FAILED_STYLE = this.createFailedStyle(wb);
        DEFAULT_SCHEDULE_STYLE = this.createDefaultScheduleStyle(wb);
        var newDetailedSheet = wb.createSheet(vocabulary.translate("Schedule"));
        final var startPosition = Coordinates.of(4, 0);
        var lastRow = 0;
        lastRow = this.writeHeader(
                schedule,
                newDetailedSheet,
                startPosition
        );
        var startPositionForAssignments = Coordinates.of(lastRow + 1, startPosition.column);
        lastRow = this.writeDetailedSchedule(
                schedule,
                newDetailedSheet,
                startPositionForAssignments
        );
        this.writeFooter(
                schedule,
                newDetailedSheet,
                Coordinates.of(lastRow + 1, startPosition.column),
                startPositionForAssignments.row
        );
        newDetailedSheet.createFreezePane(startPosition.column + 1, startPosition.row + 1);
        var lastCol = newDetailedSheet.getRow(startPosition.row).getLastCellNum();
        for (int col = startPosition.column; col < lastCol + 1; col++) {
            newDetailedSheet.autoSizeColumn(col);
        }
        try (var outStream = new FileOutputStream(output)) {
            wb.write(outStream);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer writeHeader(EmployeeSchedule schedule, XSSFSheet sheet, Coordinates start) {
        var currentColumn = new AtomicInteger(start.column);
        var headerRow = sheet.createRow(start.row);
        var nameCell = headerRow.createCell(currentColumn.getAndIncrement());
        nameCell.setCellValue(vocabulary.translate("Name"));
        nameCell.setCellStyle(DEFAULT_STYLE);
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
        return start.row + 1;
    }

    private Integer writeDetailedSchedule(EmployeeSchedule schedule, XSSFSheet sheet, Coordinates start) {
        var currentColumn = new AtomicInteger(start.column);
        var currentRow = new AtomicInteger(start.row);
        schedule.getEmployees().forEach(employee -> {
            var employeeRow = sheet.createRow(currentRow.getAndIncrement());
            currentColumn.set(start.column);
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
                                .orElseGet(() -> {
                                    //TODO ask jana
//                                    if (availabilities.size() == 2) return "RD";
                                    return "";
                                });
                        var cell = employeeRow.createCell(currentColumn.getAndIncrement());
                        cell.setCellValue(assignmentSymbol);
                        if (availabilities.isEmpty()) {
                            cell.setCellStyle(DEFAULT_SCHEDULE_STYLE);
                        } else if (availabilities.size() == 2) {
                            if (assignment.isEmpty()) {
                                cell.setCellStyle(CORRECT_STYLE);
                            } else {
                                cell.setCellStyle(FAILED_STYLE);
                            }
                        } else if (availabilities.size() == 1) {
                            var availability = availabilities.get(0);
                            if (this.isAssignmentCorrect(assignment.map(ShiftAssignment::getShiftType).orElse(null), availability)) {
                                cell.setCellStyle(CORRECT_STYLE);
                            } else {
                                cell.setCellStyle(FAILED_STYLE);
                            }
                        }
                    });

            var formulaRow = employeeRow.getRowNum() + 1;
            var formulaFirstColumn = start.column + 2;
            var formulaLastColumn = currentColumn.get();
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
            var totalFormula = "COUNTA(%s:%s)".formatted(
                    toA1(formulaRow, formulaFirstColumn),
                    toA1(formulaRow, formulaLastColumn)
            );

            Stream.of(
                    countDFormula,
                    countNFormula,
                    countVFormula,
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

    private String toA1(final int row, final int column) {
        Assert.isTrue(row > 0, "Row must be more than 0");
        Assert.isTrue(column > 0, "Column must be more than 0");
        StringBuilder colRef = new StringBuilder();
        int col = column;
        while (col > 0) {
            col--; // Excel columns are 1-indexed, but 'A' starts at 0
            colRef.insert(0, (char) ( 'A' + ( col % 26 ) ));
            col /= 26;
        }
        return colRef.toString() + row;
    }

    private Integer writeFooter(EmployeeSchedule schedule, XSSFSheet sheet, Coordinates start, Integer firstRowOfAssignments) {
        var currentRow = new AtomicInteger(start.row);
        Stream.of("Count of D", "Count of N", "Count of V", "Total").forEach(content -> {
            var cell = sheet.createRow(currentRow.getAndIncrement()).createCell(start.column);
            cell.setCellValue(vocabulary.translate(content));
            cell.setCellStyle(DEFAULT_STYLE);
        });
        var currentColumn = new AtomicInteger(start.column + 1);
        schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1))
                .forEach(date -> {
                    currentRow.set(start.row);
                    var lastRowOfAssignments = firstRowOfAssignments + schedule.getEmployees().size();
                    var countOfDFormula = "COUNTIF(%s:%s, \"D\")"
                            .formatted(
                                    toA1(firstRowOfAssignments + 1, currentColumn.get() + 1),
                                    toA1(lastRowOfAssignments, currentColumn.get() + 1)
                            );
                    var countOfNFormula = "COUNTIF(%s:%s, \"N\")"
                            .formatted(
                                    toA1(firstRowOfAssignments + 1, currentColumn.get() + 1),
                                    toA1(lastRowOfAssignments, currentColumn.get() + 1)
                            );
                    var countOfVFormula = "COUNTBLANK(%s:%s)"
                            .formatted(
                                    toA1(firstRowOfAssignments + 1, currentColumn.get() + 1),
                                    toA1(lastRowOfAssignments, currentColumn.get() + 1)
                            );
                    var totalFormula = "COUNTA(%s:%s)"
                            .formatted(
                                    toA1(firstRowOfAssignments + 1, currentColumn.get() + 1),
                                    toA1(lastRowOfAssignments, currentColumn.get() + 1)
                            );

                    Stream.of(countOfDFormula, countOfNFormula, countOfVFormula, totalFormula)
                          .forEach(formula -> {
                              var cell = sheet.getRow(currentRow.getAndIncrement()).createCell(currentColumn.get());
                              cell.setCellFormula(formula);
                              cell.setCellStyle(DEFAULT_STYLE);
                          });
                    currentColumn.getAndIncrement();
                });
        return start.row + 3;
    }


    private XSSFCellStyle createDefaultStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private XSSFCellStyle createDefaultScheduleStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFont(BOLD_FONT);
        return style;
    }

    private XSSFCellStyle createNameStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFont(BOLD_FONT);
        return style;
    }

    private XSSFCellStyle createCorrectStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFont(BOLD_FONT);
        return style;
    }

    private XSSFCellStyle createFailedStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFont(BOLD_FONT);
        return style;
    }

    private XSSFCellStyle createWeekendStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private XSSFFont createBoldFont(XSSFWorkbook workbook) {
        var font = workbook.createFont();
        font.setBold(true);
        return font;
    }

    private record Coordinates(Integer row, Integer column) {
        public static Coordinates of(Integer row, Integer column) {
            return new Coordinates(row, column);
        }
    }

}














