package com.cocroachden.scheduler.solver.utils;

import com.cocroachden.scheduler.solver.EmployeeSchedule;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ScheduleWriter {


    public void write(EmployeeSchedule schedule, Path path) throws IOException {
        var wb = new XSSFWorkbook();
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
                    cell.setCellValue(localDate.getDayOfMonth());
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
        try (var outStream = new FileOutputStream(path.toString())) {
            wb.write(outStream);
            wb.close();
        }
    }

}
