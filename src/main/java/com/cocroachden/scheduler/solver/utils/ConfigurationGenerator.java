package com.cocroachden.scheduler.solver.utils;

import com.cocroachden.scheduler.domain.Vocabulary;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConfigurationGenerator {

    public static final DateTimeFormatter WB_DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy");

    private final Vocabulary vocabulary;

    public void generate(LocalDate startDate, LocalDate endDate) {
        var currentDir = System.getProperty("user.dir");
        try (var wb = new XSSFWorkbook()) {
            var scheduleSheet = wb.createSheet(vocabulary.translate("Schedule"));
            var configurationSheet = wb.createSheet(vocabulary.translate("Configuration"));
            this.createScheduleSheet(scheduleSheet, startDate, endDate);
            var file = new File(currentDir + "/" + vocabulary.translate("Schedule") + ".xlsx");
            var outputStream = new FileOutputStream(file);
            wb.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createScheduleSheet(XSSFSheet sheet, LocalDate startDate, LocalDate endDate) {
        var firstRow = sheet.createRow(0);
        var secondRow = sheet.createRow(1);
        var headerRow = sheet.createRow(3);

        firstRow.createCell(0).setCellValue(vocabulary.translate("Start"));
        firstRow.createCell(1).setCellValue(startDate.format(WB_DATE_FORMATTER));
        firstRow.createCell(5).setCellValue(vocabulary.translate("Schedule Instructions"));
        secondRow.createCell(0).setCellValue(vocabulary.translate("End"));
        secondRow.createCell(1).setCellValue(endDate.format(WB_DATE_FORMATTER));

        headerRow.createCell(0).setCellValue(vocabulary.translate("Name"));
        headerRow.createCell(1).setCellValue(vocabulary.translate("Ideal shift count"));
        startDate.datesUntil(endDate.plusDays(1)).forEach(date -> {
            var index = (int) ChronoUnit.DAYS.between(startDate, date);
            var headerDayCell = headerRow.createCell(index + 2);
            headerDayCell.setCellValue(index);
            if (date.getDayOfWeek().getValue() > 5) {
                headerDayCell.getCellStyle().setFillBackgroundColor(IndexedColors.BRIGHT_GREEN.index);
            }
        });
        var currentRow = new AtomicInteger(4);
        Stream.of(vocabulary.translate("Employee1"), vocabulary.translate("Employee2"))
              .forEach(employee -> {
                  var row = sheet.createRow(currentRow.getAndIncrement());
                  row.createCell(0).setCellValue(employee);
              });
    }

}
