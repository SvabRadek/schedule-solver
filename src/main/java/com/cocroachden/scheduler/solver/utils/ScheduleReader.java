package com.cocroachden.scheduler.solver.utils;

import com.cocroachden.scheduler.domain.EmployeeId;
import com.cocroachden.scheduler.domain.ShiftAssignmentId;
import com.cocroachden.scheduler.domain.Vocabulary;
import com.cocroachden.scheduler.solver.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;

@Service
public class ScheduleReader {

    private final Vocabulary vocabulary;

    public ScheduleReader(final Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    public EmployeeSchedule read(File inputFile) {
        try (
                FileInputStream fis = new FileInputStream(inputFile);
                var workbook = new XSSFWorkbook(fis)
        ) {
            var schedule = new EmployeeSchedule();
            var scheduleSheet = workbook.getSheet(vocabulary.translateFromEn(ScheduleProperties.ASSIGNMENT_SHEET_NAME));
            if (scheduleSheet == null) {
                throw ScheduleReaderException.becauseCouldNotFindSheet(vocabulary.translateFromEn(ScheduleProperties.ASSIGNMENT_SHEET_NAME));
            }
            this.readSchedule(scheduleSheet, schedule);
            this.createShiftAssignments(scheduleSheet, schedule);
            return schedule;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readSchedule(XSSFSheet sheet, EmployeeSchedule schedule) {
        LocalDate startDate = LocalDate.parse(
                sheet.getRow(ScheduleProperties.START_DATE_VALUE_CELL.row()).getCell(ScheduleProperties.START_DATE_VALUE_CELL.column()).getStringCellValue(),
                ScheduleProperties.SCHEDULE_DATE_FORMAT
        );
        LocalDate endDate = LocalDate.parse(
                sheet.getRow(ScheduleProperties.END_DATE_VALUE_CELL.row()).getCell(ScheduleProperties.END_DATE_VALUE_CELL.column()).getStringCellValue(),
                ScheduleProperties.SCHEDULE_DATE_FORMAT
        );
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        var employees = new ArrayList<Employee>();
        var availabilities = new ArrayList<Availability>();
        //TODO this will also pick up footer, which it shouldn't
        for (int i = ScheduleProperties.SCHEDULE_TABLE_START.row(); i <= sheet.getLastRowNum(); i++) {
            var employeeRow = sheet.getRow(i);
            if (employeeRow == null) break;
            var nameCell = employeeRow.getCell(ScheduleProperties.SCHEDULE_TABLE_START.column());
            if (nameCell == null) break;
            var name = nameCell.getStringCellValue();
            if (name.isBlank()) break;
            var idealShiftCount = sheet.getRow(i).getCell(ScheduleProperties.SCHEDULE_TABLE_START.column() + 1).getNumericCellValue();
            Employee employee = new Employee(new EmployeeId(name), (int) Math.round(idealShiftCount));
            var lastCell = sheet.getRow(i).getLastCellNum();
            for (int j = ScheduleProperties.SCHEDULE_TABLE_START.column() + 2; j < lastCell + 1; j++) {
                var cell = sheet.getRow(i).getCell(j);
                if (cell == null) continue;
                cell.setCellType(CellType.STRING);
                var symbol = cell.getStringCellValue();
                if (symbol.isBlank()) continue;
                var date = startDate.plusDays(j - 2);
                if (symbol.contains("V")) {
                    var day = Availability.of(
                            employee,
                            date,
                            ShiftType.DAY,
                            AvailabilityType.UNAVAILABLE
                    );
                    var night = Availability.of(
                            employee,
                            date,
                            ShiftType.NIGHT,
                            AvailabilityType.UNAVAILABLE
                    );
                    availabilities.add(day);
                    availabilities.add(night);
                } else if (symbol.contains("D")) {
                    availabilities.add(
                            Availability.of(
                                    employee,
                                    date,
                                    ShiftType.DAY,
                                    this.parseAvailabilityFromSymbol(symbol)
                            )
                    );
                } else if (symbol.contains("N")) {
                    availabilities.add(
                            Availability.of(
                                    employee,
                                    date,
                                    ShiftType.NIGHT,
                                    this.parseAvailabilityFromSymbol(symbol)
                            )
                    );
                }
            }
            employees.add(employee);
        }
        schedule.setEmployees(employees);
        schedule.setAvailabilities(availabilities);
    }

    private void createShiftAssignments(final XSSFSheet sheet, final EmployeeSchedule schedule) {
        var employeesPerDayShift = sheet
                .getRow(ScheduleProperties.PEOPLE_ON_DAY_SHIFT.row())
                .getCell(ScheduleProperties.PEOPLE_ON_DAY_SHIFT.column())
                .getNumericCellValue();
        var employeesPerNightShift = sheet
                .getRow(ScheduleProperties.PEOPLE_ON_NIGHT_SHIFT.row())
                .getCell(ScheduleProperties.PEOPLE_ON_NIGHT_SHIFT.column())
                .getNumericCellValue();
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

    private AvailabilityType parseAvailabilityFromSymbol(String symbol) {
        if (symbol.contains("!")) {
            return AvailabilityType.UNAVAILABLE;
        } else if (symbol.contains("-")) {
            return AvailabilityType.UNDESIRED;
        } else if (symbol.contains("+")) {
            return AvailabilityType.DESIRED;
        } else {
            return AvailabilityType.REQUIRED;
        }
    }
}
