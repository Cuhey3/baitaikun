package com.mycode.baitaikun.sources.excel;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.Source;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public abstract class ExcelSource extends Source {

    @Setter
    String sourceNamePattern;
    @Setter
    @Getter
    String sourceName;
    @Getter
    public final List<String[]> stringArrayList = new ArrayList<>();
    public String startEndpoint;
    public int oldHash = -1;

    @Override
    public void buildEndpoint() throws Exception {
        startEndpoint = "direct:" + sourceKind;
    }

    @Override
    public void configure() {
        onException(java.io.FileNotFoundException.class).handled(true);
        from(startEndpoint)
                .bean(this, "openWorkbook")
                .filter().simple("${body} is 'org.apache.poi.ss.usermodel.Workbook'")
                .bean(this, "loadSheet")
                .choice().when().simple("${header.change}")
                .bean(this, "updated");
    }

    public Workbook openWorkbook(@Body InputStream inputStream, @Header("CamelFileName") String fileName) throws IOException {
        try {
            if (fileName.endsWith(".xlsx")) {
                return new XSSFWorkbook(inputStream);
            } else if (fileName.endsWith(".xls")) {
                return new HSSFWorkbook(inputStream);
            } else {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        Iterator<Row> iterator = workbook.getSheetAt(0).rowIterator();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();
        stringArrayList.clear();
        while (iterator.hasNext()) {
            stringArrayList.add(rowToStringArray(iterator.next(), formatter, evaluator));
        }
        int hashCode = stringArrayList.hashCode();
        if (oldHash != hashCode) {
            header.put("change", true);
            oldHash = hashCode;
        }
    }

    public String[] rowToStringArray(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        short size = row.getLastCellNum();
        try {
            String[] array = new String[size];
            for (int i = 0; i < size; i++) {
                Cell next = row.getCell(i);
                String formatCellValue = formatter.formatCellValue(next, evaluator).replaceAll("\r\n|\n|\r", " ");
                array[i] = formatCellValue;
            }
            return array;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean isUpToDate() {
        return isReady();
    }
}
