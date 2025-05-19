package com.cocroachden.scheduler.solver.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class ExcelUtils {

    public static String toA1(Coordinates coordinates) {
        return ExcelUtils.toA1(coordinates.row(), coordinates.column());
    }

    public static String toA1(final int row, final int column) {
        StringBuilder colRef = new StringBuilder();
        int col = column;
        do {
            colRef.insert(0, (char) ( 'A' + ( col % 26 ) ));
            col /= 26;
            col--;
        } while (col >= 0);
        return colRef.toString() + ( row + 1 );
    }

    public static String toRange(Coordinates start, Coordinates end) {
        return toA1(start) + ":" + toA1(end);
    }

    public static void addComment(Coordinates coordinate, String commentText, XSSFSheet sheet) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor();

        anchor.setCol1(coordinate.column());
        anchor.setRow1(coordinate.row());
        anchor.setCol2(coordinate.column() + 3);
        anchor.setRow2(coordinate.row() + 3);

        var comment = drawing.createCellComment(anchor);
        comment.setAuthor("");
        comment.setString(commentText);

        // Attach the comment to the cell
        Row row = sheet.getRow(coordinate.row());
        if (row == null) {
            row = sheet.createRow(coordinate.row());
        }

        Cell cell = row.getCell(coordinate.column());
        if (cell == null) {
            cell = row.createCell(coordinate.column());
        }

        cell.setCellComment(comment);
    }

}
