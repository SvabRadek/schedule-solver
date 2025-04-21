package com.cocroachden.scheduler.solver.utils;

import com.cocroachden.scheduler.domain.AvailabilityId;
import com.cocroachden.scheduler.domain.EmployeeId;
import com.cocroachden.scheduler.domain.ShiftAssignmentId;
import com.cocroachden.scheduler.solver.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Service
public class ScheduleParser {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy");

    public EmployeeSchedule convert(Path path) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(path.toFile());
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

    private void readSettings(final XSSFSheet settingsSheet, final EmployeeSchedule schedule) {
        var employeesPerDayShift = settingsSheet.getRow(0).getCell(1).getNumericCellValue();
        var employeesPerNightShift = settingsSheet.getRow(1).getCell(1).getNumericCellValue();
        var assignments = new ArrayList<ShiftAssignment>();
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
            var name = sheet.getRow(i).getCell(0).getStringCellValue();
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
