package com.cocroachden.scheduler.solver.utils;

import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;
import com.cocroachden.scheduler.domain.AvailabilityId;
import com.cocroachden.scheduler.domain.EmployeeId;
import com.cocroachden.scheduler.domain.ShiftAssignmentId;
import com.cocroachden.scheduler.solver.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ScheduleParser implements SolutionFileIO<EmployeeSchedule> {

    public static final String FILE_EXTENSION = "xlsx";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy");
    public static final Map<DayOfWeek, String> WEEK_DAY_TRANSLATIONS = new HashMap<>();

    static {
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.MONDAY, "Po");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.TUESDAY, "Ut");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.WEDNESDAY, "St");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.THURSDAY, "Ct");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.FRIDAY, "Pa");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.SATURDAY, "So");
        WEEK_DAY_TRANSLATIONS.put(DayOfWeek.SUNDAY, "Ne");
    }

    @Override
    public String getInputFileExtension() {
        return FILE_EXTENSION;
    }

    @Override
    public String getOutputFileExtension() {
        return FILE_EXTENSION;
    }

    @SneakyThrows
    public EmployeeSchedule read(File inputFile) {
        try (
                FileInputStream fis = new FileInputStream(inputFile);
                var workbook = new XSSFWorkbook(fis)) {
            var schedule = new EmployeeSchedule();
            var scheduleSheet = workbook.getSheet("Rozvrh");
            var employeeSheet = workbook.getSheet("Zamestnanci");
            var settingsSheet = workbook.getSheet("Nastaveni");
            if (scheduleSheet == null) {
                throw ScheduleParserException.becauseCouldNotFindSheet("Rozvrh");
            }
            if (settingsSheet == null) {
                throw ScheduleParserException.becauseCouldNotFindSheet("Nastaveni");
            }
            this.readSchedule(scheduleSheet, schedule);
            this.readSettings(settingsSheet, schedule);
            return schedule;
        }
    }

    @SneakyThrows
    public void write(EmployeeSchedule schedule, File output) {
        var wb = new XSSFWorkbook();
        this.createScheduleSheet(schedule, wb);
        this.createDetailedScheduleSheet(schedule, wb);
        try (var outStream = new FileOutputStream(output)) {
            wb.write(outStream);
            wb.close();
        }
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
            employeeRow.createCell(0).setCellValue(employee.getEmployeeId().id());
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
            employeeRow.createCell(0).setCellValue(employee.getEmployeeId().id());
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

    private void readSettings(final XSSFSheet settingsSheet, final EmployeeSchedule schedule) {
        var employeesPerDayShift = settingsSheet.getRow(0).getCell(1).getNumericCellValue();
        var employeesPerNightShift = settingsSheet.getRow(1).getCell(1).getNumericCellValue();
        var assignments = new LinkedHashSet<ShiftAssignment>();
        schedule.getStartDate().datesUntil(schedule.getEndDate().plusDays(1)).forEach(date -> {
            for (int i = 0; i < employeesPerDayShift; i++) {
                assignments.add(
                        new ShiftAssignment()
                                .setDate(date)
                                .setShiftType(ShiftType.DAY)
                                .setId(new ShiftAssignmentId(date.toString() + ShiftType.DAY.name().charAt(0) + i))
                );
            }
            for (int i = 0; i < employeesPerNightShift; i++) {
                assignments.add(
                        new ShiftAssignment()
                                .setDate(date)
                                .setShiftType(ShiftType.NIGHT)
                                .setId(new ShiftAssignmentId(date.toString() + ShiftType.NIGHT.name().charAt(0) + i))
                );
            }
        });
        schedule.setShiftAssignments(assignments);
    }

    private void readSchedule(XSSFSheet sheet, EmployeeSchedule schedule) {
        LocalDate startDate = LocalDate.parse(
                sheet.getRow(0).getCell(1).getStringCellValue(),
                DATE_TIME_FORMATTER
        );
        LocalDate endDate = LocalDate.parse(
                sheet.getRow(1).getCell(1).getStringCellValue(),
                DATE_TIME_FORMATTER
        );
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        var employees = new ArrayList<Employee>();
        var availabilities = new ArrayList<Availability>();
        for (int i = 4; i <= sheet.getLastRowNum(); i++) {
            var employeeRow = sheet.getRow(i);
            if (employeeRow == null) break;
            var nameCell = employeeRow.getCell(0);
            if (nameCell == null) break;
            var name = nameCell.getStringCellValue();
            var idealShiftCount = sheet.getRow(i).getCell(1).getNumericCellValue();
            if (name.isBlank()) break;
            Employee employee = new Employee(new EmployeeId(name), (int) Math.round(idealShiftCount));
            var lastCell = sheet.getRow(i).getLastCellNum();
            for (int j = 2; j < lastCell + 1; j++) {
                var cell = sheet.getRow(i).getCell(j);
                if (cell == null) continue;
                var symbol = cell.getStringCellValue();
                if (symbol.isBlank()) continue;
                var date = startDate.plusDays(j - 2);
                if (symbol.equalsIgnoreCase("V")) {
                    var day = Availability.builder()
                                          .id(new AvailabilityId(date.toString() + ShiftType.DAY.name().charAt(0)))
                                          .shiftType(ShiftType.DAY)
                                          .type(AvailabilityType.UNAVAILABLE)
                                          .date(date)
                                          .employee(employee)
                                          .build();
                    var night = Availability.builder()
                                            .id(new AvailabilityId(date.toString() + ShiftType.NIGHT.name().charAt(0)))
                                            .shiftType(ShiftType.NIGHT)
                                            .type(AvailabilityType.UNAVAILABLE)
                                            .date(date)
                                            .employee(employee)
                                            .build();
                    availabilities.add(day);
                    availabilities.add(night);
                    continue;
                }
                var availability = Availability.builder();
                if (symbol.contains("!")) {
                    availability.type(AvailabilityType.UNAVAILABLE);
                } else if (symbol.contains("-")) {
                    availability.type(AvailabilityType.UNDESIRED);
                } else if (symbol.contains("+")) {
                    availability.type(AvailabilityType.DESIRED);
                } else {
                    availability.type(AvailabilityType.REQUIRED);
                }
                if (symbol.contains("D")) {
                    availability.shiftType(ShiftType.DAY);
                    availability.id(new AvailabilityId(date.toString() + ShiftType.DAY.name().charAt(0)));
                } else if (symbol.contains("N")) {
                    availability.shiftType(ShiftType.NIGHT);
                    availability.id(new AvailabilityId(date.toString() + ShiftType.NIGHT.name().charAt(0)));
                }
                availability.date(date);
                availability.employee(employee);
                availabilities.add(availability.build());
            }
            employees.add(employee);
        }
        schedule.setEmployees(employees);
        schedule.setAvailabilities(availabilities);
    }
}
