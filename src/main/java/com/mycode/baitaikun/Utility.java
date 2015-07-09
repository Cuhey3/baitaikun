package com.mycode.baitaikun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

@Component
public class Utility {

    public List<Map<String, String>> createMapList(List<String[]> stringArrayList, int skip) {
        List<Map<String, String>> mapList = new ArrayList<>();
        String[] header = stringArrayList.get(skip);
        stringArrayList.stream()
                .skip(skip + 1)
                .forEach((values) -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    int valueMaxLength = IntStream.range(0, Math.min(header.length, values.length))
                    .map((i) -> {
                        map.put(header[i], values[i]);
                        return values[i].length();
                    }).max().orElse(0);
                    if (valueMaxLength > 0) {
                        mapList.add(map);
                    }
                });
        return mapList;
    }

    public List<String[]> sheetToStringArrayList(Sheet sheet) {
        FormulaEvaluator evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();

        Iterator<Row> rowIterator = sheet.rowIterator();
        List<String[]> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        int i = 0;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String[] rowToStringArray = rowToStringArray(row, formatter, evaluator);
            if (rowToStringArray != null) {
                list.add(rowToStringArray);
                i++;
                if (i % 1000 == 0) {
                    long time = System.currentTimeMillis() - start;
                    if (i * 0.5 < time) {
                        System.out.println("[MESSAGE] 読み取り処理に時間がかかっています。" + i + "行読み取りました。");
                    }
                }
            }
        }
        return list;
    }

    public String[] rowToStringArray(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        try {
            return IntStream.range(0, row.getLastCellNum())
                    .mapToObj((i)
                            -> formatter.formatCellValue(row.getCell(i), evaluator).replaceAll("\r\n|\n|\r", " "))
                    .toArray((s)
                            -> new String[s]);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public boolean isAnyNotEmpty(String[] array) {
        return Stream.of(array)
                .anyMatch((s)
                        -> s != null && !s.isEmpty());
    }

    public boolean isAnyNotEmpty(String[] array, int size) {
        return Stream.of(array)
                .limit(size)
                .anyMatch((s)
                        -> s != null && !s.isEmpty());
    }

    public boolean isFilled(String[] array, int checkSize) {
        if (array.length < checkSize) {
            return false;
        } else {
            return Stream.of(array)
                    .limit(checkSize)
                    .allMatch((s)
                            -> s != null && !s.isEmpty());
        }
    }

    public int getHashCodeWhenListContainsArray(List<String[]> list) {
        return Arrays.hashCode(list.stream()
                .map((array) -> Arrays.hashCode(array))
                .toArray(size -> new Integer[size]));
    }
}
