package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class RecordAppenderExcelSource extends ExcelSource {

    @Getter
    Map<String, List<Map<String, String>>> appendRecordSources = new LinkedHashMap<>();

    public RecordAppenderExcelSource() throws IOException, Exception {
        setSourceKind("excel.recordAppender");
        setSourceNamePattern(Settings.get("レコード追加リストのファイル名"));
        buildEndpoint();
    }

    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        appendRecordSources.clear();
        String[] sheetNames = new String[]{"納期案内", "媒体一覧", "カタログ（最新）", "カタログ（旧）"};
        for (String sheetName : sheetNames) {
            Sheet sheet = workbook.getSheet(sheetName);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rowIterator = sheet.rowIterator();
            rowIterator.next();
            Row row = rowIterator.next();
            List<Map<String, String>> appendRecordSource = new ArrayList<>();
            String[] headerRow = rowToStringArray(row, formatter, evaluator);
            while (rowIterator.hasNext()) {
                String[] values = rowToStringArray(rowIterator.next(), formatter, evaluator);
                Map<String, String> map = new LinkedHashMap<>();
                int valueLengthMax = 0;
                for (int i = 0; i < headerRow.length && i < values.length; i++) {
                    map.put(headerRow[i], values[i]);
                    valueLengthMax = Math.max(values[i].length(), valueLengthMax);
                }
                if (valueLengthMax != 0) {
                    appendRecordSource.add(map);
                }
            }
            appendRecordSources.put(sheetName, appendRecordSource);
        }
        int hashCode = appendRecordSources.hashCode();
        if (oldHash != hashCode) {
            header.put("change", true);
            oldHash = hashCode;
        }
    }

    public void appendAll(String sheetName, List<Map<String, String>> mapList) {
        mapList.addAll(this.getAppendRecordSources().get(sheetName));
    }
}
