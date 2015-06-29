package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class RecordAppenderExcelSource extends ExcelSource {

    @Getter
    Map<String, List<Map<String, String>>> appendRecordSources = new LinkedHashMap<>();

    public RecordAppenderExcelSource() throws IOException, Exception {
        setSourceKind("excel.recordAppender");
        setSourceName("レコード追加リストのファイル名");
        setSourceNamePattern(Settings.get(getSourceName()));
        buildEndpoint();
    }

    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        appendRecordSources.clear();
        Utility utility = new Utility();
        Stream.of("納期案内", "媒体一覧", "カタログ（最新）", "カタログ（旧）")
                .forEach((sheetName) -> {
                    Sheet sheet = workbook.getSheet(sheetName);
                    List<String[]> sal = utility.sheetToStringArrayList(sheet);
                    List<Map<String, String>> appendRecordSource = new ArrayList<>();
                    String[] headerRow = sal.get(1);
                    sal.stream().skip(2).forEach((values) -> {
                        Map<String, String> map = new LinkedHashMap<>();
                        int valueMaxLength = IntStream.range(0, Math.min(headerRow.length, values.length))
                        .map((i) -> {
                            map.put(headerRow[i], values[i]);
                            return values[i].length();
                        }).max().orElse(0);
                        if (valueMaxLength > 0) {
                            appendRecordSource.add(map);
                        }
                    });
                    appendRecordSources.put(sheetName, appendRecordSource);
                });
        updateHash(header, appendRecordSources.hashCode());
    }

    public void appendAll(String sheetName, List<Map<String, String>> mapList) {
        mapList.addAll(this.getAppendRecordSources().get(sheetName));
    }
}
